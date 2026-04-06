package com.wealthwise.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Entity
@Table(name = "scheme_category")
@Data
@NoArgsConstructor
public class SchemeCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String amfiCode;

    @Enumerated(EnumType.STRING)
    private BroadCategory broadCategory;

    private String sebiCategory;
    private String subCategory;

    private BigDecimal equityPercentage;

    @Enumerated(EnumType.STRING)
    private TaxationType taxationType;

    // 1=Low, 2=Low-Moderate, 3=Moderate, 4=Mod-High, 5=High, 6=Very High
    private Integer riskLevel;

    private String benchmarkIndex;

    public enum BroadCategory { EQUITY, DEBT, HYBRID, SOLUTION, OTHER }
    public enum TaxationType  { EQUITY_TAX, DEBT_TAX }
}
