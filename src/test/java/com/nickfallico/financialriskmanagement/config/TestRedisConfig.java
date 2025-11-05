package com.nickfallico.financialriskmanagement.config;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;

/**
 * Test-only Redis config so @SpringBootTest can start without a real Redis instance.
 * Lives in src/test so it never ships with prod code.
 */
@Configuration
public class TestRedisConfig {

    @Bean
    @Primary
    public RedisConnectionFactory redisConnectionFactory() {
        // We don't actually call Redis in tests, we just need something
        // that satisfies CacheConfig.cacheManager(...)
        return Mockito.mock(RedisConnectionFactory.class);
    }
}
