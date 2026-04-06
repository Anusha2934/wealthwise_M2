package com.wealthwise.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "tax_lots")
@Data
@NoArgsConstructor
public class TaxLot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private String amfiCode;
    private String folioNumber;

    private LocalDate purchaseDate;

    @Column(precision = 15, scale = 4)
    private BigDecimal units;

    @Column(precision = 15, scale = 4)
    private BigDecimal purchaseNav;

    @Column(precision = 15, scale = 2)
    private BigDecimal costBasis;

    @Column(precision = 15, scale = 4)
    private BigDecimal remainingUnits;

    private Boolean isExhausted = false;
}
