package com.wealthwise.repository;

import com.wealthwise.entity.TaxLot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

public interface TaxLotRepository extends JpaRepository<TaxLot, Long> {
    List<TaxLot> findByUserIdAndIsExhaustedFalse(Long userId);
    List<TaxLot> findByUserIdAndFolioNumber(Long userId, String folioNumber);

    // @Transactional required for Spring Data JPA derived delete queries
    @Transactional
    void deleteByUserIdAndFolioNumber(Long userId, String folioNumber);
}
