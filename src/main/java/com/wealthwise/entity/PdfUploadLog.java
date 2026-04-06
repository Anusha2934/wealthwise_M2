package com.wealthwise.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "pdf_upload_log")
@Data
@NoArgsConstructor
public class PdfUploadLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fileName;
    private LocalDateTime uploadedAt = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    private Status status;

    private Integer recordsParsed;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    public enum Status { PROCESSING, SUCCESS, FAILED }
}
