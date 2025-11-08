package com.nickfallico.financialriskmanagement.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Event published when a user's risk profile is updated.
 * Used for cache invalidation and analytics.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileUpdatedEvent {
    private String userId;
    
    // Updated profile metrics
    private Double previousOverallRiskScore;
    private Double newOverallRiskScore;
    private Integer totalTransactions;
    private Double totalTransactionValue;
    private Integer highRiskTransactions;
    
    // Change details
    private String updateReason; // TRANSACTION_COMPLETED, FRAUD_DETECTED, etc.
    private UUID triggeringTransactionId;
    
    // Event metadata
    private Instant eventTimestamp;
    private UUID eventId;
    private String eventSource;
    
    public static UserProfileUpdatedEvent create(
        String userId,
        Double previousRiskScore,
        Double newRiskScore,
        Integer totalTransactions,
        Double totalValue,
        Integer highRiskTransactions,
        String updateReason,
        UUID triggeringTransactionId
    ) {
        return UserProfileUpdatedEvent.builder()
            .userId(userId)
            .previousOverallRiskScore(previousRiskScore)
            .newOverallRiskScore(newRiskScore)
            .totalTransactions(totalTransactions)
            .totalTransactionValue(totalValue)
            .highRiskTransactions(highRiskTransactions)
            .updateReason(updateReason)
            .triggeringTransactionId(triggeringTransactionId)
            .eventTimestamp(Instant.now())
            .eventId(UUID.randomUUID())
            .eventSource("profile-service")
            .build();
    }
}