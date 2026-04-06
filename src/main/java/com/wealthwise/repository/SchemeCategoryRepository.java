package com.wealthwise.repository;

import com.wealthwise.entity.SchemeCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface SchemeCategoryRepository extends JpaRepository<SchemeCategory, Long> {
    Optional<SchemeCategory> findByAmfiCode(String amfiCode);
    List<SchemeCategory> findByBroadCategory(SchemeCategory.BroadCategory cat);
}
