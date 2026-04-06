package com.wealthwise.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "cas_upload_log")
@Data
@NoArgsConstructor
public class CasUploadLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private String fileName;
    private LocalDateTime uploadedAt = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    private Status status;

    private Integer totalFolios   = 0;
    private Integer totalTransactions = 0;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    public enum Status { PROCESSING, SUCCESS, FAILED }
}
