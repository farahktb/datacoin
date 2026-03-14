package com.cryptotrack.repository;

import com.cryptotrack.model.PriceHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for PriceHistory entity.
 * Supports time-series queries for charting and analysis.
 */
@Repository
public interface PriceHistoryRepository extends JpaRepository<PriceHistory, Long> {

    // Get price history for a specific coin, ordered by time
    List<PriceHistory> findByCoinIdOrderByRecordedAtAsc(String coinId);

    // Get price history for a coin within a time range
    @Query("SELECT p FROM PriceHistory p WHERE p.coinId = :coinId AND p.recordedAt >= :since ORDER BY p.recordedAt ASC")
    List<PriceHistory> findByCoinIdSince(@Param("coinId") String coinId, @Param("since") LocalDateTime since);

    // Get the latest price entry for each coin
    @Query("SELECT p FROM PriceHistory p WHERE p.recordedAt = (SELECT MAX(p2.recordedAt) FROM PriceHistory p2 WHERE p2.coinId = p.coinId)")
    List<PriceHistory> findLatestForAllCoins();

    // Count total records stored
    long countByCoinId(String coinId);
}
