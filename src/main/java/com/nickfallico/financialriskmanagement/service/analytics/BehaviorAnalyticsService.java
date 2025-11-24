package com.nickfallico.financialriskmanagement.service.analytics;

import reactor.core.publisher.Mono;

/**
 * Interface for behavior analytics engine.
 * Implementations can be production (custom ML pipeline) or mock.
 */
public interface BehaviorAnalyticsService {

    /**
     * Update user behavior patterns based on transaction.
     *
     * @param userId User identifier
     * @param transactionData Transaction data as JSON
     * @return Mono completing when patterns are updated
     */
    Mono<Void> updateBehaviorPatterns(String userId, String transactionData);

    /**
     * Analyze high-risk user behavior patterns.
     *
     * @param userId User identifier
     * @param riskScore User's risk score
     * @param riskFactors List of risk factors
     * @return Mono completing when analysis is done
     */
    Mono<Void> analyzeHighRiskUser(String userId, double riskScore, String riskFactors);
}
