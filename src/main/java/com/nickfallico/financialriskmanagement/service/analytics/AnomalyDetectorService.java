package com.nickfallico.financialriskmanagement.service.analytics;

import reactor.core.publisher.Mono;

/**
 * Interface for anomaly detection service.
 * Implementations can be production (ML-based anomaly detector) or mock.
 */
public interface AnomalyDetectorService {

    /**
     * Check for anomalies in transaction.
     *
     * @param transactionId Transaction identifier
     * @param userId User identifier
     * @param transactionData Transaction data as JSON
     * @return Mono<Boolean> - true if anomaly detected
     */
    Mono<Boolean> checkForAnomalies(String transactionId, String userId, String transactionData);

    /**
     * Update risk thresholds based on new high-risk user data.
     *
     * @param userId User identifier
     * @param riskScore User's risk score
     * @return Mono completing when thresholds are updated
     */
    Mono<Void> updateRiskThresholds(String userId, double riskScore);
}
