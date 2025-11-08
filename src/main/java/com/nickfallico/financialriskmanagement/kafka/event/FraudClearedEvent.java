package com.nickfallico.financialriskmanagement.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Event published when a transaction passes all fraud checks.
 * Indicates transaction is safe to process.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FraudClearedEvent {
    private UUID transactionId;
    private String userId;
    private BigDecimal amount;
    private String currency;
    private String merchantCategory;
    
    // Fraud clearance details
    private Double fraudProbability;
    private String riskLevel; // Should be LOW
    private Integer checksPerformed;
    
    // Event metadata
    private Instant eventTimestamp;
    private UUID eventId;
    private String eventSource;
    
    public static FraudClearedEvent create(
        UUID transactionId,
        String userId,
        BigDecimal amount,
        String currency,
        String merchantCategory,
        Double fraudProbability,
        Integer checksPerformed
    ) {
        return FraudClearedEvent.builder()
            .transactionId(transactionId)
            .userId(userId)
            .amount(amount)
            .currency(currency)
            .merchantCategory(merchantCategory)
            .fraudProbability(fraudProbability)
            .riskLevel("LOW")
            .checksPerformed(checksPerformed)
            .eventTimestamp(Instant.now())
            .eventId(UUID.randomUUID())
            .eventSource("fraud-detection-service")
            .build();
    }
}