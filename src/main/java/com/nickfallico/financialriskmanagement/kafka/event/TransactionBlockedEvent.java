package com.nickfallico.financialriskmanagement.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Event published when a transaction is blocked due to fraud.
 * Critical security event for audit and monitoring.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionBlockedEvent {
    private UUID transactionId;
    private String userId;
    private BigDecimal amount;
    private String currency;
    private String merchantCategory;
    private Boolean isInternational;
    
    // Block details
    private String blockReason;
    private List<String> violatedRules;
    private Double fraudProbability;
    private String severity; // MEDIUM, HIGH, CRITICAL
    
    // Event metadata
    private Instant eventTimestamp;
    private UUID eventId;
    private String eventSource;
    
    public static TransactionBlockedEvent create(
        UUID transactionId,
        String userId,
        BigDecimal amount,
        String currency,
        String merchantCategory,
        Boolean isInternational,
        String blockReason,
        List<String> violatedRules,
        Double fraudProbability,
        String severity
    ) {
        return TransactionBlockedEvent.builder()
            .transactionId(transactionId)
            .userId(userId)
            .amount(amount)
            .currency(currency)
            .merchantCategory(merchantCategory)
            .isInternational(isInternational)
            .blockReason(blockReason)
            .violatedRules(violatedRules)
            .fraudProbability(fraudProbability)
            .severity(severity)
            .eventTimestamp(Instant.now())
            .eventId(UUID.randomUUID())
            .eventSource("fraud-detection-service")
            .build();
    }
}