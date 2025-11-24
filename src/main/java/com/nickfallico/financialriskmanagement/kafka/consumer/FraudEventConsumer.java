package com.nickfallico.financialriskmanagement.kafka.consumer;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import com.nickfallico.financialriskmanagement.eventstore.model.EventType;
import com.nickfallico.financialriskmanagement.eventstore.service.EventStoreService;
import com.nickfallico.financialriskmanagement.kafka.event.FraudClearedEvent;
import com.nickfallico.financialriskmanagement.kafka.event.FraudDetectedEvent;
import com.nickfallico.financialriskmanagement.kafka.event.TransactionBlockedEvent;
import com.nickfallico.financialriskmanagement.service.FraudAlertService;
import com.nickfallico.financialriskmanagement.service.NotificationService;
import com.nickfallico.financialriskmanagement.service.UserProfileService;
import com.nickfallico.financialriskmanagement.websocket.service.DashboardEventPublisher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Kafka consumer for fraud-related events.
 * Handles fraud detection, clearance, and transaction blocking.
 * All events are stored in event log for compliance.
 */
@Component
@ConditionalOnProperty(name = "spring.kafka.enabled", havingValue = "true", matchIfMissing = true)
@Slf4j
@RequiredArgsConstructor
public class FraudEventConsumer {
    
    private final EventStoreService eventStoreService;
    private final FraudAlertService fraudAlertService;
    private final UserProfileService userProfileService;
    private final NotificationService notificationService;
    private final DashboardEventPublisher dashboardEventPublisher;
    
    /**
     * Handle FraudDetectedEvent - Critical security event
     */
    @KafkaListener(
        topics = "${kafka.topic.fraud-detected}",
        groupId = "${spring.kafka.consumer.group-id}",
        containerFactory = "fraudDetectedKafkaListenerContainerFactory"
    )
    public void handleFraudDetected(
        @Payload FraudDetectedEvent event,
        @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
        @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
        @Header(KafkaHeaders.OFFSET) long offset
    ) {
        log.error("========================================");
        log.error("🚨 FRAUD DETECTED EVENT!");
        log.error("Transaction ID: {}", event.getTransactionId());
        log.error("User ID: {}", event.getUserId());
        log.error("Amount: {} {}", event.getAmount(), event.getCurrency());
        log.error("Fraud Probability: {}%", event.getFraudProbability() * 100);
        log.error("Risk Level: {}", event.getRiskLevel());
        log.error("Violated Rules: {}", event.getViolatedRules());
        log.error("Action: {}", event.getAction());
        log.error("Event Timestamp: {}", event.getEventTimestamp());
        log.error("========================================");
        
        // Store in event log - CRITICAL for compliance and forensics
        eventStoreService.storeEvent(
            EventType.FRAUD_DETECTED,
            event.getUserId(),
            "USER",
            event,
            EventStoreService.createKafkaMetadata(topic, partition, offset)
        )
        .doOnSuccess(storedEvent -> 
            log.warn("🔒 Fraud event stored in audit log: sequenceNumber={}", storedEvent.getSequenceNumber())
        )
        .doOnError(error -> 
            log.error("❌ CRITICAL: Failed to store fraud event in audit log!", error)
        )
        .subscribe();
        
        // Send alert to fraud investigation team
        fraudAlertService.sendFraudAlert(event)
            .doOnSuccess(v -> log.info("✅ Fraud alert sent to investigation team"))
            .doOnError(error -> log.error("❌ Failed to send fraud alert", error))
            .subscribe();
        
        // Update user's risk score immediately
        userProfileService.increaseRiskScoreForFraud(
                event.getUserId(),
                event.getFraudProbability(),
                event.getRiskLevel()
            )
            .doOnSuccess(profile -> 
                log.info("✅ User risk score updated: userId={}, newScore={}", 
                    event.getUserId(), profile.getOverallRiskScore())
            )
            .doOnError(error -> 
                log.error("❌ Failed to update user risk score", error)
            )
            .subscribe();
        
        // Optionally notify user (be careful - could alert fraudsters)
        // Only for REVIEW actions, not BLOCK actions
        if ("REVIEW".equals(event.getAction())) {
            notificationService.sendSuspiciousActivityAlert(
                    event.getUserId(),
                    event.getTransactionId(),
                    "Unusual activity detected on your account"
                )
                .doOnError(error -> log.error("❌ Failed to send user notification", error))
                .subscribe();
        }
        
        log.warn("⚠️  Fraud investigation required for user: {}", event.getUserId());

        // Publish to WebSocket for real-time dashboard streaming
        dashboardEventPublisher.publishFraudDetected(event);
    }
    
    /**
     * Handle FraudClearedEvent - Transaction passed all checks
     */
    @KafkaListener(
        topics = "${kafka.topic.fraud-cleared}",
        groupId = "${spring.kafka.consumer.group-id}",
        containerFactory = "fraudClearedKafkaListenerContainerFactory"
    )
    public void handleFraudCleared(
        @Payload FraudClearedEvent event,
        @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
        @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
        @Header(KafkaHeaders.OFFSET) long offset
    ) {
        log.info("========================================");
        log.info("✅ FRAUD CLEARED EVENT!");
        log.info("Transaction ID: {}", event.getTransactionId());
        log.info("User ID: {}", event.getUserId());
        log.info("Amount: {} {}", event.getAmount(), event.getCurrency());
        log.info("Fraud Probability: {}%", event.getFraudProbability() * 100);
        log.info("Risk Level: {}", event.getRiskLevel());
        log.info("Checks Performed: {}", event.getChecksPerformed());
        log.info("Event Timestamp: {}", event.getEventTimestamp());
        log.info("========================================");
        
        // Store in event log
        eventStoreService.storeEvent(
            EventType.FRAUD_CLEARED,
            event.getUserId(),
            "USER",
            event,
            EventStoreService.createKafkaMetadata(topic, partition, offset)
        )
        .doOnSuccess(storedEvent -> 
            log.debug("✅ Fraud cleared event stored: sequenceNumber={}", storedEvent.getSequenceNumber())
        )
        .subscribe();
        
        // Log successful validation (for fraud detection metrics - true negatives)
        log.info("✅ Transaction cleared fraud checks: transactionId={}, checksPerformed={}",
            event.getTransactionId(), event.getChecksPerformed());
        
        // Send transaction confirmation to user
        notificationService.sendTransactionConfirmation(
                event.getUserId(),
                event.getTransactionId(),
                event.getAmount(),
                event.getCurrency(),
                event.getMerchantCategory()
            )
            .doOnError(error -> log.error("❌ Failed to send transaction confirmation", error))
            .subscribe();

        // Publish to WebSocket for real-time dashboard streaming
        dashboardEventPublisher.publishFraudCleared(event);
    }
    
    /**
     * Handle TransactionBlockedEvent - Transaction rejected
     */
    @KafkaListener(
        topics = "${kafka.topic.transaction-blocked}",
        groupId = "${spring.kafka.consumer.group-id}",
        containerFactory = "transactionBlockedKafkaListenerContainerFactory"
    )
    public void handleTransactionBlocked(
        @Payload TransactionBlockedEvent event,
        @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
        @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
        @Header(KafkaHeaders.OFFSET) long offset
    ) {
        log.error("========================================");
        log.error("🛑 TRANSACTION BLOCKED EVENT!");
        log.error("Transaction ID: {}", event.getTransactionId());
        log.error("User ID: {}", event.getUserId());
        log.error("Amount: {} {}", event.getAmount(), event.getCurrency());
        log.error("Merchant Category: {}", event.getMerchantCategory());
        log.error("Block Reason: {}", event.getBlockReason());
        log.error("Violated Rules: {}", event.getViolatedRules());
        log.error("Fraud Probability: {}%", event.getFraudProbability() * 100);
        log.error("Severity: {}", event.getSeverity());
        log.error("Event Timestamp: {}", event.getEventTimestamp());
        log.error("========================================");
        
        // Store in event log - CRITICAL for compliance
        eventStoreService.storeEvent(
            EventType.TRANSACTION_BLOCKED,
            event.getUserId(),
            "USER",
            event,
            EventStoreService.createKafkaMetadata(topic, partition, offset)
        )
        .doOnSuccess(storedEvent -> 
            log.error("🔒 Blocked transaction stored in audit log: sequenceNumber={}", storedEvent.getSequenceNumber())
        )
        .doOnError(error -> 
            log.error("❌ CRITICAL: Failed to store blocked transaction in audit log!", error)
        )
        .subscribe();
        
        // Send alert to fraud team
        fraudAlertService.sendBlockedTransactionAlert(event)
            .doOnSuccess(v -> log.info("✅ Blocked transaction alert sent"))
            .doOnError(error -> log.error("❌ Failed to send blocked transaction alert", error))
            .subscribe();
        
        // Notify user that transaction was blocked
        notificationService.sendTransactionBlockedNotification(
                event.getUserId(),
                event.getTransactionId(),
                event.getAmount(),
                event.getCurrency(),
                event.getBlockReason()
            )
            .doOnError(error -> log.error("❌ Failed to send blocked transaction notification", error))
            .subscribe();
        
        if ("CRITICAL".equals(event.getSeverity())) {
            log.error("🚨 CRITICAL SEVERITY - Immediate review required!");
        }

        // Publish to WebSocket for real-time dashboard streaming
        dashboardEventPublisher.publishTransactionBlocked(event);
    }
}