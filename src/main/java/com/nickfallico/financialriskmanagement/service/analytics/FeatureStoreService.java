package com.nickfallico.financialriskmanagement.service.analytics;

import reactor.core.publisher.Mono;

/**
 * Interface for ML feature store.
 * Implementations can be production (AWS SageMaker Feature Store, Feast) or mock.
 */
public interface FeatureStoreService {

    /**
     * Store transaction features for ML training.
     *
     * @param transactionId Transaction identifier
     * @param userId User identifier
     * @param features Feature data as JSON
     * @return Mono completing when features are stored
     */
    Mono<Void> storeTransactionFeatures(String transactionId, String userId, String features);

    /**
     * Update user features in the feature store.
     *
     * @param userId User identifier
     * @param features Updated feature data as JSON
     * @return Mono completing when features are updated
     */
    Mono<Void> updateUserFeatures(String userId, String features);
}
