package com.wealthwise.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data @AllArgsConstructor @NoArgsConstructor
public class PortfolioReportDTO {
    private List<HoldingDTO>        holdings;
    private BigDecimal              totalInvestedAmount;
    private BigDecimal              totalCurrentValue;
    private BigDecimal              totalUnrealisedGain;
    private Map<String, BigDecimal> allocationByCategory; // e.g. EQUITY → ₹3.2L
    private TaxSummary              taxSummary;
}
