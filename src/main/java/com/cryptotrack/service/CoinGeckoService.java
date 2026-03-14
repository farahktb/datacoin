package com.cryptotrack.service;

import com.cryptotrack.dto.CoinGeckoDTO;
import com.cryptotrack.model.Coin;
import com.cryptotrack.model.PriceHistory;
import com.cryptotrack.repository.CoinRepository;
import com.cryptotrack.repository.PriceHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;

/**
 * CoinGeckoService - The core data pipeline service.
 *
 * This is where the "data engineering" magic happens:
 * 1. EXTRACT  → Fetches raw data from CoinGecko API
 * 2. TRANSFORM → Maps API response to our domain models
 * 3. LOAD      → Saves cleaned data to the database
 *
 * This ETL pattern is fundamental in data engineering!
 */
@Service
public class CoinGeckoService {

    @Value("${coingecko.api.base-url}")
    private String baseUrl;

    @Autowired
    private CoinRepository coinRepository;

    @Autowired
    private PriceHistoryRepository priceHistoryRepository;

    private final RestTemplate restTemplate;

    // Top 20 coins to track
    private static final String COINS_ENDPOINT =
        "/coins/markets?vs_currency=usd" +
        "&order=market_cap_desc" +
        "&per_page=20" +
        "&page=1" +
        "&sparkline=true" +
        "&price_change_percentage=1h,24h,7d";

    private static final String GLOBAL_ENDPOINT = "/global";

    public CoinGeckoService() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * MAIN ETL METHOD
     * Fetches current market data for top 20 coins, transforms it,
     * and saves both current prices and a price history snapshot.
     */
    public void fetchAndStoreMarketData() {
        try {
            System.out.println("[CoinGecko] Starting data fetch at " + LocalDateTime.now());

            // ---- EXTRACT: Call the API ----
            String url = baseUrl + COINS_ENDPOINT;
            HttpHeaders headers = new HttpHeaders();
            headers.set("Accept", "application/json");
            headers.set("User-Agent", "CryptoTrack-Analytics/1.0");
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<List<CoinGeckoDTO.MarketCoin>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<List<CoinGeckoDTO.MarketCoin>>() {}
            );

            if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
                System.err.println("[CoinGecko] Failed to fetch data: " + response.getStatusCode());
                return;
            }

            List<CoinGeckoDTO.MarketCoin> marketCoins = response.getBody();
            System.out.println("[CoinGecko] Fetched " + marketCoins.size() + " coins.");

            // ---- TRANSFORM + LOAD ----
            for (CoinGeckoDTO.MarketCoin apiCoin : marketCoins) {
                // Transform: Map API DTO -> Coin Entity
                Coin coin = mapToCoinEntity(apiCoin);
                coinRepository.save(coin); // Upsert (insert or update)

                // Also save a price history snapshot (time-series data)
                PriceHistory history = new PriceHistory(
                    coin.getId(),
                    coin.getName(),
                    coin.getCurrentPrice(),
                    coin.getMarketCap(),
                    coin.getTotalVolume()
                );
                priceHistoryRepository.save(history);
            }

            System.out.println("[CoinGecko] ✓ Data pipeline complete. " +
                marketCoins.size() + " coins updated.");

        } catch (Exception e) {
            System.err.println("[CoinGecko] Pipeline error: " + e.getMessage());
        }
    }

    /**
     * Fetches global crypto market statistics.
     */
    public CoinGeckoDTO.GlobalDataInner fetchGlobalStats() {
        try {
            String url = baseUrl + GLOBAL_ENDPOINT;
            HttpHeaders headers = new HttpHeaders();
            headers.set("Accept", "application/json");
            headers.set("User-Agent", "CryptoTrack-Analytics/1.0");
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<CoinGeckoDTO.GlobalData> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                CoinGeckoDTO.GlobalData.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return response.getBody().data;
            }
        } catch (Exception e) {
            System.err.println("[CoinGecko] Global stats error: " + e.getMessage());
        }
        return null;
    }

    /**
     * TRANSFORM: Maps a CoinGecko API response object to our Coin entity.
     * This is the transformation step of the ETL pipeline.
     */
    private Coin mapToCoinEntity(CoinGeckoDTO.MarketCoin apiCoin) {
        Coin coin = new Coin(apiCoin.id, apiCoin.symbol, apiCoin.name);
        coin.setImageUrl(apiCoin.image);
        coin.setCurrentPrice(apiCoin.current_price);
        coin.setMarketCap(apiCoin.market_cap);
        coin.setMarketCapRank(apiCoin.market_cap_rank);
        coin.setTotalVolume(apiCoin.total_volume);
        coin.setPriceChange24h(apiCoin.price_change_24h);
        coin.setPriceChangePercentage24h(apiCoin.price_change_percentage_24h);
        coin.setPriceChangePercentage7d(apiCoin.price_change_percentage_7d_in_currency);
        coin.setPriceChangePercentage1h(apiCoin.price_change_percentage_1h_in_currency);
        coin.setHigh24h(apiCoin.high_24h);
        coin.setLow24h(apiCoin.low_24h);
        coin.setCirculatingSupply(apiCoin.circulating_supply);
        coin.setAth(apiCoin.ath);
        coin.setLastUpdated(LocalDateTime.now());
        return coin;
    }
}
