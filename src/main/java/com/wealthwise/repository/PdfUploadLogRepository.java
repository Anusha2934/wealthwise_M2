package com.wealthwise.repository;

import com.wealthwise.entity.PdfUploadLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PdfUploadLogRepository extends JpaRepository<PdfUploadLog, Long> {}
