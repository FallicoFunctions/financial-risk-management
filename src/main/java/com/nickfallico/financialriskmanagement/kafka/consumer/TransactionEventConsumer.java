package com.nickfallico.financialriskmanagement.kafka.consumer;

import com.nickfallico.financialriskmanagement.kafka.event.TransactionCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer that listens for transaction events.
 * Processes events asynchronously in the background.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class TransactionEventConsumer {
    
    /**
     * Listen for TransactionCreatedEvents from Kafka.
     * This runs asynchronously - does NOT block the API response!
     */
    @KafkaListener(
        topics = "${kafka.topic.transaction-created}",
        groupId = "${spring.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleTransactionCreated(TransactionCreatedEvent event) {
        log.info("========================================");
        log.info("📩 KAFKA EVENT RECEIVED!");
        log.info("Event Type: TransactionCreated");
        log.info("Transaction ID: {}", event.getTransactionId());
        log.info("User ID: {}", event.getUserId());
        log.info("Amount: {} {}", event.getAmount(), event.getCurrency());
        log.info("Merchant Category: {}", event.getMerchantCategory());
        log.info("International: {}", event.getIsInternational());
        log.info("Event Timestamp: {}", event.getEventTimestamp());
        log.info("========================================");
        
        // TODO: Add async fraud detection here in next phase
        // TODO: Add analytics processing here
        // TODO: Add notification sending here
    }
}