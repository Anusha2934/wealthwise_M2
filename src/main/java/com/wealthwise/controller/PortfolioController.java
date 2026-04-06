package com.wealthwise.controller;

import com.wealthwise.service.PortfolioService;
import com.wealthwise.service.TaxCalculatorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * M04 — Portfolio REST Controller
 *
 * Base URL: /api/v1/portfolio
 */
@RestController
@RequestMapping("/api/v1/portfolio")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PortfolioController {

    private final PortfolioService      portfolioService;
    private final TaxCalculatorService  taxService;

    /**
     * GET /api/v1/portfolio/{userId}/report
     *
     * Returns the full portfolio report:
     *   - totalInvestedAmount
     *   - totalCurrentValue
     *   - totalUnrealisedGain
     *   - allocationByCategory  (for pie chart)
     *   - holdings[]            (every fund with enriched M02 data)
     *   - taxSummary            (LTCG / STCG breakdown)
     */
    @GetMapping("/{userId}/report")
    public ResponseEntity<Map<String, Object>> fullReport(
            @PathVariable Long userId) {
        return ResponseEntity.ok(portfolioService.getFullReport(userId));
    }

    /**
     * GET /api/v1/portfolio/{userId}/holdings
     * Returns just the holdings list (lighter call for the holdings screen).
     */
    @GetMapping("/{userId}/holdings")
    public ResponseEntity<?> holdings(@PathVariable Long userId) {
        Map<String, Object> report = portfolioService.getFullReport(userId);
        return ResponseEntity.ok(report.get("holdings"));
    }

    /**
     * GET /api/v1/portfolio/{userId}/tax-summary
     * Returns only the tax breakdown (for the tax screen).
     */
    @GetMapping("/{userId}/tax-summary")
    public ResponseEntity<Map<String, Object>> taxSummary(
            @PathVariable Long userId) {
        return ResponseEntity.ok(taxService.calculateTax(userId));
    }

    /**
     * GET /api/v1/portfolio/{userId}/allocation
     * Returns category-wise allocation percentages (for pie/donut chart).
     */
    @GetMapping("/{userId}/allocation")
    public ResponseEntity<?> allocation(@PathVariable Long userId) {
        Map<String, Object> report = portfolioService.getFullReport(userId);
        return ResponseEntity.ok(report.get("allocationByCategory"));
    }
}
