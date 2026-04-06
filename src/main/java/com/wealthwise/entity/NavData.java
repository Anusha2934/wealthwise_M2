package com.wealthwise.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "nav_data",
       uniqueConstraints = @UniqueConstraint(columnNames = {"amfi_code","nav_date"}))
@Data
@NoArgsConstructor
public class NavData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "amfi_code")
    private String amfiCode;

    @Column(name = "nav_date")
    private LocalDate navDate;

    @Column(name = "nav_value", precision = 15, scale = 4)
    private BigDecimal navValue;
}
