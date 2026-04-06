package com.wealthwise.repository;

import com.wealthwise.entity.CasUploadLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CasUploadLogRepository extends JpaRepository<CasUploadLog, Long> {
    List<CasUploadLog> findByUserIdOrderByUploadedAtDesc(Long userId);
}
