package com.nickfallico.financialriskmanagement.websocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.nickfallico.financialriskmanagement.kafka.event.FraudClearedEvent;
import com.nickfallico.financialriskmanagement.kafka.event.FraudDetectedEvent;
import com.nickfallico.financialriskmanagement.kafka.event.HighRiskUserIdentifiedEvent;
import com.nickfallico.financialriskmanagement.kafka.event.TransactionBlockedEvent;
import com.nickfallico.financialriskmanagement.kafka.event.TransactionCreatedEvent;
import com.nickfallico.financialriskmanagement.kafka.event.UserProfileUpdatedEvent;
import com.nickfallico.financialriskmanagement.websocket.message.DashboardMessage;
import com.nickfallico.financialriskmanagement.websocket.message.FraudAlertMessage;
import com.nickfallico.financialriskmanagement.websocket.message.MetricsSnapshotMessage;
import com.nickfallico.financialriskmanagement.websocket.message.RiskScoreUpdateMessage;
import com.nickfallico.financialriskmanagement.websocket.message.TransactionEventMessage;
import com.nickfallico.financialriskmanagement.websocket.service.DashboardEventPublisher;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import reactor.test.StepVerifier;

/**
 * Unit tests for DashboardEventPublisher.
 * Tests event publishing to WebSocket Sinks and stream accessibility.
 */
@DisplayName("DashboardEventPublisher Tests")
class DashboardEventPublisherTest {

    private DashboardEventPublisher publisher;
    private MeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        publisher = new DashboardEventPublisher(meterRegistry);
    }

    // ========== Fraud Alert Tests ==========

    @Test
    @DisplayName("Should publish FraudDetectedEvent to fraud alert stream")
    void shouldPublishFraudDetectedEvent() {
        // Given
        FraudDetectedEvent event = FraudDetectedEvent.builder()
            .transactionId(UUID.randomUUID())
            .userId("user-123")
            .amount(new BigDecimal("1500.00"))
            .currency("USD")
            .merchantCategory("ELECTRONICS")
            .isInternational(false)
            .fraudProbability(0.85)
            .violatedRules(List.of("RULE_001", "RULE_003"))
            .riskLevel("HIGH")
            .action("BLOCK")
            .eventTimestamp(Instant.now())
            .build();

        // When & Then
        StepVerifier.create(publisher.getFraudAlertStream().take(1))
            .then(() -> publisher.publishFraudDetected(event))
            .assertNext(message -> {
                assertThat(message.getMessageType()).isEqualTo(DashboardMessage.MessageType.FRAUD_DETECTED);
                assertThat(message.getUserId()).isEqualTo("user-123");
                assertThat(message.getAmount()).isEqualByComparingTo(new BigDecimal("1500.00"));
                assertThat(message.getFraudProbability()).isEqualTo(0.85);
                assertThat(message.getRiskLevel()).isEqualTo("HIGH");
                assertThat(message.getAction()).isEqualTo("BLOCK");
                assertThat(message.getViolatedRules()).containsExactly("RULE_001", "RULE_003");
                assertThat(message.getPublishedAt()).isNotNull();
            })
            .verifyComplete();
    }

    @Test
    @DisplayName("Should publish FraudClearedEvent to fraud alert stream")
    void shouldPublishFraudClearedEvent() {
        // Given
        FraudClearedEvent event = FraudClearedEvent.builder()
            .transactionId(UUID.randomUUID())
            .userId("user-456")
            .amount(new BigDecimal("250.00"))
            .currency("EUR")
            .merchantCategory("GROCERY")
            .fraudProbability(0.15)
            .riskLevel("LOW")
            .checksPerformed(5)
            .eventTimestamp(Instant.now())
            .build();

        // When & Then
        StepVerifier.create(publisher.getFraudAlertStream().take(1))
            .then(() -> publisher.publishFraudCleared(event))
            .assertNext(message -> {
                assertThat(message.getMessageType()).isEqualTo(DashboardMessage.MessageType.FRAUD_CLEARED);
                assertThat(message.getUserId()).isEqualTo("user-456");
                assertThat(message.getFraudProbability()).isEqualTo(0.15);
                assertThat(message.getChecksPerformed()).isEqualTo(5);
            })
            .verifyComplete();
    }

    @Test
    @DisplayName("Should publish TransactionBlockedEvent to fraud alert stream")
    void shouldPublishTransactionBlockedEvent() {
        // Given
        TransactionBlockedEvent event = TransactionBlockedEvent.builder()
            .transactionId(UUID.randomUUID())
            .userId("user-789")
            .amount(new BigDecimal("10000.00"))
            .currency("USD")
            .merchantCategory("CRYPTO")
            .blockReason("High-value transaction to known risky merchant")
            .violatedRules(List.of("RULE_HIGH_VALUE", "RULE_CRYPTO"))
            .fraudProbability(0.95)
            .severity("CRITICAL")
            .eventTimestamp(Instant.now())
            .build();

        // When & Then
        StepVerifier.create(publisher.getFraudAlertStream().take(1))
            .then(() -> publisher.publishTransactionBlocked(event))
            .assertNext(message -> {
                assertThat(message.getMessageType()).isEqualTo(DashboardMessage.MessageType.TRANSACTION_BLOCKED);
                assertThat(message.getUserId()).isEqualTo("user-789");
                assertThat(message.getBlockReason()).isEqualTo("High-value transaction to known risky merchant");
                assertThat(message.getSeverity()).isEqualTo("CRITICAL");
            })
            .verifyComplete();
    }

    // ========== Transaction Event Tests ==========

    @Test
    @DisplayName("Should publish TransactionCreatedEvent to transaction stream")
    void shouldPublishTransactionCreatedEvent() {
        // Given
        TransactionCreatedEvent event = TransactionCreatedEvent.builder()
            .transactionId(UUID.randomUUID())
            .userId("user-100")
            .amount(new BigDecimal("75.50"))
            .currency("USD")
            .transactionType("PURCHASE")
            .merchantCategory("RESTAURANT")
            .merchantName("Pizzeria Italia")
            .isInternational(false)
            .country("US")
            .city("New York")
            .eventTimestamp(Instant.now())
            .build();

        // When & Then
        StepVerifier.create(publisher.getTransactionEventStream().take(1))
            .then(() -> publisher.publishTransactionCreated(event))
            .assertNext(message -> {
                assertThat(message.getMessageType()).isEqualTo(DashboardMessage.MessageType.TRANSACTION_CREATED);
                assertThat(message.getUserId()).isEqualTo("user-100");
                assertThat(message.getAmount()).isEqualByComparingTo(new BigDecimal("75.50"));
                assertThat(message.getTransactionType()).isEqualTo("PURCHASE");
                assertThat(message.getMerchantName()).isEqualTo("Pizzeria Italia");
                assertThat(message.getCountry()).isEqualTo("US");
            })
            .verifyComplete();
    }

    // ========== Risk Score Update Tests ==========

    @Test
    @DisplayName("Should publish HighRiskUserIdentifiedEvent to risk score stream")
    void shouldPublishHighRiskUserIdentifiedEvent() {
        // Given
        HighRiskUserIdentifiedEvent event = HighRiskUserIdentifiedEvent.builder()
            .userId("user-danger")
            .overallRiskScore(0.92)
            .riskThreshold(0.75)
            .riskFactors(List.of("Multiple blocked transactions", "Unusual geographic pattern"))
            .totalTransactions(50)
            .highRiskTransactions(15)
            .internationalTransactions(25)
            .totalTransactionValue(150000.0)
            .alertSeverity("CRITICAL")
            .recommendedAction("SUSPEND")
            .eventTimestamp(Instant.now())
            .build();

        // When & Then
        StepVerifier.create(publisher.getRiskScoreUpdateStream().take(1))
            .then(() -> publisher.publishHighRiskUserIdentified(event))
            .assertNext(message -> {
                assertThat(message.getMessageType()).isEqualTo(DashboardMessage.MessageType.HIGH_RISK_USER_IDENTIFIED);
                assertThat(message.getUserId()).isEqualTo("user-danger");
                assertThat(message.getOverallRiskScore()).isEqualTo(0.92);
                assertThat(message.getRiskThreshold()).isEqualTo(0.75);
                assertThat(message.getAlertSeverity()).isEqualTo("CRITICAL");
                assertThat(message.getRiskFactors()).hasSize(2);
            })
            .verifyComplete();
    }

    @Test
    @DisplayName("Should publish UserProfileUpdatedEvent to risk score stream")
    void shouldPublishUserProfileUpdatedEvent() {
        // Given
        UserProfileUpdatedEvent event = UserProfileUpdatedEvent.builder()
            .userId("user-updated")
            .previousOverallRiskScore(0.3)
            .newOverallRiskScore(0.55)
            .totalTransactions(100)
            .totalTransactionValue(50000.0)
            .highRiskTransactions(5)
            .updateReason("FRAUD_DETECTED")
            .triggeringTransactionId(UUID.randomUUID())
            .eventTimestamp(Instant.now())
            .build();

        // When & Then
        StepVerifier.create(publisher.getRiskScoreUpdateStream().take(1))
            .then(() -> publisher.publishUserProfileUpdated(event))
            .assertNext(message -> {
                assertThat(message.getMessageType()).isEqualTo(DashboardMessage.MessageType.USER_PROFILE_UPDATED);
                assertThat(message.getUserId()).isEqualTo("user-updated");
                assertThat(message.getPreviousRiskScore()).isEqualTo(0.3);
                assertThat(message.getNewRiskScore()).isEqualTo(0.55);
                assertThat(message.getUpdateReason()).isEqualTo("FRAUD_DETECTED");
            })
            .verifyComplete();
    }

    // ========== Metrics Tests ==========

    @Test
    @DisplayName("Should publish MetricsSnapshotMessage to metrics stream")
    void shouldPublishMetricsSnapshot() {
        // Given
        MetricsSnapshotMessage metricsSnapshot = MetricsSnapshotMessage.builder()
            .messageType(DashboardMessage.MessageType.METRICS_SNAPSHOT)
            .totalTransactionsProcessed(10000L)
            .transactionsLastHour(500L)
            .totalFraudDetected(150L)
            .transactionsBlocked(75L)
            .fraudDetectionRate(1.5)
            .averageRiskScore(0.35)
            .activeWebSocketConnections(10)
            .eventTimestamp(Instant.now())
            .publishedAt(Instant.now())
            .build();

        // When & Then
        StepVerifier.create(publisher.getMetricsStream().take(1))
            .then(() -> publisher.publishMetricsSnapshot(metricsSnapshot))
            .assertNext(message -> {
                assertThat(message.getMessageType()).isEqualTo(DashboardMessage.MessageType.METRICS_SNAPSHOT);
                assertThat(message.getTotalTransactionsProcessed()).isEqualTo(10000L);
                assertThat(message.getFraudDetectionRate()).isEqualTo(1.5);
                assertThat(message.getActiveWebSocketConnections()).isEqualTo(10);
            })
            .verifyComplete();
    }

    // ========== Multicast Tests ==========

    @Test
    @DisplayName("Should multicast events to multiple subscribers")
    void shouldMulticastEventsToMultipleSubscribers() {
        // Given
        FraudDetectedEvent event = FraudDetectedEvent.builder()
            .transactionId(UUID.randomUUID())
            .userId("multicast-user")
            .amount(new BigDecimal("500.00"))
            .currency("USD")
            .fraudProbability(0.75)
            .riskLevel("HIGH")
            .action("REVIEW")
            .eventTimestamp(Instant.now())
            .build();

        // Create two subscribers
        var subscriber1 = publisher.getFraudAlertStream().take(1);
        var subscriber2 = publisher.getFraudAlertStream().take(1);

        // When & Then - both subscribers should receive the event
        StepVerifier.create(subscriber1.zipWith(subscriber2))
            .then(() -> publisher.publishFraudDetected(event))
            .assertNext(tuple -> {
                assertThat(tuple.getT1().getUserId()).isEqualTo("multicast-user");
                assertThat(tuple.getT2().getUserId()).isEqualTo("multicast-user");
            })
            .verifyComplete();
    }

    // ========== Metrics Counter Tests ==========

    @Test
    @DisplayName("Should increment metrics counter when publishing fraud alerts")
    void shouldIncrementMetricsCounterForFraudAlerts() {
        // Given
        FraudDetectedEvent event = FraudDetectedEvent.builder()
            .transactionId(UUID.randomUUID())
            .userId("metrics-test")
            .amount(new BigDecimal("100.00"))
            .currency("USD")
            .fraudProbability(0.8)
            .riskLevel("HIGH")
            .action("BLOCK")
            .eventTimestamp(Instant.now())
            .build();

        // Consume the stream to allow publishing
        publisher.getFraudAlertStream()
            .take(1)
            .subscribe();

        // When
        publisher.publishFraudDetected(event);

        // Then - verify counter was incremented
        var counter = meterRegistry.find("websocket.events.published")
            .tag("type", "fraud_alert")
            .counter();

        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Should increment metrics counter when publishing transaction events")
    void shouldIncrementMetricsCounterForTransactionEvents() {
        // Given
        TransactionCreatedEvent event = TransactionCreatedEvent.builder()
            .transactionId(UUID.randomUUID())
            .userId("metrics-tx-test")
            .amount(new BigDecimal("50.00"))
            .currency("USD")
            .transactionType("PURCHASE")
            .eventTimestamp(Instant.now())
            .build();

        // Consume the stream to allow publishing
        publisher.getTransactionEventStream()
            .take(1)
            .subscribe();

        // When
        publisher.publishTransactionCreated(event);

        // Then - verify counter was incremented
        var counter = meterRegistry.find("websocket.events.published")
            .tag("type", "transaction")
            .counter();

        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }
}
