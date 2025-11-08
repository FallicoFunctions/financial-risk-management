package com.nickfallico.financialriskmanagement.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Immutable event representing a transaction creation.
 * Published to Kafka when a new transaction is created.
 * Thread-safe, serializable for Kafka.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionCreatedEvent {
    
    private UUID transactionId;
    private String userId;
    private BigDecimal amount;
    private String currency;
    private Instant createdAt;
    private String transactionType;
    private String merchantCategory;
    private String merchantName;
    private Boolean isInternational;
    
    // Event metadata
    private Instant eventTimestamp;
    private String eventId;
    private String eventSource;
    
    /**
     * Factory method to create event from transaction.
     */
    public static TransactionCreatedEvent fromTransaction(
        com.nickfallico.financialriskmanagement.model.Transactions transaction) {
        
        return TransactionCreatedEvent.builder()
            .transactionId(transaction.getId())
            .userId(transaction.getUserId())
            .amount(transaction.getAmount())
            .currency(transaction.getCurrency())
            .createdAt(transaction.getCreatedAt())
            .transactionType(transaction.getTransactionType() != null 
                ? transaction.getTransactionType().name() 
                : null)
            .merchantCategory(transaction.getMerchantCategory())
            .merchantName(transaction.getMerchantName())
            .isInternational(transaction.getIsInternational())
            .eventTimestamp(Instant.now())
            .eventId(UUID.randomUUID().toString())
            .eventSource("transaction-service")
            .build();
    }
}