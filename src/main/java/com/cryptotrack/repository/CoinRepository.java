package com.cryptotrack.repository;

import com.cryptotrack.model.Coin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for Coin entity.
 * Spring Data JPA auto-generates all basic CRUD operations.
 */
@Repository
public interface CoinRepository extends JpaRepository<Coin, String> {

    // Get top N coins sorted by market cap rank
    List<Coin> findAllByOrderByMarketCapRankAsc();

    // Get top gainers in last 24h
    @Query("SELECT c FROM Coin c WHERE c.priceChangePercentage24h IS NOT NULL ORDER BY c.priceChangePercentage24h DESC")
    List<Coin> findTopGainers();

    // Get top losers in last 24h
    @Query("SELECT c FROM Coin c WHERE c.priceChangePercentage24h IS NOT NULL ORDER BY c.priceChangePercentage24h ASC")
    List<Coin> findTopLosers();
}
