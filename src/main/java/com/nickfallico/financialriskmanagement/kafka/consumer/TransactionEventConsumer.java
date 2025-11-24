package com.nickfallico.financialriskmanagement.kafka.consumer;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import com.nickfallico.financialriskmanagement.eventstore.model.EventType;
import com.nickfallico.financialriskmanagement.eventstore.service.EventStoreService;
import com.nickfallico.financialriskmanagement.kafka.event.TransactionCreatedEvent;
import com.nickfallico.financialriskmanagement.service.AnalyticsService;
import com.nickfallico.financialriskmanagement.websocket.service.DashboardEventPublisher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Kafka consumer that listens for transaction events.
 * Processes events asynchronously and stores them in event log.
 */
@Component
@ConditionalOnProperty(name = "spring.kafka.enabled", havingValue = "true", matchIfMissing = true)
@Slf4j
@RequiredArgsConstructor
public class TransactionEventConsumer {
    
    private final EventStoreService eventStoreService;
    private final AnalyticsService analyticsService;
    private final DashboardEventPublisher dashboardEventPublisher;
    
    /**
     * Listen for TransactionCreatedEvents from Kafka.
     * This runs asynchronously - does NOT block the API response!
     * 
     * Now also stores every event in the event log for audit trail.
     */
    @KafkaListener(
        topics = "${kafka.topic.transaction-created}",
        groupId = "${spring.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleTransactionCreated(
        @Payload TransactionCreatedEvent event,
        @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
        @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
        @Header(KafkaHeaders.OFFSET) long offset
    ) {
        log.info("========================================");
        log.info("📩 KAFKA EVENT RECEIVED!");
        log.info("Event Type: TransactionCreated");
        log.info("Transaction ID: {}", event.getTransactionId());
        log.info("User ID: {}", event.getUserId());
        log.info("Amount: {} {}", event.getAmount(), event.getCurrency());
        log.info("Merchant Category: {}", event.getMerchantCategory());
        log.info("International: {}", event.getIsInternational());
        log.info("Event Timestamp: {}", event.getEventTimestamp());
        log.info("Kafka Metadata: topic={}, partition={}, offset={}", topic, partition, offset);
        log.info("========================================");
        
        // Store event in event log for audit trail
        eventStoreService.storeEvent(
            EventType.TRANSACTION_CREATED,
            event.getUserId(),
            "USER",
            event,
            EventStoreService.createKafkaMetadata(topic, partition, offset)
        )
        .doOnSuccess(storedEvent -> 
            log.debug("✅ Event stored in event log: sequenceNumber={}", storedEvent.getSequenceNumber())
        )
        .doOnError(error -> 
            log.error("❌ Failed to store event in event log", error)
        )
        .subscribe();
        
        // Process analytics for ML training and business intelligence
        analyticsService.processTransactionAnalytics(event)
            .doOnSuccess(v ->
                log.debug("✅ Analytics processed for transaction: {}", event.getTransactionId())
            )
            .doOnError(error ->
                log.error("❌ Failed to process analytics for transaction: {}",
                    event.getTransactionId(), error)
            )
            .subscribe();

        // Publish to WebSocket for real-time dashboard streaming
        dashboardEventPublisher.publishTransactionCreated(event);
    }
}