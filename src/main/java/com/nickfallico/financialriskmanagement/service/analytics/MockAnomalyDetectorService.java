package com.nickfallico.financialriskmanagement.service.analytics;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * Mock anomaly detector for development and demonstration.
 * Simulates anomaly detection with simple heuristics.
 *
 * In production, replace with ML-based anomaly detection (Isolation Forest, Autoencoders).
 */
@Service
@Slf4j
@ConditionalOnProperty(name = "analytics.anomaly.provider", havingValue = "mock", matchIfMissing = true)
public class MockAnomalyDetectorService implements AnomalyDetectorService {

    private final Counter anomaliesCheckedCounter;
    private final Counter anomaliesDetectedCounter;
    private final Counter thresholdsUpdatedCounter;

    public MockAnomalyDetectorService(MeterRegistry meterRegistry) {
        this.anomaliesCheckedCounter = Counter.builder("analytics.anomalies.checked")
            .description("Total number of anomaly checks performed (mock)")
            .tag("provider", "mock")
            .register(meterRegistry);

        this.anomaliesDetectedCounter = Counter.builder("analytics.anomalies.detected")
            .description("Total number of anomalies detected (mock)")
            .tag("provider", "mock")
            .register(meterRegistry);

        this.thresholdsUpdatedCounter = Counter.builder("analytics.thresholds.updated")
            .description("Total number of threshold updates (mock)")
            .tag("provider", "mock")
            .register(meterRegistry);
    }

    @Override
    public Mono<Boolean> checkForAnomalies(String transactionId, String userId, String transactionData) {
        return Mono.fromCallable(() -> {
            anomaliesCheckedCounter.increment();

            // Simple mock: Randomly detect anomalies for demo (10% chance)
            boolean isAnomaly = Math.random() < 0.1;

            if (isAnomaly) {
                anomaliesDetectedCounter.increment();
                log.warn("""

                    ⚠️  [MOCK] Anomaly Detected
                    Transaction ID: {}
                    User ID: {}
                    [MOCK MODE - In production: Would use Isolation Forest/Autoencoder]
                    """,
                    transactionId,
                    userId
                );
            } else {
                log.trace("[MOCK] No anomaly detected for transaction: {}", transactionId);
            }

            return isAnomaly;
        });
    }

    @Override
    public Mono<Void> updateRiskThresholds(String userId, double riskScore) {
        return Mono.fromRunnable(() -> {
            thresholdsUpdatedCounter.increment();

            log.debug("""

                📊 [MOCK] Risk Thresholds Updated
                User ID: {}
                Risk Score: {}
                [MOCK MODE - In production: Would adjust ML model thresholds]
                """,
                userId,
                riskScore
            );
        });
    }
}
