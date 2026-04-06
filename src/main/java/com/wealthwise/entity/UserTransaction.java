package com.wealthwise.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "user_transactions")
@Data
@NoArgsConstructor
public class UserTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private String folioNumber;
    private String amfiCode;

    private LocalDate transactionDate;

    @Enumerated(EnumType.STRING)
    private TransactionType transactionType;

    @Column(precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(precision = 15, scale = 4)
    private BigDecimal units;

    @Column(precision = 15, scale = 4)
    private BigDecimal nav;

    @Column(precision = 15, scale = 4)
    private BigDecimal balanceUnits;

    @Column(length = 500)
    private String description;

    public enum TransactionType {
        PURCHASE, REDEMPTION, SIP,
        SWITCH_IN, SWITCH_OUT, DIVIDEND, BONUS
    }
}
