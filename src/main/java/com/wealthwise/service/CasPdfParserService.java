package com.wealthwise.service;

import com.wealthwise.entity.*;
import com.wealthwise.entity.UserTransaction.TransactionType;
import com.wealthwise.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.*;

/**
 * M04 — CAS PDF Parser Service
 *
 * Parses an AMFI Consolidated Account Statement (CAS) PDF.
 * Extracts folio blocks, transactions, and saves them to the database,
 * rebuilding the user_portfolio and tax_lots tables.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CasPdfParserService {

    private final SchemeMasterRepository   schemeRepo;
    private final UserTransactionRepository txRepo;
    private final UserPortfolioRepository  portfolioRepo;
    private final CasUploadLogRepository   logRepo;
    private final TaxLotRepository         taxLotRepo;

    // ── Regex Patterns ──────────────────────────────────────────────────────

    private static final Pattern FOLIO_PATTERN = Pattern.compile(
            "Folio\\s*(?:No|Number)[:.\\s]+([\\w/\\-\\s]+?)(?:\\s+PAN|\\s+KYC|\\n|$)",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern ISIN_PATTERN = Pattern.compile(
            "ISIN\\s*[:\\-]\\s*([A-Z]{2}[A-Z0-9]{10})",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern CLOSING_UNITS_PATTERN = Pattern.compile(
            "(?:Closing|Available)\\s+(?:Unit\\s+)?Balance\\s*[:\\-]?\\s*([\\d,]+\\.\\d+)",
            Pattern.CASE_INSENSITIVE);

    // date  description                        amount      units       nav       balance
    private static final Pattern TX_PATTERN = Pattern.compile(
            "(\\d{2}-[A-Za-z]{3}-\\d{4})\\s+(.{4,60}?)\\s+([\\d,]+\\.\\d{2})\\s+([\\d.]+)\\s+([\\d.]+)\\s+([\\d.]+)",
            Pattern.MULTILINE);

    private static final Pattern SCHEME_NAME_PATTERN = Pattern.compile(
            "^(.+?)(?:\\s*-\\s*(?:Direct|Regular))?(?:\\s*-\\s*(?:Growth|IDCW))?\\s*$",
            Pattern.CASE_INSENSITIVE);

    // FIXED: Locale.ENGLISH prevents crashes on non-English OS/servers
    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd-MMM-yyyy", Locale.ENGLISH);

    // ── Folio Block ─────────────────────────────────────────────────────────

    /**
     * Holds all data extracted for one folio (= one fund account).
     */
    static class FolioBlock {
        String folioNumber;
        String isin;
        String schemeName;
        String amcName;
        BigDecimal closingUnits = BigDecimal.ZERO;
        List<TxRow> transactions = new ArrayList<>();
    }

    /**
     * One parsed transaction row.
     */
    static class TxRow {
        LocalDate date;
        String    description;
        BigDecimal amount       = BigDecimal.ZERO;
        BigDecimal units        = BigDecimal.ZERO;
        BigDecimal nav          = BigDecimal.ZERO;
        BigDecimal balanceUnits = BigDecimal.ZERO;
    }

    // ── Public API ──────────────────────────────────────────────────────────

    /**
     * Entry point — not @Transactional so that the FAILED log entry is not
     * rolled back on exception.
     */
    public Map<String, Object> parseCas(MultipartFile file, Long userId) throws IOException {
        CasUploadLog uploadLog = new CasUploadLog();
        uploadLog.setUserId(userId);
        uploadLog.setFileName(file.getOriginalFilename());
        uploadLog.setStatus(CasUploadLog.Status.PROCESSING);
        uploadLog = logRepo.save(uploadLog);

        try {
            return processCasTransactionally(file, userId, uploadLog);
        } catch (Exception e) {
            uploadLog.setStatus(CasUploadLog.Status.FAILED);
            uploadLog.setErrorMessage(e.getMessage());
            logRepo.save(uploadLog); // survives because we are outside the rolled-back tx
            log.error("CAS parse failed for user {}: {}", userId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Runs the full parse + DB write inside a fresh transaction.
     * If anything fails, the entire batch is rolled back cleanly.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Map<String, Object> processCasTransactionally(
            MultipartFile file, Long userId, CasUploadLog uploadLog) throws IOException {

        byte[] pdfBytes = file.getBytes();
        try (PDDocument doc = PDDocument.load(pdfBytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String fullText = stripper.getText(doc);

            List<FolioBlock> folios = splitIntoFolios(fullText);
            int totalTx = 0;

            for (FolioBlock folio : folios) {
                List<UserTransaction> txList = saveTransactions(folio, userId);
                savePortfolio(folio, userId, txList);
                saveTaxLots(folio, userId, txList);
                totalTx += txList.size();
            }

            uploadLog.setStatus(CasUploadLog.Status.SUCCESS);
            uploadLog.setTotalFolios(folios.size());
            uploadLog.setTotalTransactions(totalTx);
            logRepo.save(uploadLog);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("status",            "SUCCESS");
            result.put("totalFolios",       folios.size());
            result.put("totalTransactions", totalTx);
            result.put("uploadId",          uploadLog.getId());
            return result;
        }
    }

    // ── Private Helpers ─────────────────────────────────────────────────────

    /**
     * Splits the full CAS text into per-folio blocks.
     * A new block starts whenever "Folio No" / "Folio Number" appears.
     */
    private List<FolioBlock> splitIntoFolios(String fullText) {
        List<FolioBlock> result = new ArrayList<>();
        String[] lines = fullText.split("\\r?\\n");

        FolioBlock current = null;
        StringBuilder blockBuffer = null;
        String currentAmc = "";

        for (String line : lines) {
            String trimmed = line.trim();

            // Detect AMC name (lines that look like headers: all-caps or known patterns)
            if (trimmed.matches("(?i).*Mutual Fund.*") && !trimmed.contains(":")) {
                currentAmc = trimmed;
            }

            // New folio starts
            Matcher folioMatcher = FOLIO_PATTERN.matcher(trimmed);
            if (folioMatcher.find()) {
                if (current != null && blockBuffer != null) {
                    finishFolioBlock(current, blockBuffer.toString());
                    result.add(current);
                }
                current = new FolioBlock();
                current.folioNumber = folioMatcher.group(1).trim();
                current.amcName = currentAmc;
                blockBuffer = new StringBuilder();
            }

            if (blockBuffer != null) {
                blockBuffer.append(line).append("\n");
            }
        }

        // Add last block
        if (current != null && blockBuffer != null) {
            finishFolioBlock(current, blockBuffer.toString());
            result.add(current);
        }

        log.info("splitIntoFolios: found {} folio blocks", result.size());
        return result;
    }

    /**
     * Extracts ISIN, schemeName, closingUnits, and transactions from a folio's text.
     */
    private void finishFolioBlock(FolioBlock folio, String blockText) {
        // ISIN
        Matcher isinMatcher = ISIN_PATTERN.matcher(blockText);
        if (isinMatcher.find()) {
            folio.isin = isinMatcher.group(1).trim();
            // Resolve AMFI code from ISIN
            schemeRepo.findByIsinGrowth(folio.isin)
                .or(() -> schemeRepo.findByIsinIdcw(folio.isin))
                .ifPresent(s -> folio.schemeName = s.getSchemeName());
        }

        // Closing units
        Matcher closingMatcher = CLOSING_UNITS_PATTERN.matcher(blockText);
        if (closingMatcher.find()) {
            folio.closingUnits = parseBigDecimal(closingMatcher.group(1));
        }

        // Scheme name fallback if not resolved from ISIN
        if (folio.schemeName == null || folio.schemeName.isBlank()) {
            // Try to pick first non-folio, non-ISIN content line as scheme name
            for (String line : blockText.split("\\r?\\n")) {
                String t = line.trim();
                if (!t.isEmpty() && !t.startsWith("Folio") && !t.startsWith("ISIN")
                        && !t.startsWith("Date") && t.length() > 10) {
                    folio.schemeName = t;
                    break;
                }
            }
        }

        // Transactions
        Matcher txMatcher = TX_PATTERN.matcher(blockText);
        while (txMatcher.find()) {
            TxRow row = new TxRow();
            try {
                row.date        = LocalDate.parse(txMatcher.group(1), DATE_FMT);
                row.description = txMatcher.group(2).trim();
                row.amount      = parseBigDecimal(txMatcher.group(3));
                row.units       = parseBigDecimal(txMatcher.group(4));
                row.nav         = parseBigDecimal(txMatcher.group(5));
                row.balanceUnits= parseBigDecimal(txMatcher.group(6));
                folio.transactions.add(row);
            } catch (Exception e) {
                log.warn("Could not parse transaction row: {}", txMatcher.group(0));
            }
        }
    }

    /**
     * Persists all transactions for a folio, replacing any existing ones.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    List<UserTransaction> saveTransactions(FolioBlock folio, Long userId) {
        // Clear old data for this folio so re-uploads don't duplicate
        txRepo.deleteByUserIdAndFolioNumber(userId, folio.folioNumber);

        // Resolve amfiCode from ISIN
        String amfiCode = resolveAmfiCode(folio.isin);

        List<UserTransaction> saved = new ArrayList<>();
        for (TxRow row : folio.transactions) {
            UserTransaction tx = new UserTransaction();
            tx.setUserId(userId);
            tx.setFolioNumber(folio.folioNumber);
            tx.setAmfiCode(amfiCode);
            tx.setTransactionDate(row.date);
            tx.setTransactionType(detectType(row.description));
            tx.setAmount(row.amount);
            tx.setUnits(row.units);
            tx.setNav(row.nav);
            tx.setBalanceUnits(row.balanceUnits);
            tx.setDescription(row.description);
            saved.add(txRepo.save(tx));
        }
        return saved;
    }

    /**
     * Creates or updates the user_portfolio summary row for this folio.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    void savePortfolio(FolioBlock folio, Long userId, List<UserTransaction> txList) {
        String amfiCode = resolveAmfiCode(folio.isin);

        UserPortfolio portfolio = portfolioRepo
            .findByUserIdAndFolioNumber(userId, folio.folioNumber)
            .orElse(new UserPortfolio());

        portfolio.setUserId(userId);
        portfolio.setFolioNumber(folio.folioNumber);
        portfolio.setAmfiCode(amfiCode);
        portfolio.setSchemeName(folio.schemeName);
        portfolio.setAmcName(folio.amcName);
        portfolio.setTotalUnits(folio.closingUnits);
        portfolio.setLastUpdated(java.time.LocalDateTime.now());

        // Invested amount = sum of all buy transactions
        BigDecimal invested = txList.stream()
            .filter(tx -> isBuy(tx.getTransactionType()))
            .map(tx -> tx.getAmount() != null ? tx.getAmount() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Withdrawn amount = sum of sell transactions
        BigDecimal withdrawn = txList.stream()
            .filter(tx -> isSell(tx.getTransactionType()))
            .map(tx -> tx.getAmount() != null ? tx.getAmount() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal netInvested = invested.subtract(withdrawn).max(BigDecimal.ZERO);
        portfolio.setInvestedAmount(netInvested);

        // avgNav = netInvested / closingUnits (guard divide-by-zero)
        if (folio.closingUnits.compareTo(BigDecimal.ZERO) > 0) {
            portfolio.setAvgNav(
                netInvested.divide(folio.closingUnits, 4, RoundingMode.HALF_UP));
        } else {
            portfolio.setAvgNav(BigDecimal.ZERO);
        }

        // current value and gain are filled later by PortfolioCalculatorService
        portfolioRepo.save(portfolio);
    }

    /**
     * Creates FIFO tax lots for buy transactions (PURCHASE, SIP, SWITCH_IN).
     * Clears any old lots for this folio before inserting fresh ones.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    void saveTaxLots(FolioBlock folio, Long userId, List<UserTransaction> txList) {
        taxLotRepo.deleteByUserIdAndFolioNumber(userId, folio.folioNumber);

        String amfiCode = resolveAmfiCode(folio.isin);

        for (UserTransaction tx : txList) {
            if (!isBuy(tx.getTransactionType())) continue;
            if (tx.getUnits() == null || tx.getUnits().compareTo(BigDecimal.ZERO) <= 0) continue;

            TaxLot lot = new TaxLot();
            lot.setUserId(userId);
            lot.setAmfiCode(amfiCode);
            lot.setFolioNumber(folio.folioNumber);
            lot.setPurchaseDate(tx.getTransactionDate());
            lot.setUnits(tx.getUnits());
            lot.setPurchaseNav(tx.getNav() != null ? tx.getNav() : BigDecimal.ZERO);
            lot.setCostBasis(tx.getAmount() != null ? tx.getAmount() : BigDecimal.ZERO);
            lot.setRemainingUnits(tx.getUnits());
            lot.setIsExhausted(false);
            taxLotRepo.save(lot);
        }
    }

    // ── Utility Methods ─────────────────────────────────────────────────────

    /**
     * Resolves an AMFI scheme code from an ISIN string.
     * Returns null if not found — downstream code must handle null gracefully.
     */
    private String resolveAmfiCode(String isin) {
        if (isin == null || isin.isBlank()) return null;
        return schemeRepo.findByIsinGrowth(isin)
            .or(() -> schemeRepo.findByIsinIdcw(isin))
            .map(SchemeMaster::getAmfiCode)
            .orElse(null);
    }

    /**
     * Classifies a transaction description into a TransactionType enum value.
     */
    TransactionType detectType(String description) {
        if (description == null) return TransactionType.PURCHASE;
        String upper = description.toUpperCase();

        if (upper.contains("SIP") || upper.contains("SYSTEMATIC"))
            return TransactionType.SIP;
        if (upper.contains("SWITCH IN"))
            return TransactionType.SWITCH_IN;
        if (upper.contains("SWITCH OUT"))
            return TransactionType.SWITCH_OUT;
        if (upper.contains("REDEMPTION") || upper.contains("REDEEM"))
            return TransactionType.REDEMPTION;
        if (upper.contains("DIVIDEND") || upper.contains("IDCW"))
            return TransactionType.DIVIDEND;
        if (upper.contains("BONUS"))
            return TransactionType.BONUS;
        return TransactionType.PURCHASE;
    }

    private boolean isBuy(TransactionType type) {
        return type == TransactionType.PURCHASE
            || type == TransactionType.SIP
            || type == TransactionType.SWITCH_IN;
    }

    private boolean isSell(TransactionType type) {
        return type == TransactionType.REDEMPTION
            || type == TransactionType.SWITCH_OUT;
    }

    /**
     * Parses a BigDecimal from a string, stripping commas.
     * Returns BigDecimal.ZERO on parse failure.
     */
    private BigDecimal parseBigDecimal(String raw) {
        if (raw == null || raw.isBlank()) return BigDecimal.ZERO;
        try {
            return new BigDecimal(raw.replace(",", "").trim());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }
}