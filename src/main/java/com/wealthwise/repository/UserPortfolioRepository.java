package com.wealthwise.repository;

import com.wealthwise.entity.UserPortfolio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

public interface UserPortfolioRepository extends JpaRepository<UserPortfolio, Long> {
    List<UserPortfolio> findByUserId(Long userId);
    Optional<UserPortfolio> findByUserIdAndFolioNumber(Long userId, String folioNumber);

    // @Transactional is required for derived DELETE queries in Spring Data JPA
    @Transactional
    void deleteByUserIdAndFolioNumber(Long userId, String folioNumber);
}
