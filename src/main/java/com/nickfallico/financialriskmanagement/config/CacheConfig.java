package com.nickfallico.financialriskmanagement.config;

import java.time.Duration;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
 
@Configuration
@EnableCaching
@Profile("!test") // don't load this in tests
public class CacheConfig {
 
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
        // Use JSON serializer that preserves type information
        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer();
 
        RedisCacheConfiguration userProfileCacheConfig = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(30))
            .disableCachingNullValues()
            .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer));
 
        RedisCacheConfiguration transactionCacheConfig = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(15))
            .disableCachingNullValues()
            .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer));
 
        return RedisCacheManager.builder(redisConnectionFactory)
            .withCacheConfiguration("userProfiles", userProfileCacheConfig)
            .withCacheConfiguration("transactions", transactionCacheConfig)
            .withCacheConfiguration("merchantFrequencies", userProfileCacheConfig)
            .build();
    }
}