package com.nickfallico.financialriskmanagement.websocket.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * WebSocket message for real-time metrics streaming.
 * Provides periodic snapshots of platform performance metrics.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class MetricsSnapshotMessage extends DashboardMessage {

    // Transaction metrics
    private Long totalTransactionsProcessed;
    private Long transactionsLastHour;
    private Long transactionsLastMinute;

    // Fraud metrics
    private Long totalFraudDetected;
    private Long fraudDetectedLastHour;
    private Long transactionsBlocked;
    private Long transactionsBlockedLastHour;
    private Double fraudDetectionRate;

    // Risk metrics
    private Double averageRiskScore;
    private Long highRiskTransactions;

    // Performance metrics
    private Double avgFraudDetectionTimeMs;
    private Double avgTransactionProcessingTimeMs;

    // WebSocket metrics
    private Integer activeWebSocketConnections;
    private Long totalMessagesPublished;
}
