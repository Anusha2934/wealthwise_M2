package com.wealthwise.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data @NoArgsConstructor
public class FolioData {
    private String                folioNumber;
    private String                amfiCode;
    private String                schemeName;
    private String                amcName;
    private BigDecimal            closingUnits;
    private List<TransactionData> transactions = new ArrayList<>();
}
