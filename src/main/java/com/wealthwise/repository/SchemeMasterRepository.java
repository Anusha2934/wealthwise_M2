package com.wealthwise.repository;

import com.wealthwise.entity.SchemeMaster;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface SchemeMasterRepository extends JpaRepository<SchemeMaster, Long> {
    Optional<SchemeMaster> findByAmfiCode(String amfiCode);
    Optional<SchemeMaster> findByIsinGrowth(String isin);
    Optional<SchemeMaster> findByIsinIdcw(String isin);
    Page<SchemeMaster> findByAmcNameContainingIgnoreCase(String amc, Pageable p);
    long countByIsActive(boolean active);
    long countByPlanType(SchemeMaster.PlanType planType);
}
