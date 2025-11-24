package com.nickfallico.financialriskmanagement.service.analytics;

import reactor.core.publisher.Mono;

/**
 * Interface for user profile cache service.
 * Implementations can be production (Redis, Memcached) or mock.
 */
public interface UserProfileCacheService {

    /**
     * Invalidate user profile cache.
     *
     * @param userId User identifier
     * @return Mono completing when cache is invalidated
     */
    Mono<Void> invalidateUserProfile(String userId);
}
