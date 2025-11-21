package com.nickfallico.financialriskmanagement.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisClusterConnection;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisSentinelConnection;

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
        // that satisfies CacheConfig.cacheManager(...) without needing Mockito inline agents
        return new NoOpRedisConnectionFactory();
    }

    /**
     * Minimal RedisConnectionFactory that never connects. All methods return null or safe defaults.
     */
    static class NoOpRedisConnectionFactory implements RedisConnectionFactory {
        @Override
        public DataAccessException translateExceptionIfPossible(RuntimeException ex) {
            return null;
        }

        @Override
        public boolean getConvertPipelineAndTxResults() {
            return false;
        }

        @Override
        public RedisConnection getConnection() {
            return null;
        }

        @Override
        public RedisClusterConnection getClusterConnection() {
            return null;
        }

        @Override
        public RedisSentinelConnection getSentinelConnection() {
            return null;
        }
    }
}
