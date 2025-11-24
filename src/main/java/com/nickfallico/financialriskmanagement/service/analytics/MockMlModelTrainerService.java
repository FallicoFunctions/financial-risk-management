package com.nickfallico.financialriskmanagement.service.analytics;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * Mock ML model trainer for development and demonstration.
 * Simulates model training triggers and pattern updates.
 *
 * In production, replace with AWS SageMaker or MLflow integration.
 */
@Service
@Slf4j
@ConditionalOnProperty(name = "analytics.mltrainer.provider", havingValue = "mock", matchIfMissing = true)
public class MockMlModelTrainerService implements MlModelTrainerService {

    private final Counter patternsUpdatedCounter;
    private final Counter retrainingTriggersCounter;

    public MockMlModelTrainerService(MeterRegistry meterRegistry) {
        this.patternsUpdatedCounter = Counter.builder("analytics.ml.patterns.updated")
            .description("Total number of ML patterns updated (mock)")
            .tag("provider", "mock")
            .register(meterRegistry);

        this.retrainingTriggersCounter = Counter.builder("analytics.ml.retraining.triggers")
            .description("Total number of model retraining triggers (mock)")
            .tag("provider", "mock")
            .register(meterRegistry);
    }

    @Override
    public Mono<Void> updateHighRiskPatterns(String userId, String patterns) {
        return Mono.fromRunnable(() -> {
            patternsUpdatedCounter.increment();

            log.debug("""

                🤖 [MOCK] High-Risk Patterns Updated for ML Training
                User ID: {}
                Patterns: {}
                [MOCK MODE - In production: Would feed to SageMaker/MLflow]
                """,
                userId,
                patterns.length() > 150 ? patterns.substring(0, 150) + "..." : patterns
            );
        });
    }

    @Override
    public Mono<Boolean> checkForRetrainingTrigger(String userId, double previousRiskScore, double newRiskScore) {
        return Mono.fromCallable(() -> {
            // Trigger retraining if risk score changed significantly (>0.3)
            double riskChange = Math.abs(newRiskScore - previousRiskScore);
            boolean shouldRetrain = riskChange > 0.3;

            if (shouldRetrain) {
                retrainingTriggersCounter.increment();
                log.warn("""

                    🔄 [MOCK] Model Retraining Triggered
                    User ID: {}
                    Previous Risk Score: {}
                    New Risk Score: {}
                    Risk Change: {}
                    [MOCK MODE - In production: Would trigger batch retraining job]
                    """,
                    userId,
                    previousRiskScore,
                    newRiskScore,
                    riskChange
                );
            } else {
                log.trace("[MOCK] No retraining needed for user: {}", userId);
            }

            return shouldRetrain;
        });
    }
}
