package com.wealthwise.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "scheme_master")
@Data
@NoArgsConstructor
public class SchemeMaster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String amfiCode;

    private String isinGrowth;
    private String isinIdcw;

    @Column(length = 500)
    private String schemeName;

    private String amcName;
    private String fundFamilyName;

    @Enumerated(EnumType.STRING)
    private PlanType planType;

    @Enumerated(EnumType.STRING)
    private OptionType optionType;

    @Enumerated(EnumType.STRING)
    private FundType fundType;

    private Boolean isActive = true;

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();

    public enum PlanType   { DIRECT, REGULAR }
    public enum OptionType { GROWTH, IDCW_PAYOUT, IDCW_REINVESTMENT }
    public enum FundType   { OPEN_ENDED, CLOSE_ENDED, INTERVAL }
}
