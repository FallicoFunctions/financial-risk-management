package com.nickfallico.financialriskmanagement.model;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.Entity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Immutable value object representing user risk profile.
 * All state is computed from transaction history (event sourcing approach).
 * No setters; state changes via builder pattern only.
 */
@Entity
@jakarta.persistence.Table(name = "user_risk_profiles")
@Table("user_risk_profiles")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ImmutableUserRiskProfile {
    
    @jakarta.persistence.Id
    @Id
    private String userId;
    
    private double averageTransactionAmount;
    private int totalTransactions;
    private double totalTransactionValue;
    private int highRiskTransactions;
    private int internationalTransactions;
    
    private double behavioralRiskScore;
    private double transactionRiskScore;
    private double overallRiskScore;
    
    private Instant firstTransactionDate;
    private Instant lastTransactionDate;
    
    // Factory method: Create new profile for user
    public static ImmutableUserRiskProfile createNew(String userId) {
        Instant now = Instant.now();
        return ImmutableUserRiskProfile.builder()
            .userId(userId)
            .averageTransactionAmount(0.0)
            .totalTransactions(0)
            .totalTransactionValue(0.0)
            .highRiskTransactions(0)
            .internationalTransactions(0)
            .behavioralRiskScore(0.5)
            .transactionRiskScore(0.5)
            .overallRiskScore(0.5)
            .firstTransactionDate(now)
            .lastTransactionDate(now)
            .build();
    }
    
    // Builder method: Create updated profile (functional approach)
    public ImmutableUserRiskProfile withUpdatedMetrics(
        double avgAmount,
        int totalTx,
        double totalValue,
        int highRiskTx,
        int intlTx,
        double behavioralScore,
        double txScore,
        double overallScore,
        Instant lastUpdate) {
        
        return this.toBuilder()
            .averageTransactionAmount(avgAmount)
            .totalTransactions(totalTx)
            .totalTransactionValue(totalValue)
            .highRiskTransactions(highRiskTx)
            .internationalTransactions(intlTx)
            .behavioralRiskScore(behavioralScore)
            .transactionRiskScore(txScore)
            .overallRiskScore(overallScore)
            .lastTransactionDate(lastUpdate)
            .build();
    }
    
    /**
     * Check if this is a new user (zero or very few transactions)
     */
    public boolean isNewUser() {
        return this.totalTransactions <= 2;
    }
    
    /**
     * Check if user has moderate transaction history
     */
    public boolean hasModerateHistory() {
        return this.totalTransactions > 2 && this.totalTransactions <= 50;
    }
    
    /**
     * Check if user is established
     */
    public boolean isEstablished() {
        return this.totalTransactions > 50;
    }
}