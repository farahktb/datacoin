package com.cryptotrack.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Coin entity - represents a cryptocurrency with its current market data.
 * Data is fetched from CoinGecko API and stored in the database.
 */
@Entity
@Table(name = "coins")
public class Coin {

    @Id
    @Column(nullable = false, unique = true)
    private String id; // CoinGecko coin ID (e.g., "bitcoin")

    @Column(nullable = false)
    private String symbol; // e.g., "btc"

    @Column(nullable = false)
    private String name; // e.g., "Bitcoin"

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "current_price")
    private Double currentPrice;

    @Column(name = "market_cap")
    private Long marketCap;

    @Column(name = "market_cap_rank")
    private Integer marketCapRank;

    @Column(name = "total_volume")
    private Long totalVolume;

    @Column(name = "price_change_24h")
    private Double priceChange24h;

    @Column(name = "price_change_percentage_24h")
    private Double priceChangePercentage24h;

    @Column(name = "price_change_percentage_7d")
    private Double priceChangePercentage7d;

    @Column(name = "price_change_percentage_1h")
    private Double priceChangePercentage1h;

    @Column(name = "high_24h")
    private Double high24h;

    @Column(name = "low_24h")
    private Double low24h;

    @Column(name = "circulating_supply")
    private Double circulatingSupply;

    @Column(name = "ath")
    private Double ath; // All-time high

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;

    // ---- Constructors ----

    public Coin() {}

    public Coin(String id, String symbol, String name) {
        this.id = id;
        this.symbol = symbol;
        this.name = name;
    }

    // ---- Getters & Setters ----

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public Double getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(Double currentPrice) { this.currentPrice = currentPrice; }

    public Long getMarketCap() { return marketCap; }
    public void setMarketCap(Long marketCap) { this.marketCap = marketCap; }

    public Integer getMarketCapRank() { return marketCapRank; }
    public void setMarketCapRank(Integer marketCapRank) { this.marketCapRank = marketCapRank; }

    public Long getTotalVolume() { return totalVolume; }
    public void setTotalVolume(Long totalVolume) { this.totalVolume = totalVolume; }

    public Double getPriceChange24h() { return priceChange24h; }
    public void setPriceChange24h(Double priceChange24h) { this.priceChange24h = priceChange24h; }

    public Double getPriceChangePercentage24h() { return priceChangePercentage24h; }
    public void setPriceChangePercentage24h(Double priceChangePercentage24h) { this.priceChangePercentage24h = priceChangePercentage24h; }

    public Double getPriceChangePercentage7d() { return priceChangePercentage7d; }
    public void setPriceChangePercentage7d(Double priceChangePercentage7d) { this.priceChangePercentage7d = priceChangePercentage7d; }

    public Double getPriceChangePercentage1h() { return priceChangePercentage1h; }
    public void setPriceChangePercentage1h(Double priceChangePercentage1h) { this.priceChangePercentage1h = priceChangePercentage1h; }

    public Double getHigh24h() { return high24h; }
    public void setHigh24h(Double high24h) { this.high24h = high24h; }

    public Double getLow24h() { return low24h; }
    public void setLow24h(Double low24h) { this.low24h = low24h; }

    public Double getCirculatingSupply() { return circulatingSupply; }
    public void setCirculatingSupply(Double circulatingSupply) { this.circulatingSupply = circulatingSupply; }

    public Double getAth() { return ath; }
    public void setAth(Double ath) { this.ath = ath; }

    public LocalDateTime getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }
}
