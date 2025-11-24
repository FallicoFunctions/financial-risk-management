package com.nickfallico.financialriskmanagement.kafka.consumer;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import com.nickfallico.financialriskmanagement.eventstore.model.EventType;
import com.nickfallico.financialriskmanagement.eventstore.service.EventStoreService;
import com.nickfallico.financialriskmanagement.kafka.event.HighRiskUserIdentifiedEvent;
import com.nickfallico.financialriskmanagement.kafka.event.UserProfileUpdatedEvent;
import com.nickfallico.financialriskmanagement.service.AnalyticsService;
import com.nickfallico.financialriskmanagement.service.FraudAlertService;
import com.nickfallico.financialriskmanagement.service.MetricsService;
import com.nickfallico.financialriskmanagement.websocket.service.DashboardEventPublisher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Kafka consumer for risk and profile events.
 * Handles high-risk user identification and user profile updates.
 */
@Component
@ConditionalOnProperty(name = "spring.kafka.enabled", havingValue = "true", matchIfMissing = true)
@Slf4j
@RequiredArgsConstructor
public class RiskEventConsumer {

    private final EventStoreService eventStoreService;
    private final FraudAlertService fraudAlertService;
    private final AnalyticsService analyticsService;
    private final MetricsService metricsService;
    private final DashboardEventPublisher dashboardEventPublisher;

    /**
     * Handle HighRiskUserIdentifiedEvent - Critical alert requiring immediate action
     */
    @KafkaListener(
        topics = "${kafka.topic.high-risk-user}",
        groupId = "${spring.kafka.consumer.group-id}",
        containerFactory = "highRiskUserKafkaListenerContainerFactory"
    )
    public void handleHighRiskUserIdentified(
        @Payload HighRiskUserIdentifiedEvent event,
        @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
        @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
        @Header(KafkaHeaders.OFFSET) long offset
    ) {
        log.warn("========================================");
        log.warn("⚠️  HIGH RISK USER IDENTIFIED!");
        log.warn("User ID: {}", event.getUserId());
        log.warn("Risk Score: {}", event.getOverallRiskScore());
        log.warn("Risk Threshold: {}", event.getRiskThreshold());
        log.warn("Risk Factors: {}", event.getRiskFactors());
        log.warn("Total Transactions: {}", event.getTotalTransactions());
        log.warn("High Risk Transactions: {}", event.getHighRiskTransactions());
        log.warn("International Transactions: {}", event.getInternationalTransactions());
        log.warn("Total Transaction Value: {}", event.getTotalTransactionValue());
        log.warn("Alert Severity: {}", event.getAlertSeverity());
        log.warn("Recommended Action: {}", event.getRecommendedAction());
        log.warn("Event Timestamp: {}", event.getEventTimestamp());
        log.warn("========================================");

        // Store in event log for compliance and audit
        eventStoreService.storeEvent(
            EventType.HIGH_RISK_USER_IDENTIFIED,
            event.getUserId(),
            "USER",
            event,
            EventStoreService.createKafkaMetadata(topic, partition, offset)
        )
        .doOnSuccess(storedEvent ->
            log.warn("🔒 High-risk user event stored in audit log: sequenceNumber={}", storedEvent.getSequenceNumber())
        )
        .doOnError(error ->
            log.error("❌ CRITICAL: Failed to store high-risk user event in audit log!", error)
        )
        .subscribe();

        // Send alert to compliance team based on severity
        fraudAlertService.sendHighRiskUserAlert(event)
            .doOnSuccess(v -> log.info("✅ High-risk user alert sent to compliance team"))
            .doOnError(error -> log.error("❌ Failed to send high-risk user alert", error))
            .subscribe();

        // Record business metrics
        metricsService.recordUserRiskProfileUpdate(
            event.getUserId(),
            "NORMAL", // Previous level (unknown, using default)
            mapRiskScoreToLevel(event.getOverallRiskScore())
        );

        // Process analytics for ML training
        analyticsService.processHighRiskUserAnalytics(event)
            .doOnSuccess(v ->
                log.debug("✅ High-risk user analytics processed for user: {}", event.getUserId())
            )
            .doOnError(error ->
                log.error("❌ Failed to process high-risk user analytics", error)
            )
            .subscribe();

        // Log recommended action
        if ("SUSPEND".equals(event.getRecommendedAction())) {
            log.error("🚨 CRITICAL: User {} requires immediate account suspension!", event.getUserId());
        } else if ("REVIEW".equals(event.getRecommendedAction())) {
            log.warn("⚠️  User {} requires manual review by compliance team", event.getUserId());
        } else {
            log.info("ℹ️  User {} flagged for enhanced monitoring", event.getUserId());
        }

        // Publish to WebSocket for real-time dashboard streaming
        dashboardEventPublisher.publishHighRiskUserIdentified(event);
    }

    /**
     * Handle UserProfileUpdatedEvent - User risk profile changed
     */
    @KafkaListener(
        topics = "${kafka.topic.user-profile-updated}",
        groupId = "${spring.kafka.consumer.group-id}",
        containerFactory = "userProfileUpdatedKafkaListenerContainerFactory"
    )
    public void handleUserProfileUpdated(
        @Payload UserProfileUpdatedEvent event,
        @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
        @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
        @Header(KafkaHeaders.OFFSET) long offset
    ) {
        log.info("========================================");
        log.info("📊 USER PROFILE UPDATED EVENT!");
        log.info("User ID: {}", event.getUserId());
        log.info("Previous Risk Score: {}", event.getPreviousOverallRiskScore());
        log.info("New Risk Score: {}", event.getNewOverallRiskScore());
        log.info("Total Transactions: {}", event.getTotalTransactions());
        log.info("Total Transaction Value: {}", event.getTotalTransactionValue());
        log.info("High Risk Transactions: {}", event.getHighRiskTransactions());
        log.info("Update Reason: {}", event.getUpdateReason());
        log.info("Triggering Transaction ID: {}", event.getTriggeringTransactionId());
        log.info("Event Timestamp: {}", event.getEventTimestamp());
        log.info("========================================");

        // Store in event log for audit trail
        eventStoreService.storeEvent(
            EventType.USER_PROFILE_UPDATED,
            event.getUserId(),
            "USER",
            event,
            EventStoreService.createKafkaMetadata(topic, partition, offset)
        )
        .doOnSuccess(storedEvent ->
            log.debug("✅ User profile update event stored: sequenceNumber={}", storedEvent.getSequenceNumber())
        )
        .doOnError(error ->
            log.error("❌ Failed to store user profile update event", error)
        )
        .subscribe();

        // Record metrics for risk score change
        String previousLevel = mapRiskScoreToLevel(event.getPreviousOverallRiskScore());
        String newLevel = mapRiskScoreToLevel(event.getNewOverallRiskScore());

        metricsService.recordUserRiskProfileUpdate(
            event.getUserId(),
            previousLevel,
            newLevel
        );

        // Process analytics for business intelligence
        analyticsService.processUserProfileUpdateAnalytics(event)
            .doOnSuccess(v ->
                log.debug("✅ User profile update analytics processed for user: {}", event.getUserId())
            )
            .doOnError(error ->
                log.error("❌ Failed to process user profile update analytics", error)
            )
            .subscribe();

        // Log significant changes
        double riskChange = event.getNewOverallRiskScore() - event.getPreviousOverallRiskScore();
        if (Math.abs(riskChange) > 0.2) {
            log.warn("⚠️  Significant risk score change for user {}: {} -> {} ({})",
                event.getUserId(),
                event.getPreviousOverallRiskScore(),
                event.getNewOverallRiskScore(),
                event.getUpdateReason()
            );
        }

        log.debug("✅ User profile cache invalidated for user: {}", event.getUserId());

        // Publish to WebSocket for real-time dashboard streaming
        dashboardEventPublisher.publishUserProfileUpdated(event);
    }

    /**
     * Map risk score (0.0-1.0) to risk level string
     */
    private String mapRiskScoreToLevel(Double riskScore) {
        if (riskScore == null) return "UNKNOWN";
        if (riskScore >= 0.75) return "CRITICAL";
        if (riskScore >= 0.50) return "HIGH";
        if (riskScore >= 0.25) return "MEDIUM";
        return "LOW";
    }
}
