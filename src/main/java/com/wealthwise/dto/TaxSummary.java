package com.wealthwise.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data @AllArgsConstructor @NoArgsConstructor
public class TaxSummary {
    private BigDecimal totalLtcgGains;
    private BigDecimal totalStcgGains;
    private BigDecimal taxableLtcg;       // after ₹1.25L exemption
    private BigDecimal ltcgTaxPayable;
    private BigDecimal stcgTaxPayable;
    private BigDecimal totalTaxPayable;
}
