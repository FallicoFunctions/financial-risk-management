package com.nickfallico.financialriskmanagement.service.analytics;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * Mock feature store for development and demonstration.
 * Logs feature storage instead of actually storing to feature store.
 *
 * In production, replace with AWS SageMaker Feature Store or Feast.
 */
@Service
@Slf4j
@ConditionalOnProperty(name = "analytics.featurestore.provider", havingValue = "mock", matchIfMissing = true)
public class MockFeatureStoreService implements FeatureStoreService {

    private final Counter featuresStoredCounter;
    private final Counter featuresUpdatedCounter;

    public MockFeatureStoreService(MeterRegistry meterRegistry) {
        this.featuresStoredCounter = Counter.builder("analytics.features.stored")
            .description("Total number of features stored (mock)")
            .tag("provider", "mock")
            .register(meterRegistry);

        this.featuresUpdatedCounter = Counter.builder("analytics.features.updated")
            .description("Total number of features updated (mock)")
            .tag("provider", "mock")
            .register(meterRegistry);
    }

    @Override
    public Mono<Void> storeTransactionFeatures(String transactionId, String userId, String features) {
        return Mono.fromRunnable(() -> {
            featuresStoredCounter.increment();

            log.debug("""

                🗄️  [MOCK] Features Stored to Feature Store
                Transaction ID: {}
                User ID: {}
                Features: {}
                [MOCK MODE - In production: Would store to SageMaker/Feast]
                """,
                transactionId,
                userId,
                features.length() > 200 ? features.substring(0, 200) + "..." : features
            );
        });
    }

    @Override
    public Mono<Void> updateUserFeatures(String userId, String features) {
        return Mono.fromRunnable(() -> {
            featuresUpdatedCounter.increment();

            log.debug("""

                🔄 [MOCK] User Features Updated in Feature Store
                User ID: {}
                Features: {}
                [MOCK MODE - In production: Would update in SageMaker/Feast]
                """,
                userId,
                features.length() > 200 ? features.substring(0, 200) + "..." : features
            );
        });
    }
}
