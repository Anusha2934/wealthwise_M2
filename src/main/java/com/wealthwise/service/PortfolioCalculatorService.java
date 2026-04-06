package com.wealthwise.service;

import com.wealthwise.entity.UserPortfolio;
import com.wealthwise.entity.UserTransaction;
import com.wealthwise.entity.UserTransaction.TransactionType;
import com.wealthwise.repository.NavDataRepository;
import com.wealthwise.repository.UserPortfolioRepository;
import com.wealthwise.repository.UserTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * M04 — F04.2 Portfolio Calculator
 *
 * For each holding:
 *   - Fetches the latest NAV and computes current value
 *   - Computes unrealised gain / loss
 *   - Computes XIRR (actual annualised return)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PortfolioCalculatorService {

    private final UserPortfolioRepository  portfolioRepo;
    private final UserTransactionRepository txRepo;
    private final NavDataRepository        navRepo;

    @Transactional
    public void refreshPortfolioValues(Long userId) {
        List<UserPortfolio> holdings = portfolioRepo.findByUserId(userId);

        for (UserPortfolio h : holdings) {
            navRepo.findTopByAmfiCodeOrderByNavDateDesc(h.getAmfiCode()).ifPresent(nav -> {

                // Current value = closing units × latest NAV
                BigDecimal currentValue = h.getTotalUnits()
                    .multiply(nav.getNavValue())
                    .setScale(2, RoundingMode.HALF_UP);

                BigDecimal gain = currentValue
                    .subtract(h.getInvestedAmount())
                    .setScale(2, RoundingMode.HALF_UP);

                h.setCurrentValue(currentValue);
                h.setUnrealisedGain(gain);
                h.setAvgNav(nav.getNavValue());

                // XIRR
                BigDecimal xirr = calculateXirr(userId,
                    h.getFolioNumber(), currentValue);
                h.setXirr(xirr);
                h.setLastUpdated(java.time.LocalDateTime.now());
            });
        }
        portfolioRepo.saveAll(holdings);
    }

    // ── XIRR ──────────────────────────────────────────────────────────────
    private BigDecimal calculateXirr(Long userId,
                                     String folioNumber,
                                     BigDecimal currentValue) {
        List<UserTransaction> txList =
            txRepo.findByUserIdAndFolioNumber(userId, folioNumber);
        if (txList.isEmpty()) return BigDecimal.ZERO;

        txList.sort(java.util.Comparator.comparing(
            UserTransaction::getTransactionDate));

        LocalDate t0 = txList.get(0).getTransactionDate();

        // Build cashflow array: [amount, days_from_t0]
        List<double[]> cashflows = new ArrayList<>();

        for (UserTransaction tx : txList) {
            long days = ChronoUnit.DAYS.between(t0, tx.getTransactionDate());
            double amt = tx.getAmount().doubleValue();

            if (tx.getTransactionType() == TransactionType.PURCHASE
             || tx.getTransactionType() == TransactionType.SIP
             || tx.getTransactionType() == TransactionType.SWITCH_IN) {
                cashflows.add(new double[]{-amt, days});   // money out of pocket
            } else if (tx.getTransactionType() == TransactionType.REDEMPTION
                    || tx.getTransactionType() == TransactionType.SWITCH_OUT) {
                cashflows.add(new double[]{ amt, days});   // money received
            }
        }

        // Final cashflow: today's portfolio value (positive = money in hand)
        long today = ChronoUnit.DAYS.between(t0, LocalDate.now());
        cashflows.add(new double[]{currentValue.doubleValue(), today});

        try {
            double xirr = xirrNewtonRaphson(cashflows);
            return BigDecimal.valueOf(xirr * 100)           // convert to %
                             .setScale(2, RoundingMode.HALF_UP);
        } catch (Exception e) {
            log.warn("XIRR calculation failed for folio {}: {}",
                     folioNumber, e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    /**
     * Newton-Raphson method to solve for XIRR.
     * Solves: Σ [ CF_i / (1+r)^(t_i/365) ] = 0
     */
    private double xirrNewtonRaphson(List<double[]> cashflows) {
        double rate = 0.10; // initial guess: 10% p.a.

        for (int iter = 0; iter < 1000; iter++) {
            double f  = 0.0;  // NPV
            double df = 0.0;  // derivative of NPV

            for (double[] cf : cashflows) {
                double t = cf[1] / 365.0;
                double pv = cf[0] / Math.pow(1 + rate, t);
                f  += pv;
                df -= t * pv / (1 + rate);
            }

            if (Math.abs(df) < 1e-12) break;

            double newRate = rate - f / df;

            if (Math.abs(newRate - rate) < 1e-8) return newRate;
            rate = newRate;

            // Guard against divergence
            if (rate < -0.999) rate = -0.999;
            if (rate >  100.0) rate =  100.0;
        }
        return rate;
    }
}
