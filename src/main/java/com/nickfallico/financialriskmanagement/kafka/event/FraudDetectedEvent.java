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
 * Event published when fraud is detected in a transaction.
 * Immutable record of fraud detection for compliance and investigation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FraudDetectedEvent {
    private UUID transactionId;
    private String userId;
    private BigDecimal amount;
    private String currency;
    private String merchantCategory;
    private Boolean isInternational;
    
    // Fraud-specific fields
    private Double fraudProbability;
    private List<String> violatedRules;
    private String riskLevel; // LOW, MEDIUM, HIGH, CRITICAL
    private String action; // REVIEW, BLOCK
    
    // Event metadata
    private Instant eventTimestamp;
    private UUID eventId;
    private String eventSource;
    
    public static FraudDetectedEvent create(
        UUID transactionId,
        String userId,
        BigDecimal amount,
        String currency,
        String merchantCategory,
        Boolean isInternational,
        Double fraudProbability,
        List<String> violatedRules,
        String riskLevel,
        String action
    ) {
        return FraudDetectedEvent.builder()
            .transactionId(transactionId)
            .userId(userId)
            .amount(amount)
            .currency(currency)
            .merchantCategory(merchantCategory)
            .isInternational(isInternational)
            .fraudProbability(fraudProbability)
            .violatedRules(violatedRules)
            .riskLevel(riskLevel)
            .action(action)
            .eventTimestamp(Instant.now())
            .eventId(UUID.randomUUID())
            .eventSource("fraud-detection-service")
            .build();
    }
}