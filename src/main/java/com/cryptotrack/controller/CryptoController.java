package com.cryptotrack.controller;

import com.cryptotrack.dto.CoinGeckoDTO;
import com.cryptotrack.model.Coin;
import com.cryptotrack.model.PriceHistory;
import com.cryptotrack.repository.CoinRepository;
import com.cryptotrack.repository.PriceHistoryRepository;
import com.cryptotrack.service.CoinGeckoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

/**
 * CryptoController - The REST API layer.
 *
 * Exposes the following endpoints:
 *   GET /api/coins              → All tracked coins (current prices)
 *   GET /api/coins/{id}         → Single coin details
 *   GET /api/coins/{id}/history → Price history for charting
 *   GET /api/market/stats       → Global market stats
 *   GET /api/market/gainers     → Top 5 gainers today
 *   GET /api/market/losers      → Top 5 losers today
 *   POST /api/refresh           → Manually trigger a data refresh
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*") // Allow frontend to call this API
public class CryptoController {

    @Autowired
    private CoinRepository coinRepository;

    @Autowired
    private PriceHistoryRepository priceHistoryRepository;

    @Autowired
    private CoinGeckoService coinGeckoService;

    // =====================================================
    //  COINS ENDPOINTS
    // =====================================================

    /**
     * GET /api/coins
     * Returns all tracked coins sorted by market cap rank.
     */
    @GetMapping("/coins")
    public ResponseEntity<List<Coin>> getAllCoins() {
        List<Coin> coins = coinRepository.findAllByOrderByMarketCapRankAsc();
        if (coins.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(coins);
    }

    /**
     * GET /api/coins/{id}
     * Returns details for a specific coin (e.g., "bitcoin", "ethereum").
     */
    @GetMapping("/coins/{id}")
    public ResponseEntity<Coin> getCoinById(@PathVariable String id) {
        Optional<Coin> coin = coinRepository.findById(id);
        return coin.map(ResponseEntity::ok)
                   .orElse(ResponseEntity.notFound().build());
    }

    /**
     * GET /api/coins/{id}/history?days=7
     * Returns price history for a coin over the last N days.
     * Used for rendering the price charts on the dashboard.
     */
    @GetMapping("/coins/{id}/history")
    public ResponseEntity<List<PriceHistory>> getCoinHistory(
            @PathVariable String id,
            @RequestParam(defaultValue = "7") int days) {

        LocalDateTime since = LocalDateTime.now().minusDays(days);
        List<PriceHistory> history = priceHistoryRepository.findByCoinIdSince(id, since);

        if (history.isEmpty()) {
            // Return all available data if no data in range
            history = priceHistoryRepository.findByCoinIdOrderByRecordedAtAsc(id);
        }
        return ResponseEntity.ok(history);
    }

    // =====================================================
    //  MARKET STATS ENDPOINTS
    // =====================================================

    /**
     * GET /api/market/stats
     * Returns global crypto market statistics and summary metrics.
     */
    @GetMapping("/market/stats")
    public ResponseEntity<Map<String, Object>> getMarketStats() {
        Map<String, Object> stats = new HashMap<>();

        List<Coin> allCoins = coinRepository.findAllByOrderByMarketCapRankAsc();

        // Calculate aggregate stats from our stored data
        long totalCoins = allCoins.size();
        double totalMarketCap = allCoins.stream()
            .filter(c -> c.getMarketCap() != null)
            .mapToLong(Coin::getMarketCap)
            .sum();
        double totalVolume = allCoins.stream()
            .filter(c -> c.getTotalVolume() != null)
            .mapToLong(Coin::getTotalVolume)
            .sum();

        // Bitcoin dominance
        Optional<Coin> bitcoin = coinRepository.findById("bitcoin");
        double btcDominance = 0;
        if (bitcoin.isPresent() && bitcoin.get().getMarketCap() != null && totalMarketCap > 0) {
            btcDominance = (bitcoin.get().getMarketCap() / totalMarketCap) * 100;
        }

        // Count gainers & losers
        long gainers = allCoins.stream()
            .filter(c -> c.getPriceChangePercentage24h() != null && c.getPriceChangePercentage24h() > 0)
            .count();
        long losers = allCoins.stream()
            .filter(c -> c.getPriceChangePercentage24h() != null && c.getPriceChangePercentage24h() < 0)
            .count();

        long totalHistoryRecords = priceHistoryRepository.count();

        stats.put("totalTrackedCoins", totalCoins);
        stats.put("totalMarketCap", totalMarketCap);
        stats.put("totalVolume24h", totalVolume);
        stats.put("btcDominance", Math.round(btcDominance * 100.0) / 100.0);
        stats.put("gainers24h", gainers);
        stats.put("losers24h", losers);
        stats.put("totalDataPoints", totalHistoryRecords);
        stats.put("lastUpdated", LocalDateTime.now().toString());

        // Also try to get live global data from CoinGecko
        try {
            CoinGeckoDTO.GlobalDataInner globalData = coinGeckoService.fetchGlobalStats();
            if (globalData != null) {
                stats.put("activeCryptocurrencies", globalData.active_cryptocurrencies);
                if (globalData.market_cap_percentage != null) {
                    stats.put("btcDominanceLive", globalData.market_cap_percentage.get("btc"));
                    stats.put("ethDominanceLive", globalData.market_cap_percentage.get("eth"));
                }
                stats.put("marketCapChange24h", globalData.market_cap_change_percentage_24h_usd);
            }
        } catch (Exception e) {
            // Use cached stats if live call fails
        }

        return ResponseEntity.ok(stats);
    }

    /**
     * GET /api/market/gainers
     * Returns top 5 coins by 24h price gain.
     */
    @GetMapping("/market/gainers")
    public ResponseEntity<List<Coin>> getTopGainers() {
        List<Coin> gainers = coinRepository.findTopGainers();
        return ResponseEntity.ok(gainers.stream().limit(5).toList());
    }

    /**
     * GET /api/market/losers
     * Returns top 5 coins by 24h price loss.
     */
    @GetMapping("/market/losers")
    public ResponseEntity<List<Coin>> getTopLosers() {
        List<Coin> losers = coinRepository.findTopLosers();
        return ResponseEntity.ok(losers.stream().limit(5).toList());
    }

    // =====================================================
    //  UTILITY ENDPOINTS
    // =====================================================

    /**
     * POST /api/refresh
     * Manually triggers the data pipeline to refresh all coin data.
     * Useful for testing without waiting for the scheduler.
     */
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, String>> refreshData() {
        coinGeckoService.fetchAndStoreMarketData();
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Data pipeline executed successfully!");
        response.put("timestamp", LocalDateTime.now().toString());
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/health
     * Simple health check endpoint.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> status = new HashMap<>();
        status.put("status", "UP");
        status.put("service", "CryptoTrack Analytics API");
        status.put("timestamp", LocalDateTime.now().toString());
        long coinCount = coinRepository.count();
        long historyCount = priceHistoryRepository.count();
        status.put("coinsTracked", String.valueOf(coinCount));
        status.put("dataPoints", String.valueOf(historyCount));
        return ResponseEntity.ok(status);
    }
}
