package com.nickfallico.financialriskmanagement.websocket.service;

import java.time.Instant;

import org.springframework.stereotype.Service;

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

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

/**
 * Central event publisher for WebSocket dashboard streaming.
 * Uses Reactor Sinks for multicast event broadcasting to all connected clients.
 *
 * Supports:
 * - Fraud alerts (FraudDetected, FraudCleared, TransactionBlocked)
 * - Transaction events (TransactionCreated)
 * - Risk score updates (HighRiskUserIdentified, UserProfileUpdated)
 * - Metrics snapshots (periodic platform metrics)
 */
@Service
@Slf4j
public class DashboardEventPublisher {

    // Sinks for broadcasting events to WebSocket clients
    private final Sinks.Many<FraudAlertMessage> fraudAlertSink;
    private final Sinks.Many<TransactionEventMessage> transactionEventSink;
    private final Sinks.Many<RiskScoreUpdateMessage> riskScoreUpdateSink;
    private final Sinks.Many<MetricsSnapshotMessage> metricsSink;

    // Metrics counters
    private final Counter fraudAlertsPublished;
    private final Counter transactionEventsPublished;
    private final Counter riskScoreUpdatesPublished;
    private final Counter metricsSnapshotsPublished;

    public DashboardEventPublisher(MeterRegistry meterRegistry) {
        // Initialize Sinks with replay capability for late subscribers
        // multicast() allows multiple subscribers, onBackpressureBuffer handles slow consumers
        this.fraudAlertSink = Sinks.many().multicast().onBackpressureBuffer(100);
        this.transactionEventSink = Sinks.many().multicast().onBackpressureBuffer(1000);
        this.riskScoreUpdateSink = Sinks.many().multicast().onBackpressureBuffer(100);
        this.metricsSink = Sinks.many().multicast().onBackpressureBuffer(10);

        // Initialize metrics counters
        this.fraudAlertsPublished = Counter.builder("websocket.events.published")
            .tag("type", "fraud_alert")
            .tag("component", "websocket")
            .description("Number of fraud alerts published to WebSocket clients")
            .register(meterRegistry);

        this.transactionEventsPublished = Counter.builder("websocket.events.published")
            .tag("type", "transaction")
            .tag("component", "websocket")
            .description("Number of transaction events published to WebSocket clients")
            .register(meterRegistry);

        this.riskScoreUpdatesPublished = Counter.builder("websocket.events.published")
            .tag("type", "risk_score")
            .tag("component", "websocket")
            .description("Number of risk score updates published to WebSocket clients")
            .register(meterRegistry);

        this.metricsSnapshotsPublished = Counter.builder("websocket.events.published")
            .tag("type", "metrics")
            .tag("component", "websocket")
            .description("Number of metrics snapshots published to WebSocket clients")
            .register(meterRegistry);

        log.info("DashboardEventPublisher initialized with reactive Sinks");
    }

    // ========== Fraud Alert Publishing ==========

    /**
     * Publish a FraudDetectedEvent to connected dashboard clients.
     */
    public void publishFraudDetected(FraudDetectedEvent event) {
        FraudAlertMessage message = FraudAlertMessage.builder()
            .messageType(DashboardMessage.MessageType.FRAUD_DETECTED)
            .transactionId(event.getTransactionId())
            .userId(event.getUserId())
            .amount(event.getAmount())
            .currency(event.getCurrency())
            .fraudProbability(event.getFraudProbability())
            .riskLevel(event.getRiskLevel())
            .action(event.getAction())
            .violatedRules(event.getViolatedRules())
            .merchantCategory(event.getMerchantCategory())
            .isInternational(event.getIsInternational())
            .eventTimestamp(event.getEventTimestamp())
            .publishedAt(Instant.now())
            .build();

        emitFraudAlert(message);
    }

    /**
     * Publish a FraudClearedEvent to connected dashboard clients.
     */
    public void publishFraudCleared(FraudClearedEvent event) {
        FraudAlertMessage message = FraudAlertMessage.builder()
            .messageType(DashboardMessage.MessageType.FRAUD_CLEARED)
            .transactionId(event.getTransactionId())
            .userId(event.getUserId())
            .amount(event.getAmount())
            .currency(event.getCurrency())
            .fraudProbability(event.getFraudProbability())
            .riskLevel(event.getRiskLevel())
            .checksPerformed(event.getChecksPerformed())
            .merchantCategory(event.getMerchantCategory())
            .eventTimestamp(event.getEventTimestamp())
            .publishedAt(Instant.now())
            .build();

        emitFraudAlert(message);
    }

    /**
     * Publish a TransactionBlockedEvent to connected dashboard clients.
     */
    public void publishTransactionBlocked(TransactionBlockedEvent event) {
        FraudAlertMessage message = FraudAlertMessage.builder()
            .messageType(DashboardMessage.MessageType.TRANSACTION_BLOCKED)
            .transactionId(event.getTransactionId())
            .userId(event.getUserId())
            .amount(event.getAmount())
            .currency(event.getCurrency())
            .fraudProbability(event.getFraudProbability())
            .blockReason(event.getBlockReason())
            .violatedRules(event.getViolatedRules())
            .severity(event.getSeverity())
            .merchantCategory(event.getMerchantCategory())
            .eventTimestamp(event.getEventTimestamp())
            .publishedAt(Instant.now())
            .build();

        emitFraudAlert(message);
    }

    private void emitFraudAlert(FraudAlertMessage message) {
        Sinks.EmitResult result = fraudAlertSink.tryEmitNext(message);
        if (result.isFailure()) {
            log.warn("Failed to emit fraud alert to WebSocket sink: {}", result);
        } else {
            fraudAlertsPublished.increment();
            log.debug("Published fraud alert to WebSocket: type={}, transactionId={}",
                message.getMessageType(), message.getTransactionId());
        }
    }

    // ========== Transaction Event Publishing ==========

    /**
     * Publish a TransactionCreatedEvent to connected dashboard clients.
     */
    public void publishTransactionCreated(TransactionCreatedEvent event) {
        TransactionEventMessage message = TransactionEventMessage.builder()
            .messageType(DashboardMessage.MessageType.TRANSACTION_CREATED)
            .transactionId(event.getTransactionId())
            .userId(event.getUserId())
            .amount(event.getAmount())
            .currency(event.getCurrency())
            .transactionType(event.getTransactionType())
            .merchantCategory(event.getMerchantCategory())
            .merchantName(event.getMerchantName())
            .isInternational(event.getIsInternational())
            .country(event.getCountry())
            .city(event.getCity())
            .eventTimestamp(event.getEventTimestamp())
            .publishedAt(Instant.now())
            .build();

        Sinks.EmitResult result = transactionEventSink.tryEmitNext(message);
        if (result.isFailure()) {
            log.warn("Failed to emit transaction event to WebSocket sink: {}", result);
        } else {
            transactionEventsPublished.increment();
            log.debug("Published transaction event to WebSocket: transactionId={}",
                message.getTransactionId());
        }
    }

    // ========== Risk Score Update Publishing ==========

    /**
     * Publish a HighRiskUserIdentifiedEvent to connected dashboard clients.
     */
    public void publishHighRiskUserIdentified(HighRiskUserIdentifiedEvent event) {
        RiskScoreUpdateMessage message = RiskScoreUpdateMessage.builder()
            .messageType(DashboardMessage.MessageType.HIGH_RISK_USER_IDENTIFIED)
            .userId(event.getUserId())
            .overallRiskScore(event.getOverallRiskScore())
            .riskThreshold(event.getRiskThreshold())
            .alertSeverity(event.getAlertSeverity())
            .riskFactors(event.getRiskFactors())
            .eventTimestamp(event.getEventTimestamp())
            .publishedAt(Instant.now())
            .build();

        emitRiskScoreUpdate(message);
    }

    /**
     * Publish a UserProfileUpdatedEvent to connected dashboard clients.
     */
    public void publishUserProfileUpdated(UserProfileUpdatedEvent event) {
        RiskScoreUpdateMessage message = RiskScoreUpdateMessage.builder()
            .messageType(DashboardMessage.MessageType.USER_PROFILE_UPDATED)
            .userId(event.getUserId())
            .previousRiskScore(event.getPreviousOverallRiskScore())
            .newRiskScore(event.getNewOverallRiskScore())
            .updateReason(event.getUpdateReason())
            .eventTimestamp(event.getEventTimestamp())
            .publishedAt(Instant.now())
            .build();

        emitRiskScoreUpdate(message);
    }

    private void emitRiskScoreUpdate(RiskScoreUpdateMessage message) {
        Sinks.EmitResult result = riskScoreUpdateSink.tryEmitNext(message);
        if (result.isFailure()) {
            log.warn("Failed to emit risk score update to WebSocket sink: {}", result);
        } else {
            riskScoreUpdatesPublished.increment();
            log.debug("Published risk score update to WebSocket: type={}, userId={}",
                message.getMessageType(), message.getUserId());
        }
    }

    // ========== Metrics Publishing ==========

    /**
     * Publish a metrics snapshot to connected dashboard clients.
     */
    public void publishMetricsSnapshot(MetricsSnapshotMessage message) {
        Sinks.EmitResult result = metricsSink.tryEmitNext(message);
        if (result.isFailure()) {
            log.warn("Failed to emit metrics snapshot to WebSocket sink: {}", result);
        } else {
            metricsSnapshotsPublished.increment();
            log.debug("Published metrics snapshot to WebSocket");
        }
    }

    // ========== Flux Accessors for WebSocket Handlers ==========

    /**
     * Get the Flux stream of fraud alerts for WebSocket clients.
     */
    public Flux<FraudAlertMessage> getFraudAlertStream() {
        return fraudAlertSink.asFlux();
    }

    /**
     * Get the Flux stream of transaction events for WebSocket clients.
     */
    public Flux<TransactionEventMessage> getTransactionEventStream() {
        return transactionEventSink.asFlux();
    }

    /**
     * Get the Flux stream of risk score updates for WebSocket clients.
     */
    public Flux<RiskScoreUpdateMessage> getRiskScoreUpdateStream() {
        return riskScoreUpdateSink.asFlux();
    }

    /**
     * Get the Flux stream of metrics snapshots for WebSocket clients.
     */
    public Flux<MetricsSnapshotMessage> getMetricsStream() {
        return metricsSink.asFlux();
    }
}
