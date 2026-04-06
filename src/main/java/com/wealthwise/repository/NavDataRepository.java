package com.wealthwise.repository;

import com.wealthwise.entity.NavData;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface NavDataRepository extends JpaRepository<NavData, Long> {

    // Uses Spring Data's derived-query naming — JPQL-compliant, no raw LIMIT needed
    Optional<NavData> findTopByAmfiCodeOrderByNavDateDesc(String amfiCode);
}
