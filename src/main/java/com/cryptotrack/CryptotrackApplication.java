package com.cryptotrack;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * CryptoTrack Analytics - Main Application Entry Point
 * A real-time cryptocurrency market analytics dashboard
 * powered by Java Spring Boot and CoinGecko API.
 */
@SpringBootApplication
@EnableScheduling
public class CryptotrackApplication {

    public static void main(String[] args) {
        SpringApplication.run(CryptotrackApplication.class, args);
        System.out.println("\n====================================");
        System.out.println("  DataCoin Analytics is running!");
        System.out.println("  Dashboard: http://localhost:8080");
        System.out.println("  API:       http://localhost:8080/api");
        System.out.println("====================================\n");
    }
}
