package com.nickfallico.financialriskmanagement.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Event published when a user crosses the high-risk threshold.
 * Critical alert requiring immediate investigation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HighRiskUserIdentifiedEvent {
    private String userId;
    
    // Risk details
    private Double overallRiskScore;
    private Double riskThreshold;
    private List<String> riskFactors;
    
    // User activity summary
    private Integer totalTransactions;
    private Integer highRiskTransactions;
    private Integer internationalTransactions;
    private Double totalTransactionValue;
    
    // Alert details
    private String alertSeverity; // WARNING, URGENT, CRITICAL
    private String recommendedAction; // MONITOR, REVIEW, SUSPEND
    
    // Event metadata
    private Instant eventTimestamp;
    private UUID eventId;
    private String eventSource;
    
    public static HighRiskUserIdentifiedEvent create(
        String userId,
        Double riskScore,
        Double threshold,
        List<String> riskFactors,
        Integer totalTx,
        Integer highRiskTx,
        Integer intlTx,
        Double totalValue,
        String severity,
        String recommendedAction
    ) {
        return HighRiskUserIdentifiedEvent.builder()
            .userId(userId)
            .overallRiskScore(riskScore)
            .riskThreshold(threshold)
            .riskFactors(riskFactors)
            .totalTransactions(totalTx)
            .highRiskTransactions(highRiskTx)
            .internationalTransactions(intlTx)
            .totalTransactionValue(totalValue)
            .alertSeverity(severity)
            .recommendedAction(recommendedAction)
            .eventTimestamp(Instant.now())
            .eventId(UUID.randomUUID())
            .eventSource("risk-monitoring-service")
            .build();
    }
}