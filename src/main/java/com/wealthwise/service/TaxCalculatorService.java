package com.wealthwise.service;

import com.wealthwise.entity.SchemeCategory;
import com.wealthwise.entity.TaxLot;
import com.wealthwise.repository.NavDataRepository;
import com.wealthwise.repository.SchemeCategoryRepository;
import com.wealthwise.repository.TaxLotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * M04 — Tax Calculator
 *
 * Calculates unrealised LTCG and STCG for a user's portfolio
 * based on FIFO tax lots and current NAV.
 *
 * Rules (post-budget 2024):
 *   Equity fund (≥65% equity):
 *     Holding ≥ 1 year  → LTCG @ 12.5% (exempt up to ₹1.25L)
 *     Holding < 1 year  → STCG @ 20%
 *
 *   Debt fund (<65% equity):
 *     All gains → added to income (taxed at slab rate)
 *     No separate LTCG / STCG benefit
 */
@Service
@RequiredArgsConstructor
public class TaxCalculatorService {

    private final TaxLotRepository         taxLotRepo;
    private final SchemeCategoryRepository categoryRepo;
    private final NavDataRepository        navRepo;

    private static final BigDecimal LTCG_EXEMPTION  = new BigDecimal("125000"); // ₹1.25L
    private static final BigDecimal LTCG_EQUITY_RATE = new BigDecimal("0.125"); // 12.5%
    private static final BigDecimal STCG_EQUITY_RATE = new BigDecimal("0.20");  // 20%

    public Map<String, Object> calculateTax(Long userId) {

        List<TaxLot> lots = taxLotRepo.findByUserIdAndIsExhaustedFalse(userId);

        BigDecimal equityLtcg = BigDecimal.ZERO;
        BigDecimal equityStcg = BigDecimal.ZERO;
        BigDecimal debtGains  = BigDecimal.ZERO;

        for (TaxLot lot : lots) {
            SchemeCategory cat = categoryRepo
                .findByAmfiCode(lot.getAmfiCode()).orElse(null);
            if (cat == null) continue;

            var latestNav = navRepo.findTopByAmfiCodeOrderByNavDateDesc(lot.getAmfiCode())
                                   .orElse(null);
            if (latestNav == null) continue;

            BigDecimal currentVal = lot.getRemainingUnits()
                .multiply(latestNav.getNavValue())
                .setScale(2, RoundingMode.HALF_UP);

            BigDecimal costVal = lot.getRemainingUnits()
                .multiply(lot.getPurchaseNav())
                .setScale(2, RoundingMode.HALF_UP);

            BigDecimal gain = currentVal.subtract(costVal);
            if (gain.compareTo(BigDecimal.ZERO) <= 0) continue; // no gain = no tax

            long holdingDays = ChronoUnit.DAYS.between(
                lot.getPurchaseDate(), LocalDate.now());

            boolean isEquity = cat.getTaxationType()
                == SchemeCategory.TaxationType.EQUITY_TAX;

            if (isEquity) {
                if (holdingDays >= 365) equityLtcg = equityLtcg.add(gain);
                else                    equityStcg = equityStcg.add(gain);
            } else {
                debtGains = debtGains.add(gain); // taxed at income slab
            }
        }

        // Apply ₹1.25L LTCG exemption on equity
        BigDecimal taxableLtcg = equityLtcg.subtract(LTCG_EXEMPTION)
                                            .max(BigDecimal.ZERO);
        BigDecimal ltcgTax = taxableLtcg.multiply(LTCG_EQUITY_RATE)
                                         .setScale(2, RoundingMode.HALF_UP);
        BigDecimal stcgTax = equityStcg.multiply(STCG_EQUITY_RATE)
                                        .setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalTax = ltcgTax.add(stcgTax);

        Map<String, Object> summary = new HashMap<>();
        summary.put("equityLtcgGains",       equityLtcg);
        summary.put("equityStcgGains",        equityStcg);
        summary.put("debtGains",              debtGains);
        summary.put("ltcgExemption",          LTCG_EXEMPTION);
        summary.put("taxableLtcg",            taxableLtcg);
        summary.put("ltcgTaxPayable",         ltcgTax);
        summary.put("stcgTaxPayable",         stcgTax);
        summary.put("totalEquityTaxPayable",  totalTax);
        summary.put("debtTaxNote",
            "Debt gains of ₹" + debtGains + " will be taxed at your income slab rate.");
        return summary;
    }
}
