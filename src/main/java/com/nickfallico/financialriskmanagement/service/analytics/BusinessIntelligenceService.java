package com.nickfallico.financialriskmanagement.service.analytics;

import reactor.core.publisher.Mono;

/**
 * Interface for business intelligence integration.
 * Implementations can be production (Tableau, Looker, PowerBI) or mock.
 */
public interface BusinessIntelligenceService {

    /**
     * Feed user profile update to BI system.
     *
     * @param userId User identifier
     * @param profileData Profile data as JSON
     * @return Mono completing when data is sent to BI
     */
    Mono<Void> feedProfileUpdate(String userId, String profileData);

    /**
     * Update dashboard with risk metrics.
     *
     * @param userId User identifier
     * @param riskScore Current risk score
     * @return Mono completing when dashboard is updated
     */
    Mono<Void> updateDashboardMetrics(String userId, double riskScore);
}
