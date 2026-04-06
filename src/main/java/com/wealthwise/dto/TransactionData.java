package com.wealthwise.dto;

import com.wealthwise.entity.UserTransaction.TransactionType;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data @NoArgsConstructor
public class TransactionData {
    private LocalDate       transactionDate;
    private TransactionType transactionType;
    private BigDecimal      amount;
    private BigDecimal      units;
    private BigDecimal      nav;
    private BigDecimal      balanceUnits;
    private String          description;
}
