package com.nickfallico.financialriskmanagement.service.analytics;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * Mock behavior analytics for development and demonstration.
 * Logs behavior pattern updates instead of actual ML processing.
 *
 * In production, replace with custom ML behavior analytics engine.
 */
@Service
@Slf4j
@ConditionalOnProperty(name = "analytics.behavior.provider", havingValue = "mock", matchIfMissing = true)
public class MockBehaviorAnalyticsService implements BehaviorAnalyticsService {

    private final Counter patternsUpdatedCounter;
    private final Counter highRiskAnalyzedCounter;

    public MockBehaviorAnalyticsService(MeterRegistry meterRegistry) {
        this.patternsUpdatedCounter = Counter.builder("analytics.behavior.patterns.updated")
            .description("Total number of behavior patterns updated (mock)")
            .tag("provider", "mock")
            .register(meterRegistry);

        this.highRiskAnalyzedCounter = Counter.builder("analytics.behavior.high_risk_analyzed")
            .description("Total number of high-risk users analyzed (mock)")
            .tag("provider", "mock")
            .register(meterRegistry);
    }

    @Override
    public Mono<Void> updateBehaviorPatterns(String userId, String transactionData) {
        return Mono.fromRunnable(() -> {
            patternsUpdatedCounter.increment();

            log.debug("""

                🧠 [MOCK] Behavior Patterns Updated
                User ID: {}
                Transaction Data: {}
                [MOCK MODE - In production: Would analyze with ML engine]
                """,
                userId,
                transactionData.length() > 150 ? transactionData.substring(0, 150) + "..." : transactionData
            );
        });
    }

    @Override
    public Mono<Void> analyzeHighRiskUser(String userId, double riskScore, String riskFactors) {
        return Mono.fromRunnable(() -> {
            highRiskAnalyzedCounter.increment();

            log.debug("""

                🔍 [MOCK] High-Risk User Behavior Analyzed
                User ID: {}
                Risk Score: {}
                Risk Factors: {}
                [MOCK MODE - In production: Would perform deep behavior analysis]
                """,
                userId,
                riskScore,
                riskFactors
            );
        });
    }
}
