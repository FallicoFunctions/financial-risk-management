package com.nickfallico.financialriskmanagement.service.analytics;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * Mock user profile cache for development and demonstration.
 * Simulates cache invalidation.
 *
 * In production, replace with Redis or Memcached integration.
 */
@Service
@Slf4j
@ConditionalOnProperty(name = "analytics.cache.provider", havingValue = "mock", matchIfMissing = true)
public class MockUserProfileCacheService implements UserProfileCacheService {

    private final Counter cacheInvalidationsCounter;

    public MockUserProfileCacheService(MeterRegistry meterRegistry) {
        this.cacheInvalidationsCounter = Counter.builder("analytics.cache.invalidations")
            .description("Total number of cache invalidations (mock)")
            .tag("provider", "mock")
            .register(meterRegistry);
    }

    @Override
    public Mono<Void> invalidateUserProfile(String userId) {
        return Mono.fromRunnable(() -> {
            cacheInvalidationsCounter.increment();

            log.debug("""

                🗑️  [MOCK] User Profile Cache Invalidated
                User ID: {}
                [MOCK MODE - In production: Would invalidate Redis/Memcached]
                """,
                userId
            );
        });
    }
}
