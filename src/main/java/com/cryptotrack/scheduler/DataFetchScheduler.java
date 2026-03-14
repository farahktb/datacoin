package com.cryptotrack.scheduler;

import com.cryptotrack.service.CoinGeckoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;

/**
 * DataFetchScheduler - Triggers the ETL pipeline on a schedule.
 *
 * This component ensures the database always has fresh data by:
 * 1. Running once immediately on startup (@PostConstruct)
 * 2. Then running every 5 minutes automatically (@Scheduled)
 *
 * The 5-minute interval respects CoinGecko's free tier rate limits.
 */
@Component
public class DataFetchScheduler {

    @Autowired
    private CoinGeckoService coinGeckoService;

    /**
     * Runs once when the application starts up.
     * This ensures data is available immediately when you open the dashboard.
     */
    @PostConstruct
    public void runOnStartup() {
        System.out.println("[Scheduler] Application started. Running initial data fetch...");
        coinGeckoService.fetchAndStoreMarketData();
        System.out.println("[Scheduler] Initial fetch complete at " + LocalDateTime.now());
    }

    /**
     * Runs every 5 minutes to keep data fresh.
     * fixedDelay = 300,000 milliseconds = 5 minutes
     */
    @Scheduled(fixedDelayString = "${scheduler.fetch-interval}")
    public void scheduledFetch() {
        System.out.println("[Scheduler] Scheduled fetch triggered at " + LocalDateTime.now());
        coinGeckoService.fetchAndStoreMarketData();
    }
}
