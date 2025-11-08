package com.nickfallico.financialriskmanagement.kafka.producer;

import com.nickfallico.financialriskmanagement.kafka.event.TransactionCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.concurrent.CompletableFuture;

/**
 * Kafka producer for transaction events.
 * Publishes events to Kafka topics asynchronously.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class TransactionEventProducer {
    
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    @Value("${kafka.topic.transaction-created}")
    private String transactionCreatedTopic;
    
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
        }).then();
    }
}