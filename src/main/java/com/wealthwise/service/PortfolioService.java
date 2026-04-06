package com.wealthwise.service;

import com.wealthwise.entity.*;
import com.wealthwise.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * M04 — F04.3 Portfolio Report Assembler
 *
 * Joins data from:
 *   user_portfolio  → what you hold
 *   scheme_master   → what plan/option type it is    (M02)
 *   scheme_category → risk, category, tax type       (M02)
 *   nav_data        → latest price                   (M02)
 *
 * Returns the full portfolio report map for the REST API.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PortfolioService {

    private final UserPortfolioRepository   portfolioRepo;
    private final SchemeMasterRepository    schemeRepo;
    private final SchemeCategoryRepository  categoryRepo;
    private final TaxCalculatorService      taxService;
    private final PortfolioCalculatorService calcService;

    public Map<String, Object> getFullReport(Long userId) {

        // Step 1: refresh current values with latest NAV
        calcService.refreshPortfolioValues(userId);

        // Step 2: load all holdings
        List<UserPortfolio> holdings = portfolioRepo.findByUserId(userId);

        // Step 3: enrich each holding
        List<Map<String, Object>> holdingList = holdings.stream()
            .map(this::enrichHolding)
            .collect(Collectors.toList());

        // Step 4: summary totals
        BigDecimal totalInvested = holdings.stream()
            .map(h -> h.getInvestedAmount() != null
                      ? h.getInvestedAmount() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCurrent = holdings.stream()
            .map(h -> h.getCurrentValue() != null
                      ? h.getCurrentValue() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalGain = totalCurrent.subtract(totalInvested);

        // Step 5: allocation by broad category (for pie chart)
        Map<String, BigDecimal> allocation = holdingList.stream()
            .collect(Collectors.groupingBy(
                h -> String.valueOf(h.getOrDefault("broadCategory", "UNKNOWN")),
                Collectors.reducing(BigDecimal.ZERO,
                    h -> h.get("currentValue") instanceof BigDecimal
                         ? (BigDecimal) h.get("currentValue") : BigDecimal.ZERO,
                    BigDecimal::add)));

        // Step 6: tax summary
        Map<String, Object> tax = taxService.calculateTax(userId);

        // Final report
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("totalInvestedAmount",  totalInvested);
        report.put("totalCurrentValue",    totalCurrent);
        report.put("totalUnrealisedGain",  totalGain);
        report.put("allocationByCategory", allocation);
        report.put("holdings",             holdingList);
        report.put("taxSummary",           tax);
        return report;
    }

    // ── Enrich a single holding with M02 data ─────────────────────────────
    private Map<String, Object> enrichHolding(UserPortfolio h) {
        Map<String, Object> map = new LinkedHashMap<>();

        map.put("folioNumber",    h.getFolioNumber());
        map.put("schemeName",     h.getSchemeName());
        map.put("amcName",        h.getAmcName());
        map.put("totalUnits",     h.getTotalUnits());
        map.put("investedAmount", h.getInvestedAmount());
        map.put("currentValue",   h.getCurrentValue());
        map.put("unrealisedGain", h.getUnrealisedGain());
        map.put("xirr",           h.getXirr());

        // From scheme_master (M02)
        schemeRepo.findByAmfiCode(h.getAmfiCode()).ifPresent(s -> {
            map.put("planType",   s.getPlanType());
            map.put("optionType", s.getOptionType());
            map.put("fundType",   s.getFundType());
        });

        // From scheme_category (M02)
        categoryRepo.findByAmfiCode(h.getAmfiCode()).ifPresent(c -> {
            map.put("broadCategory",   c.getBroadCategory());
            map.put("sebiCategory",    c.getSebiCategory());
            map.put("subCategory",     c.getSubCategory());
            map.put("riskLevel",       c.getRiskLevel());
            map.put("taxationType",    c.getTaxationType());
            map.put("equityPct",       c.getEquityPercentage());
            map.put("benchmarkIndex",  c.getBenchmarkIndex());
        });

        return map;
    }
}
