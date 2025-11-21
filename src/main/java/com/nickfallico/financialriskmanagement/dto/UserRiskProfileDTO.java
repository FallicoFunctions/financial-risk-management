package com.nickfallico.financialriskmanagement.dto;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for user risk profile API responses.
 * Provides a comprehensive view of user risk metrics and transaction history.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRiskProfileDTO {
    
    // User identification
    private String userId;
    
    // Risk scores (0.0 to 1.0 where 1.0 is highest risk)
    private Double overallRiskScore;
    private Double behavioralRiskScore;
    private Double transactionRiskScore;
    private String riskLevel;  // LOW, MEDIUM, HIGH, CRITICAL
    
    // Transaction statistics
    private Integer totalTransactions;
    private Integer highRiskTransactions;
    private Integer internationalTransactions;
    private Double averageTransactionAmount;
    private Double totalTransactionValue;
    
    // Account information
    private Instant firstTransactionDate;
    private Instant lastTransactionDate;
    private Long accountAgeDays;
    
    // User classification
    private String userType;  // NEW_USER, MODERATE_HISTORY, ESTABLISHED
    
    // Metadata
    private Instant profileLastUpdated;
    
    /**
     * Convert risk score to human-readable risk level.
     */
    public static String calculateRiskLevel(Double overallRiskScore) {
        if (overallRiskScore >= 0.8) return "CRITICAL";
        if (overallRiskScore >= 0.6) return "HIGH";
        if (overallRiskScore >= 0.4) return "MEDIUM";
        return "LOW";
    }
    
    /**
     * Determine user type based on transaction history.
     */
    public static String calculateUserType(Integer totalTransactions) {
        if (totalTransactions <= 2) return "NEW_USER";
        if (totalTransactions <= 50) return "MODERATE_HISTORY";
        return "ESTABLISHED";
    }
    
    /**
     * Calculate account age in days.
     */
    public static Long calculateAccountAgeDays(Instant firstTransaction, Instant lastTransaction) {
        if (firstTransaction == null || lastTransaction == null) {
            return 0L;
        }
        return java.time.Duration.between(firstTransaction, lastTransaction).toDays();
    }
}