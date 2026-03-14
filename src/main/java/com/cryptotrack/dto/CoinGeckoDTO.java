package com.cryptotrack.dto;

import java.util.List;
import java.util.Map;

/**
 * Data Transfer Objects (DTOs) for CoinGecko API responses.
 * These are used to parse the JSON data returned by CoinGecko.
 * DTOs are separate from entity classes to keep concerns separated.
 */
public class CoinGeckoDTO {

    /**
     * Represents one coin from the /coins/markets endpoint.
     * Field names match the JSON keys returned by CoinGecko API.
     */
    public static class MarketCoin {
        public String id;
        public String symbol;
        public String name;
        public String image;
        public Double current_price;
        public Long market_cap;
        public Integer market_cap_rank;
        public Long total_volume;
        public Double price_change_24h;
        public Double price_change_percentage_24h;
        public Double price_change_percentage_7d_in_currency;
        public Double price_change_percentage_1h_in_currency;
        public Double high_24h;
        public Double low_24h;
        public Double circulating_supply;
        public Double ath;
        public String last_updated;

        // Sparkline data (7d price array for mini-charts)
        public SparklineData sparkline_in_7d;
    }

    /**
     * Sparkline data wrapper.
     */
    public static class SparklineData {
        public List<Double> price;
    }

    /**
     * Global market data response.
     */
    public static class GlobalData {
        public GlobalDataInner data;
    }

    public static class GlobalDataInner {
        public Integer active_cryptocurrencies;
        public Double total_market_cap_usd;
        public Double total_volume_usd;
        public Double market_cap_percentage_btc;
        public Double market_cap_change_percentage_24h_usd;
        public Map<String, Double> total_market_cap;
        public Map<String, Double> total_volume;
        public Map<String, Double> market_cap_percentage;
    }
}
