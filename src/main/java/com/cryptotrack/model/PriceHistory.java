package com.cryptotrack.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * PriceHistory entity - stores historical price snapshots for each coin.
 * Every time the scheduler runs, new price records are saved here.
 * This builds up a time-series dataset for charting price trends.
 */
@Entity
@Table(name = "price_history", indexes = {
    @Index(name = "idx_coin_timestamp", columnList = "coin_id, recorded_at")
})
public class PriceHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "coin_id", nullable = false)
    private String coinId;

    @Column(name = "coin_name", nullable = false)
    private String coinName;

    @Column(name = "price", nullable = false)
    private Double price;

    @Column(name = "market_cap")
    private Long marketCap;

    @Column(name = "volume")
    private Long volume;

    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt;

    // ---- Constructors ----

    public PriceHistory() {}

    public PriceHistory(String coinId, String coinName, Double price, Long marketCap, Long volume) {
        this.coinId = coinId;
        this.coinName = coinName;
        this.price = price;
        this.marketCap = marketCap;
        this.volume = volume;
        this.recordedAt = LocalDateTime.now();
    }

    // ---- Getters & Setters ----

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCoinId() { return coinId; }
    public void setCoinId(String coinId) { this.coinId = coinId; }

    public String getCoinName() { return coinName; }
    public void setCoinName(String coinName) { this.coinName = coinName; }

    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }

    public Long getMarketCap() { return marketCap; }
    public void setMarketCap(Long marketCap) { this.marketCap = marketCap; }

    public Long getVolume() { return volume; }
    public void setVolume(Long volume) { this.volume = volume; }

    public LocalDateTime getRecordedAt() { return recordedAt; }
    public void setRecordedAt(LocalDateTime recordedAt) { this.recordedAt = recordedAt; }
}
