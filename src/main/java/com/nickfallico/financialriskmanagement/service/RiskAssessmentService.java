package com.nickfallico.financialriskmanagement.service;

import com.nickfallico.financialriskmanagement.model.Transactions;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class RiskAssessmentService {

    private final TransactionService transactionService;

    public Mono<RiskScore> assessTransactionRisk(Transactions transaction) {
        // Use transactionService to get daily total for risk assessment
        return transactionService.getDailyTotal(transaction.getUserId())
            .map(dailyTotal -> calculateRiskScore(transaction, dailyTotal));
    }

    private RiskScore calculateRiskScore(Transactions transaction, BigDecimal dailyTotal) {
        int baseScore = 50; // Neutral risk score
        
        // Adjust risk based on transaction characteristics
        if (transaction.getAmount().compareTo(BigDecimal.valueOf(10000)) > 0) {
            baseScore += 30; // High amount increases risk
        }

        // Check daily total impact
        if (dailyTotal.add(transaction.getAmount()).compareTo(BigDecimal.valueOf(20000)) > 0) {
            baseScore += 25; // Exceeding daily limit
        }

        if (Boolean.TRUE.equals(transaction.getIsInternational())) {
            baseScore += 20; // International transactions are riskier
        }

        // Check merchant category risks
        if ("GAMBLING".equals(transaction.getMerchantCategory())) {
            baseScore += 40;
        }

        // Normalize score
        int finalScore = Math.min(Math.max(baseScore, 0), 100);

        return RiskScore.builder()
            .transactionId(transaction.getId())
            .userId(transaction.getUserId())
            .riskScore(finalScore)
            .riskLevel(calculateRiskLevel(finalScore))
            .build();
    }

    private RiskLevel calculateRiskLevel(int score) {
        if (score < 30) return RiskLevel.LOW;
        if (score < 60) return RiskLevel.MEDIUM;
        if (score < 80) return RiskLevel.HIGH;
        return RiskLevel.VERY_HIGH;
    }

    // Enum for risk levels
    public enum RiskLevel {
        LOW, MEDIUM, HIGH, VERY_HIGH
    }

    // Risk Score model
    @lombok.Data
    @lombok.Builder
    public static class RiskScore {
        private java.util.UUID transactionId;
        private String userId;
        private int riskScore;
        private RiskLevel riskLevel;
    }
}