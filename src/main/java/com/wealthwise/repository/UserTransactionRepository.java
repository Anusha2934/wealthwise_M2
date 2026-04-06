package com.wealthwise.repository;

import com.wealthwise.entity.UserTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

public interface UserTransactionRepository extends JpaRepository<UserTransaction, Long> {
    List<UserTransaction> findByUserIdAndFolioNumber(Long userId, String folioNumber);
    List<UserTransaction> findByUserId(Long userId);

    // @Transactional required for Spring Data JPA derived delete queries
    @Transactional
    void deleteByUserIdAndFolioNumber(Long userId, String folioNumber);
}
