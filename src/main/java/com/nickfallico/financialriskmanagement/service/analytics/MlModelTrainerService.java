package com.nickfallico.financialriskmanagement.service.analytics;

import reactor.core.publisher.Mono;

/**
 * Interface for ML model training service.
 * Implementations can be production (AWS SageMaker, MLflow) or mock.
 */
public interface MlModelTrainerService {

    /**
     * Update high-risk user patterns for model retraining.
     *
     * @param userId User identifier
     * @param patterns Pattern data as JSON
     * @return Mono completing when patterns are updated
     */
    Mono<Void> updateHighRiskPatterns(String userId, String patterns);

    /**
     * Check if model retraining should be triggered based on profile changes.
     *
     * @param userId User identifier
     * @param previousRiskScore Previous risk score
     * @param newRiskScore New risk score
     * @return Mono<Boolean> - true if retraining should be triggered
     */
    Mono<Boolean> checkForRetrainingTrigger(String userId, double previousRiskScore, double newRiskScore);
}
