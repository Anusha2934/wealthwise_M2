package com.wealthwise.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_portfolio")
@Data
@NoArgsConstructor
public class UserPortfolio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private String folioNumber;
    private String amfiCode;

    @Column(length = 500)
    private String schemeName;
    private String amcName;

    @Column(precision = 15, scale = 4)
    private BigDecimal totalUnits;

    @Column(precision = 15, scale = 4)
    private BigDecimal avgNav;

    @Column(precision = 15, scale = 2)
    private BigDecimal investedAmount;

    @Column(precision = 15, scale = 2)
    private BigDecimal currentValue;

    @Column(precision = 15, scale = 2)
    private BigDecimal unrealisedGain;

    // XIRR as percentage e.g. 14.52 means 14.52% p.a.
    @Column(precision = 8, scale = 4)
    private BigDecimal xirr;

    private LocalDateTime lastUpdated;
}
