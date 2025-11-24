package com.nickfallico.financialriskmanagement.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.nickfallico.financialriskmanagement.kafka.event.HighRiskUserIdentifiedEvent;
import com.nickfallico.financialriskmanagement.kafka.event.TransactionCreatedEvent;
import com.nickfallico.financialriskmanagement.kafka.event.UserProfileUpdatedEvent;
import com.nickfallico.financialriskmanagement.service.analytics.AnomalyDetectorService;
import com.nickfallico.financialriskmanagement.service.analytics.BehaviorAnalyticsService;
import com.nickfallico.financialriskmanagement.service.analytics.BusinessIntelligenceService;
import com.nickfallico.financialriskmanagement.service.analytics.FeatureStoreService;
import com.nickfallico.financialriskmanagement.service.analytics.MlModelTrainerService;
import com.nickfallico.financialriskmanagement.service.analytics.UserProfileCacheService;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock
    private FeatureStoreService featureStoreService;

    @Mock
    private BehaviorAnalyticsService behaviorAnalyticsService;

    @Mock
    private AnomalyDetectorService anomalyDetectorService;

    @Mock
    private MlModelTrainerService mlModelTrainerService;

    @Mock
    private UserProfileCacheService userProfileCacheService;

    @Mock
    private BusinessIntelligenceService businessIntelligenceService;

    private MeterRegistry meterRegistry;
    private AnalyticsService analyticsService;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        analyticsService = new AnalyticsService(
            meterRegistry,
            featureStoreService,
            behaviorAnalyticsService,
            anomalyDetectorService,
            mlModelTrainerService,
            userProfileCacheService,
            businessIntelligenceService
        );

        // Default: all services return successful Mono
        when(featureStoreService.storeTransactionFeatures(anyString(), anyString(), anyString()))
            .thenReturn(Mono.empty());
        when(featureStoreService.updateUserFeatures(anyString(), anyString()))
            .thenReturn(Mono.empty());
        when(behaviorAnalyticsService.updateBehaviorPatterns(anyString(), anyString()))
            .thenReturn(Mono.empty());
        when(behaviorAnalyticsService.analyzeHighRiskUser(anyString(), anyDouble(), anyString()))
            .thenReturn(Mono.empty());
        when(anomalyDetectorService.checkForAnomalies(anyString(), anyString(), anyString()))
            .thenReturn(Mono.just(false));
        when(anomalyDetectorService.updateRiskThresholds(anyString(), anyDouble()))
            .thenReturn(Mono.empty());
        when(mlModelTrainerService.updateHighRiskPatterns(anyString(), anyString()))
            .thenReturn(Mono.empty());
        when(mlModelTrainerService.checkForRetrainingTrigger(anyString(), anyDouble(), anyDouble()))
            .thenReturn(Mono.just(false));
        when(userProfileCacheService.invalidateUserProfile(anyString()))
            .thenReturn(Mono.empty());
        when(businessIntelligenceService.updateDashboardMetrics(anyString(), anyDouble()))
            .thenReturn(Mono.empty());
        when(businessIntelligenceService.feedProfileUpdate(anyString(), anyString()))
            .thenReturn(Mono.empty());
    }

    @Test
    void processTransactionAnalytics_CallsAllServices() {
        // Arrange
        TransactionCreatedEvent event = createTransactionEvent();

        // Act & Assert
        StepVerifier.create(analyticsService.processTransactionAnalytics(event))
            .verifyComplete();

        // Verify feature store was called
        verify(featureStoreService).storeTransactionFeatures(
            eq(event.getTransactionId().toString()),
            eq(event.getUserId()),
            anyString()
        );
        verify(featureStoreService).updateUserFeatures(eq(event.getUserId()), anyString());

        // Verify behavior analytics was called
        verify(behaviorAnalyticsService).updateBehaviorPatterns(
            eq(event.getUserId()),
            anyString()
        );

        // Verify anomaly detector was called
        verify(anomalyDetectorService).checkForAnomalies(
            eq(event.getTransactionId().toString()),
            eq(event.getUserId()),
            anyString()
        );

        // Verify metrics were recorded
        assert meterRegistry.counter("analytics.ml_features_extracted").count() == 1.0;
    }

    @Test
    void processTransactionAnalytics_DetectsAnomalies() {
        // Arrange
        TransactionCreatedEvent event = createTransactionEvent();
        when(anomalyDetectorService.checkForAnomalies(anyString(), anyString(), anyString()))
            .thenReturn(Mono.just(true)); // Anomaly detected

        // Act & Assert
        StepVerifier.create(analyticsService.processTransactionAnalytics(event))
            .verifyComplete();

        // Verify anomaly metric was incremented
        assert meterRegistry.counter("analytics.anomalies.detected", "type", "spending_pattern").count() == 1.0;
    }

    @Test
    void processHighRiskUserAnalytics_CallsAllMLServices() {
        // Arrange
        HighRiskUserIdentifiedEvent event = createHighRiskUserEvent();

        // Act & Assert
        StepVerifier.create(analyticsService.processHighRiskUserAnalytics(event))
            .verifyComplete();

        // Verify ML trainer was called
        verify(mlModelTrainerService).updateHighRiskPatterns(eq(event.getUserId()), anyString());

        // Verify behavior analytics was called
        verify(behaviorAnalyticsService).analyzeHighRiskUser(
            eq(event.getUserId()),
            eq(event.getOverallRiskScore()),
            anyString()
        );

        // Verify anomaly detector thresholds updated
        verify(anomalyDetectorService).updateRiskThresholds(
            eq(event.getUserId()),
            eq(event.getOverallRiskScore())
        );

        // Verify metrics were recorded
        assert meterRegistry.counter("analytics.high_risk_users.identified").count() == 1.0;
    }

    @Test
    void processUserProfileUpdateAnalytics_InvalidatesCacheAndUpdatesDashboard() {
        // Arrange
        UserProfileUpdatedEvent event = createUserProfileUpdateEvent(0.4, 0.6);

        // Act & Assert
        StepVerifier.create(analyticsService.processUserProfileUpdateAnalytics(event))
            .verifyComplete();

        // Verify cache was invalidated
        verify(userProfileCacheService).invalidateUserProfile(eq(event.getUserId()));

        // Verify dashboard was updated
        verify(businessIntelligenceService).updateDashboardMetrics(
            eq(event.getUserId()),
            eq(event.getNewOverallRiskScore())
        );

        // Verify BI was fed profile update
        verify(businessIntelligenceService).feedProfileUpdate(eq(event.getUserId()), anyString());

        // Verify metrics
        assert meterRegistry.counter("analytics.user_profile.updates").count() == 1.0;
    }

    @Test
    void processUserProfileUpdateAnalytics_TriggersRetrainingForSignificantChange() {
        // Arrange
        UserProfileUpdatedEvent event = createUserProfileUpdateEvent(0.3, 0.7); // Significant change
        when(mlModelTrainerService.checkForRetrainingTrigger(anyString(), anyDouble(), anyDouble()))
            .thenReturn(Mono.just(true)); // Should retrain

        // Act & Assert
        StepVerifier.create(analyticsService.processUserProfileUpdateAnalytics(event))
            .verifyComplete();

        // Verify retraining check was performed
        verify(mlModelTrainerService).checkForRetrainingTrigger(
            eq(event.getUserId()),
            eq(0.3),
            eq(0.7)
        );
    }

    @Test
    void processUserProfileUpdateAnalytics_TracksSignificantRiskIncrease() {
        // Arrange
        UserProfileUpdatedEvent event = createUserProfileUpdateEvent(0.3, 0.6); // +0.3 change

        // Act
        StepVerifier.create(analyticsService.processUserProfileUpdateAnalytics(event))
            .verifyComplete();

        // Assert - significant increase metric should be incremented
        assert meterRegistry.counter("analytics.user_risk_score.significant_increase").count() == 1.0;
    }

    @Test
    void processUserProfileUpdateAnalytics_TracksSignificantRiskDecrease() {
        // Arrange
        UserProfileUpdatedEvent event = createUserProfileUpdateEvent(0.8, 0.5); // -0.3 change

        // Act
        StepVerifier.create(analyticsService.processUserProfileUpdateAnalytics(event))
            .verifyComplete();

        // Assert - significant decrease metric should be incremented
        assert meterRegistry.counter("analytics.user_risk_score.significant_decrease").count() == 1.0;
    }

    @Test
    void analyticsService_HandlesServiceFailures() {
        // Arrange
        TransactionCreatedEvent event = createTransactionEvent();
        when(featureStoreService.storeTransactionFeatures(anyString(), anyString(), anyString()))
            .thenReturn(Mono.error(new RuntimeException("Feature store unavailable")));

        // Act & Assert - should propagate error
        StepVerifier.create(analyticsService.processTransactionAnalytics(event))
            .expectError(RuntimeException.class)
            .verify();
    }

    // Helper methods to create test events

    private TransactionCreatedEvent createTransactionEvent() {
        return TransactionCreatedEvent.builder()
            .transactionId(UUID.randomUUID())
            .userId("user123")
            .amount(new BigDecimal("500.00"))
            .currency("USD")
            .transactionType("PURCHASE")
            .merchantCategory("RETAIL")
            .merchantName("Test Store")
            .isInternational(false)
            .eventTimestamp(Instant.now())
            .eventId(UUID.randomUUID().toString())
            .eventSource("transaction-service")
            .build();
    }

    private HighRiskUserIdentifiedEvent createHighRiskUserEvent() {
        return HighRiskUserIdentifiedEvent.builder()
            .userId("user456")
            .overallRiskScore(0.82)
            .riskThreshold(0.75)
            .riskFactors(List.of("HIGH_VALUE_TX", "UNUSUAL_PATTERNS"))
            .totalTransactions(50)
            .highRiskTransactions(15)
            .internationalTransactions(10)
            .totalTransactionValue(25000.0)
            .alertSeverity("HIGH")
            .recommendedAction("REVIEW")
            .eventTimestamp(Instant.now())
            .eventId(UUID.randomUUID())
            .eventSource("risk-assessment-service")
            .build();
    }

    private UserProfileUpdatedEvent createUserProfileUpdateEvent(double previousScore, double newScore) {
        return UserProfileUpdatedEvent.builder()
            .userId("user789")
            .previousOverallRiskScore(previousScore)
            .newOverallRiskScore(newScore)
            .totalTransactions(120)
            .totalTransactionValue(45000.0)
            .highRiskTransactions(8)
            .updateReason("FRAUD_DETECTED")
            .triggeringTransactionId(UUID.randomUUID())
            .eventTimestamp(Instant.now())
            .eventId(UUID.randomUUID())
            .eventSource("profile-service")
            .build();
    }
}
