package com.wealthwise.dto;

import com.wealthwise.entity.SchemeCategory.BroadCategory;
import com.wealthwise.entity.SchemeCategory.TaxationType;
import com.wealthwise.entity.SchemeMaster.OptionType;
import com.wealthwise.entity.SchemeMaster.PlanType;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data @NoArgsConstructor
public class HoldingDTO {
    private String        schemeName;
    private String        amcName;
    private BigDecimal    units;
    private BigDecimal    investedAmount;
    private BigDecimal    currentValue;
    private BigDecimal    unrealisedGain;
    private BigDecimal    xirr;             // annualised return %

    // From scheme_master
    private PlanType      planType;
    private OptionType    optionType;

    // From scheme_category
    private BroadCategory broadCategory;
    private String        sebiCategory;
    private Integer       riskLevel;
    private TaxationType  taxationType;
    private String        benchmarkIndex;
}
