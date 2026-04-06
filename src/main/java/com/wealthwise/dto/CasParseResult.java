package com.wealthwise.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.List;

@Data @AllArgsConstructor @NoArgsConstructor
public class CasParseResult {
    private int             totalFolios;
    private int             totalTransactions;
    private List<FolioData> folios;
}
