package com.nickfallico.financialriskmanagement.service.analytics;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * Mock business intelligence service for development and demonstration.
 * Simulates BI dashboard updates.
 *
 * In production, replace with Tableau, Looker, or PowerBI integration.
 */
@Service
@Slf4j
@ConditionalOnProperty(name = "analytics.bi.provider", havingValue = "mock", matchIfMissing = true)
public class MockBusinessIntelligenceService implements BusinessIntelligenceService {

    private final Counter profileUpdatesFedCounter;
    private final Counter dashboardUpdatesCounter;

    public MockBusinessIntelligenceService(MeterRegistry meterRegistry) {
        this.profileUpdatesFedCounter = Counter.builder("analytics.bi.profile_updates.fed")
            .description("Total number of profile updates fed to BI (mock)")
            .tag("provider", "mock")
            .register(meterRegistry);

        this.dashboardUpdatesCounter = Counter.builder("analytics.bi.dashboard.updates")
            .description("Total number of dashboard updates (mock)")
            .tag("provider", "mock")
            .register(meterRegistry);
    }

    @Override
    public Mono<Void> feedProfileUpdate(String userId, String profileData) {
        return Mono.fromRunnable(() -> {
            profileUpdatesFedCounter.increment();

            log.debug("""

                📊 [MOCK] Profile Update Fed to BI System
                User ID: {}
                Profile Data: {}
                [MOCK MODE - In production: Would send to Tableau/Looker/PowerBI]
                """,
                userId,
                profileData.length() > 200 ? profileData.substring(0, 200) + "..." : profileData
            );
        });
    }

    @Override
    public Mono<Void> updateDashboardMetrics(String userId, double riskScore) {
        return Mono.fromRunnable(() -> {
            dashboardUpdatesCounter.increment();

            log.debug("""

                📈 [MOCK] Dashboard Metrics Updated
                User ID: {}
                Risk Score: {}
                [MOCK MODE - In production: Would update real-time dashboard]
                """,
                userId,
                riskScore
            );
        });
    }
}
