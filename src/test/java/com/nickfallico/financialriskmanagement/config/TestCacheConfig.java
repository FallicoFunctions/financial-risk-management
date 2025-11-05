package com.nickfallico.financialriskmanagement.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.*;

@TestConfiguration
@Profile("test")
public class TestCacheConfig {

    @Bean
    @Primary
    public CacheManager cacheManager() {
        // In-memory, no Redis
        return new ConcurrentMapCacheManager("transactions", "riskScores");
    }
}
