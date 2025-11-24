package com.nickfallico.financialriskmanagement.websocket.message;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * WebSocket message for fraud-related alerts.
 * Covers FraudDetected, FraudCleared, and TransactionBlocked events.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class FraudAlertMessage extends DashboardMessage {

    // Transaction identification
    private UUID transactionId;
    private String userId;

    // Transaction details
    private BigDecimal amount;
    private String currency;
    private String merchantCategory;
    private Boolean isInternational;

    // Fraud assessment
    private Double fraudProbability;
    private String riskLevel;
    private List<String> violatedRules;
    private Integer checksPerformed;

    // Action taken
    private String action;         // For FRAUD_DETECTED: REVIEW, BLOCK
    private String blockReason;    // For TRANSACTION_BLOCKED
    private String severity;       // For TRANSACTION_BLOCKED: WARNING, CRITICAL
}
