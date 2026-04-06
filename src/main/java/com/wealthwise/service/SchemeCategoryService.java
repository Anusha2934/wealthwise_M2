package com.wealthwise.service;

import com.wealthwise.entity.SchemeCategory;
import com.wealthwise.entity.SchemeCategory.BroadCategory;
import com.wealthwise.entity.SchemeCategory.TaxationType;
import com.wealthwise.entity.SchemeMaster;
import com.wealthwise.repository.SchemeCategoryRepository;
import com.wealthwise.repository.SchemeMasterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * F02.2 — Scheme Category Classifier
 *
 * Maps each scheme to SEBI's 36 categories.
 * Determines: broad category, SEBI category, equity %, taxation type,
 * risk level (1–6), and benchmark index.
 *
 * Call classifyAll() once after initial seed, or classifyScheme() per scheme.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SchemeCategoryService {

    private final SchemeMasterRepository   schemeRepo;
    private final SchemeCategoryRepository categoryRepo;

    @Transactional
    public void classifyAll() {
        List<SchemeMaster> schemes = schemeRepo.findAll();
        log.info("Classifying {} schemes ...", schemes.size());
        schemes.forEach(this::classifyScheme);
        log.info("Classification complete.");
    }

    @Transactional
    public SchemeCategory classifyScheme(SchemeMaster scheme) {
        SchemeCategory cat = categoryRepo
            .findByAmfiCode(scheme.getAmfiCode())
            .orElse(new SchemeCategory());

        cat.setAmfiCode(scheme.getAmfiCode());

        String name = scheme.getSchemeName().toUpperCase();

        // ── SEBI Category + Broad Category ───────────────────────────────
        assignCategory(cat, name);

        // ── Equity % and Taxation ────────────────────────────────────────
        assignEquityAndTax(cat, name);

        // ── Risk Level (1–6) ─────────────────────────────────────────────
        assignRisk(cat, name);

        // ── Benchmark Index ───────────────────────────────────────────────
        assignBenchmark(cat, name);

        return categoryRepo.save(cat);
    }

    // ─────────────────────────────────────────────────────────────────────
    // SEBI 36-category keyword mapping
    // ─────────────────────────────────────────────────────────────────────
    private void assignCategory(SchemeCategory cat, String name) {

        // ── EQUITY ──────────────────────────────────────────────────────
        if (name.contains("LARGE CAP") && name.contains("MID CAP")) {
            cat.setBroadCategory(BroadCategory.EQUITY);
            cat.setSebiCategory("Large & Mid Cap Fund");
            cat.setSubCategory("Large & Mid Cap");

        } else if (name.contains("LARGE CAP") || name.contains("BLUECHIP")
                || name.contains("BLUE CHIP") || name.contains("TOP 100")
                || name.contains("TOP100")) {
            cat.setBroadCategory(BroadCategory.EQUITY);
            cat.setSebiCategory("Large Cap Fund");
            cat.setSubCategory("Large Cap");

        } else if (name.contains("MID CAP") || name.contains("MIDCAP")) {
            cat.setBroadCategory(BroadCategory.EQUITY);
            cat.setSebiCategory("Mid Cap Fund");
            cat.setSubCategory("Mid Cap");

        } else if (name.contains("SMALL CAP") || name.contains("SMALLCAP")) {
            cat.setBroadCategory(BroadCategory.EQUITY);
            cat.setSebiCategory("Small Cap Fund");
            cat.setSubCategory("Small Cap");

        } else if (name.contains("MULTI CAP") || name.contains("MULTICAP")) {
            cat.setBroadCategory(BroadCategory.EQUITY);
            cat.setSebiCategory("Multi Cap Fund");
            cat.setSubCategory("Multi Cap");

        } else if (name.contains("FLEXI CAP") || name.contains("FLEXICAP")) {
            cat.setBroadCategory(BroadCategory.EQUITY);
            cat.setSebiCategory("Flexi Cap Fund");
            cat.setSubCategory("Flexi Cap");

        } else if (name.contains("ELSS") || name.contains("TAX SAVER")
                || name.contains("TAX SAVING") || name.contains("TAXSAVER")) {
            cat.setBroadCategory(BroadCategory.EQUITY);
            cat.setSebiCategory("ELSS");
            cat.setSubCategory("Tax Saving");

        } else if (name.contains("DIVIDEND YIELD")) {
            cat.setBroadCategory(BroadCategory.EQUITY);
            cat.setSebiCategory("Dividend Yield Fund");
            cat.setSubCategory("Dividend Yield");

        } else if (name.contains("VALUE FUND") || name.contains("CONTRA")) {
            cat.setBroadCategory(BroadCategory.EQUITY);
            cat.setSebiCategory("Value/Contra Fund");
            cat.setSubCategory("Value/Contra");

        } else if (name.contains("FOCUSED FUND") || name.contains("FOCUS FUND")) {
            cat.setBroadCategory(BroadCategory.EQUITY);
            cat.setSebiCategory("Focused Fund");
            cat.setSubCategory("Focused");

        } else if (name.contains("SECTORAL") || name.contains("THEMATIC")
                || name.contains("PHARMA") || name.contains("BANKING")
                || name.contains("TECHNOLOGY") || name.contains("INFRA")
                || name.contains("CONSUMPTION") || name.contains("ENERGY")
                || name.contains("HEALTHCARE") || name.contains("ESG")) {
            cat.setBroadCategory(BroadCategory.EQUITY);
            cat.setSebiCategory("Sectoral/Thematic Fund");
            cat.setSubCategory("Sectoral/Thematic");

        // ── DEBT ─────────────────────────────────────────────────────────
        } else if (name.contains("OVERNIGHT")) {
            cat.setBroadCategory(BroadCategory.DEBT);
            cat.setSebiCategory("Overnight Fund");
            cat.setSubCategory("Overnight");

        } else if (name.contains("LIQUID")) {
            cat.setBroadCategory(BroadCategory.DEBT);
            cat.setSebiCategory("Liquid Fund");
            cat.setSubCategory("Liquid");

        } else if (name.contains("ULTRA SHORT")) {
            cat.setBroadCategory(BroadCategory.DEBT);
            cat.setSebiCategory("Ultra Short Duration Fund");
            cat.setSubCategory("Ultra Short Duration");

        } else if (name.contains("LOW DURATION")) {
            cat.setBroadCategory(BroadCategory.DEBT);
            cat.setSebiCategory("Low Duration Fund");
            cat.setSubCategory("Low Duration");

        } else if (name.contains("MONEY MARKET")) {
            cat.setBroadCategory(BroadCategory.DEBT);
            cat.setSebiCategory("Money Market Fund");
            cat.setSubCategory("Money Market");

        } else if (name.contains("SHORT DURATION") || name.contains("SHORT TERM")) {
            cat.setBroadCategory(BroadCategory.DEBT);
            cat.setSebiCategory("Short Duration Fund");
            cat.setSubCategory("Short Duration");

        } else if (name.contains("MEDIUM DURATION") || name.contains("MEDIUM TERM")) {
            cat.setBroadCategory(BroadCategory.DEBT);
            cat.setSebiCategory("Medium Duration Fund");
            cat.setSubCategory("Medium Duration");

        } else if (name.contains("LONG DURATION") || name.contains("LONG TERM")) {
            cat.setBroadCategory(BroadCategory.DEBT);
            cat.setSebiCategory("Long Duration Fund");
            cat.setSubCategory("Long Duration");

        } else if (name.contains("DYNAMIC BOND")) {
            cat.setBroadCategory(BroadCategory.DEBT);
            cat.setSebiCategory("Dynamic Bond Fund");
            cat.setSubCategory("Dynamic Bond");

        } else if (name.contains("CORPORATE BOND")) {
            cat.setBroadCategory(BroadCategory.DEBT);
            cat.setSebiCategory("Corporate Bond Fund");
            cat.setSubCategory("Corporate Bond");

        } else if (name.contains("CREDIT RISK")) {
            cat.setBroadCategory(BroadCategory.DEBT);
            cat.setSebiCategory("Credit Risk Fund");
            cat.setSubCategory("Credit Risk");

        } else if (name.contains("BANKING AND PSU") || name.contains("BANKING & PSU")) {
            cat.setBroadCategory(BroadCategory.DEBT);
            cat.setSebiCategory("Banking and PSU Fund");
            cat.setSubCategory("Banking & PSU");

        } else if (name.contains("GILT") && name.contains("10 YEAR")) {
            cat.setBroadCategory(BroadCategory.DEBT);
            cat.setSebiCategory("Gilt Fund with 10 year constant duration");
            cat.setSubCategory("Gilt 10yr");

        } else if (name.contains("GILT")) {
            cat.setBroadCategory(BroadCategory.DEBT);
            cat.setSebiCategory("Gilt Fund");
            cat.setSubCategory("Gilt");

        } else if (name.contains("FLOATER")) {
            cat.setBroadCategory(BroadCategory.DEBT);
            cat.setSebiCategory("Floater Fund");
            cat.setSubCategory("Floater");

        // ── HYBRID ────────────────────────────────────────────────────────
        } else if (name.contains("CONSERVATIVE HYBRID")) {
            cat.setBroadCategory(BroadCategory.HYBRID);
            cat.setSebiCategory("Conservative Hybrid Fund");
            cat.setSubCategory("Conservative Hybrid");

        } else if (name.contains("BALANCED HYBRID")) {
            cat.setBroadCategory(BroadCategory.HYBRID);
            cat.setSebiCategory("Balanced Hybrid Fund");
            cat.setSubCategory("Balanced Hybrid");

        } else if (name.contains("AGGRESSIVE HYBRID") || name.contains("EQUITY HYBRID")) {
            cat.setBroadCategory(BroadCategory.HYBRID);
            cat.setSebiCategory("Aggressive Hybrid Fund");
            cat.setSubCategory("Aggressive Hybrid");

        } else if (name.contains("BALANCED ADVANTAGE") || name.contains("DYNAMIC ASSET")) {
            cat.setBroadCategory(BroadCategory.HYBRID);
            cat.setSebiCategory("Dynamic Asset Allocation Fund");
            cat.setSubCategory("Balanced Advantage");

        } else if (name.contains("MULTI ASSET") || name.contains("MULTI-ASSET")) {
            cat.setBroadCategory(BroadCategory.HYBRID);
            cat.setSebiCategory("Multi Asset Allocation Fund");
            cat.setSubCategory("Multi Asset");

        } else if (name.contains("ARBITRAGE")) {
            cat.setBroadCategory(BroadCategory.HYBRID);
            cat.setSebiCategory("Arbitrage Fund");
            cat.setSubCategory("Arbitrage");

        // ── SOLUTION ORIENTED ─────────────────────────────────────────────
        } else if (name.contains("RETIREMENT")) {
            cat.setBroadCategory(BroadCategory.SOLUTION);
            cat.setSebiCategory("Retirement Fund");
            cat.setSubCategory("Retirement");

        } else if (name.contains("CHILDREN") || name.contains("CHILD")) {
            cat.setBroadCategory(BroadCategory.SOLUTION);
            cat.setSebiCategory("Children's Fund");
            cat.setSubCategory("Children");

        // ── OTHER (Index / ETF / FoF) ─────────────────────────────────────
        } else if (name.contains("INDEX FUND") || name.contains("ETF")
                || name.contains("NIFTY") || name.contains("SENSEX")
                || name.contains("FOF") || name.contains("FUND OF FUND")) {
            cat.setBroadCategory(BroadCategory.OTHER);
            cat.setSebiCategory("Index/ETF/FoF");
            cat.setSubCategory("Index/ETF/FoF");

        } else {
            // Fallback — unclassified
            cat.setBroadCategory(BroadCategory.OTHER);
            cat.setSebiCategory("Unclassified");
            cat.setSubCategory("Unclassified");
        }
    }

    // ── Equity % + Taxation ──────────────────────────────────────────────
    private void assignEquityAndTax(SchemeCategory cat, String name) {
        BigDecimal equityPct;
        TaxationType tax;

        switch (cat.getSubCategory() == null ? "" : cat.getSubCategory()) {
            case "Large Cap", "Mid Cap", "Small Cap", "Large & Mid Cap",
                 "Multi Cap", "Flexi Cap", "Tax Saving", "Dividend Yield",
                 "Value/Contra", "Focused", "Sectoral/Thematic" -> {
                equityPct = new BigDecimal("95.00");
                tax = TaxationType.EQUITY_TAX;
            }
            case "Aggressive Hybrid" -> {
                equityPct = new BigDecimal("75.00");
                tax = TaxationType.EQUITY_TAX;
            }
            case "Balanced Advantage", "Multi Asset" -> {
                equityPct = new BigDecimal("65.00");
                tax = TaxationType.EQUITY_TAX;
            }
            case "Arbitrage" -> {
                equityPct = new BigDecimal("65.00");
                tax = TaxationType.EQUITY_TAX; // hedged equity — still equity tax
            }
            case "Balanced Hybrid" -> {
                equityPct = new BigDecimal("50.00");
                tax = TaxationType.DEBT_TAX;
            }
            case "Conservative Hybrid" -> {
                equityPct = new BigDecimal("25.00");
                tax = TaxationType.DEBT_TAX;
            }
            case "Index/ETF/FoF" -> {
                // Index funds on equity indices qualify as equity
                equityPct = (name.contains("NIFTY") || name.contains("SENSEX")
                             || name.contains("BSE") || name.contains("EQUITY"))
                    ? new BigDecimal("95.00") : new BigDecimal("0.00");
                tax = equityPct.compareTo(new BigDecimal("65")) >= 0
                    ? TaxationType.EQUITY_TAX : TaxationType.DEBT_TAX;
            }
            default -> {
                // Debt funds
                equityPct = new BigDecimal("0.00");
                tax = TaxationType.DEBT_TAX;
            }
        }

        cat.setEquityPercentage(equityPct);
        cat.setTaxationType(tax);
    }

    // ── Risk Level 1–6 ────────────────────────────────────────────────────
    private void assignRisk(SchemeCategory cat, String name) {
        int risk = switch (cat.getSubCategory() == null ? "" : cat.getSubCategory()) {
            case "Overnight"                        -> 1;
            case "Liquid", "Ultra Short Duration"   -> 2;
            case "Low Duration", "Money Market",
                 "Short Duration", "Gilt",
                 "Banking & PSU", "Floater"         -> 3;
            case "Medium Duration", "Long Duration",
                 "Dynamic Bond", "Corporate Bond",
                 "Conservative Hybrid",
                 "Balanced Hybrid", "Arbitrage",
                 "Large Cap", "Index/ETF/FoF"       -> 4;
            case "Large & Mid Cap", "Multi Cap",
                 "Flexi Cap", "Dividend Yield",
                 "Value/Contra", "Focused",
                 "Balanced Advantage", "Multi Asset",
                 "Aggressive Hybrid", "Tax Saving",
                 "Mid Cap"                          -> 5;
            case "Small Cap", "Sectoral/Thematic",
                 "Credit Risk", "Gilt 10yr"         -> 6;
            default                                 -> 4;
        };
        cat.setRiskLevel(risk);
    }

    // ── Benchmark Index ───────────────────────────────────────────────────
    private void assignBenchmark(SchemeCategory cat, String name) {
        String bench = switch (cat.getSubCategory() == null ? "" : cat.getSubCategory()) {
            case "Large Cap"          -> "NIFTY 100 TRI";
            case "Mid Cap"            -> "NIFTY MIDCAP 150 TRI";
            case "Small Cap"          -> "NIFTY SMALLCAP 250 TRI";
            case "Large & Mid Cap"    -> "NIFTY LARGEMIDCAP 250 TRI";
            case "Multi Cap"          -> "NIFTY 500 MULTICAP 50:25:25 TRI";
            case "Flexi Cap"          -> "NIFTY 500 TRI";
            case "Tax Saving"         -> "NIFTY 500 TRI";
            case "Dividend Yield"     -> "NIFTY DIVIDEND OPPORTUNITIES 50 TRI";
            case "Value/Contra"       -> "NIFTY 500 VALUE 50 TRI";
            case "Focused"            -> "NIFTY 500 TRI";
            case "Sectoral/Thematic"  -> "NIFTY 500 TRI";
            case "Aggressive Hybrid"  -> "NIFTY 50 HYBRID COMPOSITE DEBT 65:35 INDEX";
            case "Balanced Hybrid"    -> "NIFTY 50 HYBRID COMPOSITE DEBT 50:50 INDEX";
            case "Conservative Hybrid"-> "NIFTY 50 HYBRID COMPOSITE DEBT 15:85 INDEX";
            case "Balanced Advantage" -> "NIFTY 50 HYBRID COMPOSITE DEBT 50:50 INDEX";
            case "Arbitrage"          -> "NIFTY 50 ARBITRAGE INDEX";
            case "Multi Asset"        -> "NIFTY 500 TRI";
            case "Overnight"          -> "NIFTY 1D RATE INDEX";
            case "Liquid"             -> "NIFTY LIQUID INDEX";
            case "Ultra Short Duration"-> "NIFTY ULTRA SHORT DURATION DEBT INDEX";
            case "Low Duration"       -> "NIFTY LOW DURATION DEBT INDEX";
            case "Money Market"       -> "NIFTY MONEY MARKET INDEX";
            case "Short Duration"     -> "NIFTY SHORT DURATION DEBT INDEX";
            case "Medium Duration"    -> "NIFTY MEDIUM DURATION DEBT INDEX";
            case "Long Duration"      -> "NIFTY LONG DURATION DEBT INDEX";
            case "Dynamic Bond"       -> "NIFTY COMPOSITE DEBT INDEX";
            case "Corporate Bond"     -> "NIFTY CORPORATE BOND INDEX";
            case "Credit Risk"        -> "NIFTY CREDIT RISK BOND INDEX";
            case "Banking & PSU"      -> "NIFTY BANKING & PSU DEBT INDEX";
            case "Gilt"               -> "NIFTY ALL DURATION G-SEC INDEX";
            case "Gilt 10yr"          -> "NIFTY 10 YEAR BENCHMARK G-SEC INDEX";
            case "Floater"            -> "NIFTY COMPOSITE DEBT INDEX";
            case "Retirement"         -> "NIFTY 500 TRI";
            case "Children"           -> "NIFTY 500 TRI";
            case "Index/ETF/FoF"      -> deriveBenchmarkFromName(name);
            default                   -> "NIFTY 500 TRI";
        };
        cat.setBenchmarkIndex(bench);
    }

    private String deriveBenchmarkFromName(String name) {
        if (name.contains("NIFTY 50"))      return "NIFTY 50 TRI";
        if (name.contains("NIFTY NEXT 50")) return "NIFTY NEXT 50 TRI";
        if (name.contains("NIFTY 100"))     return "NIFTY 100 TRI";
        if (name.contains("NIFTY 200"))     return "NIFTY 200 TRI";
        if (name.contains("NIFTY 500"))     return "NIFTY 500 TRI";
        if (name.contains("SENSEX"))        return "S&P BSE SENSEX TRI";
        if (name.contains("MIDCAP"))        return "NIFTY MIDCAP 150 TRI";
        if (name.contains("SMALLCAP"))      return "NIFTY SMALLCAP 250 TRI";
        return "NIFTY 50 TRI";
    }
}
