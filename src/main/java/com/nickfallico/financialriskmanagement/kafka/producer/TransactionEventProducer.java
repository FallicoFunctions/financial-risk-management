package com.nickfallico.financialriskmanagement.kafka.producer;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import com.nickfallico.financialriskmanagement.kafka.event.FraudClearedEvent;
import com.nickfallico.financialriskmanagement.kafka.event.FraudDetectedEvent;
import com.nickfallico.financialriskmanagement.kafka.event.TransactionBlockedEvent;
import com.nickfallico.financialriskmanagement.kafka.event.TransactionCreatedEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * Kafka producer for transaction events.
 * Publishes events to Kafka topics asynchronously.
 */
@Component
@ConditionalOnProperty(name = "spring.kafka.enabled", havingValue = "true", matchIfMissing = true)
@Slf4j
@RequiredArgsConstructor
public class TransactionEventProducer {
    
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    @Value("${kafka.topic.transaction-created}")
    private String transactionCreatedTopic;

    @Value("${kafka.topic.fraud-detected}")
    private String fraudDetectedTopic;

    @Value("${kafka.topic.fraud-cleared}")
    private String fraudClearedTopic;

    @Value("${kafka.topic.transaction-blocked}")
    private String transactionBlockedTopic;
    
    /**
     * Publish TransactionCreatedEvent to Kafka.
     * Non-blocking, returns immediately.
     */
    public Mono<Void> publishTransactionCreated(TransactionCreatedEvent event) {
        return Mono.fromFuture(() -> {
            log.info("Publishing TransactionCreatedEvent: txId={}, userId={}, amount={}", 
                event.getTransactionId(), event.getUserId(), event.getAmount());
            
            CompletableFuture<SendResult<String, Object>> future = 
                kafkaTemplate.send(transactionCreatedTopic, event.getUserId(), event);
            
            return future.thenApply(result -> {
                log.info("Successfully published to Kafka: topic={}, partition={}, offset={}", 
                    result.getRecordMetadata().topic(),
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset());
                return null;
            }).exceptionally(ex -> {
                log.error("Failed to publish TransactionCreatedEvent: {}", ex.getMessage(), ex);
                return null;
            });
        })
        .timeout(Duration.ofSeconds(5))  // Don't wait more than 5 seconds
        .onErrorResume(e -> {            // Continue even if Kafka fails
            log.error("Kafka publish timeout/error for TransactionCreatedEvent, continuing anyway", e);
            return Mono.empty();
        })
        .then();
    }

    /**
     * Publish FraudDetectedEvent to Kafka.
     */
    public Mono<Void> publishFraudDetected(FraudDetectedEvent event) {
        return Mono.fromFuture(() -> {
            log.info("Publishing FraudDetectedEvent: txId={}, userId={}, probability={}, rules={}", 
                event.getTransactionId(), event.getUserId(), event.getFraudProbability(), event.getViolatedRules());
            
            CompletableFuture<SendResult<String, Object>> future = 
                kafkaTemplate.send(fraudDetectedTopic, event.getUserId(), event);
            
            return future.thenApply(result -> {
                log.warn("🚨 FRAUD DETECTED: Published to Kafka: topic={}, partition={}, offset={}", 
                    result.getRecordMetadata().topic(),
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset());
                return null;
            }).exceptionally(ex -> {
                log.error("Failed to publish FraudDetectedEvent: {}", ex.getMessage(), ex);
                return null;
            });
        })
        .timeout(Duration.ofSeconds(5))  // Don't wait more than 5 seconds
        .onErrorResume(e -> {            // Continue even if Kafka fails
            log.error("Kafka publish timeout/error for FraudDetectedEvent, continuing anyway", e);
            return Mono.empty();
        })
        .then();
    }

    /**
     * Publish FraudClearedEvent to Kafka.
     */
    public Mono<Void> publishFraudCleared(FraudClearedEvent event) {
        return Mono.fromFuture(() -> {
            log.info("Publishing FraudClearedEvent: txId={}, userId={}, probability={}", 
                event.getTransactionId(), event.getUserId(), event.getFraudProbability());
            
            CompletableFuture<SendResult<String, Object>> future = 
                kafkaTemplate.send(fraudClearedTopic, event.getUserId(), event);
            
            return future.thenApply(result -> {
                log.info("✅ FRAUD CLEARED: Published to Kafka: topic={}, partition={}, offset={}", 
                    result.getRecordMetadata().topic(),
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset());
                return null;
            }).exceptionally(ex -> {
                log.error("Failed to publish FraudClearedEvent: {}", ex.getMessage(), ex);
                return null;
            });
        })
        .timeout(Duration.ofSeconds(5))  // Don't wait more than 5 seconds
        .onErrorResume(e -> {            // Continue even if Kafka fails
            log.error("Kafka publish timeout/error for FraudClearedEvent, continuing anyway", e);
            return Mono.empty();
        })
        .then();
    }

    /**
     * Publish TransactionBlockedEvent to Kafka.
     */
    public Mono<Void> publishTransactionBlocked(TransactionBlockedEvent event) {
        return Mono.fromFuture(() -> {
            log.error("Publishing TransactionBlockedEvent: txId={}, userId={}, amount={}, reason={}", 
                event.getTransactionId(), event.getUserId(), event.getAmount(), event.getBlockReason());

            // DEBUG: Log the exact topic name being used
            log.error("DEBUG: About to publish to topic: [{}]", transactionBlockedTopic);
            log.error("DEBUG: Topic name length: {}", transactionBlockedTopic.length());
            log.error("DEBUG: Topic bytes: {}", java.util.Arrays.toString(transactionBlockedTopic.getBytes()));
            
            CompletableFuture<SendResult<String, Object>> future = 
                kafkaTemplate.send(transactionBlockedTopic, event.getUserId(), event);
            
            return future.thenApply(result -> {
                log.error("🛑 TRANSACTION BLOCKED: Published to Kafka: topic={}, partition={}, offset={}", 
                    result.getRecordMetadata().topic(),
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset());
                return null;
            }).exceptionally(ex -> {
                log.error("Failed to publish TransactionBlockedEvent: {}", ex.getMessage(), ex);
                return null;
            });
        })
        .timeout(Duration.ofSeconds(5))  // Don't wait more than 5 seconds
        .onErrorResume(e -> {            // Continue even if Kafka fails
            log.error("Kafka publish timeout/error for TransactionBlockedEvent, continuing anyway", e);
            return Mono.empty();
        })
        .then();
    }
}