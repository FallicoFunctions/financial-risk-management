package com.nickfallico.financialriskmanagement.ml;

import com.nickfallico.financialriskmanagement.model.Transactions;
import com.nickfallico.financialriskmanagement.model.ImmutableUserRiskProfile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Component
public class FraudDetectionModel {
    // Configurable risk thresholds
    private static final Map<String, Double> MERCHANT_CATEGORY_RISK_MULTIPLIERS = new HashMap<>() {{
        put("GAMBLING", 1.5);
        put("CRYPTO", 1.4);
        put("ADULT_ENTERTAINMENT", 1.3);
        put("ONLINE_SHOPPING", 1.1);
    }};

    private static final BigDecimal HIGH_RISK_TRANSACTION_THRESHOLD = BigDecimal.valueOf(10000);
    private static final int HIGH_FREQUENCY_THRESHOLD = 50;
    private static final int VERY_HIGH_FREQUENCY_THRESHOLD = 200;

    public double predictFraudProbability(Transactions transaction, ImmutableUserRiskProfile profile) {
        double baseRiskScore = calculateBaseRiskScore(transaction, profile);
        double categoryRiskMultiplier = calculateCategoryRiskMultiplier(transaction);
        double temporalRiskFactor = calculateTemporalRiskFactor(transaction, profile);
        double frequencyRiskFactor = calculateFrequencyRiskFactor(profile);

        double finalRiskScore = baseRiskScore 
            * categoryRiskMultiplier 
            * temporalRiskFactor 
            * frequencyRiskFactor;

        return Math.min(Math.max(finalRiskScore, 0.0), 1.0);
    }

    private double calculateBaseRiskScore(Transactions transaction, ImmutableUserRiskProfile profile) {
        double amountRisk = calculateAmountRisk(transaction);
        double internationalRisk = calculateInternationalRisk(transaction);
        double averageAmountDeviation = calculateAverageAmountDeviation(transaction, profile);

        return (amountRisk + internationalRisk + averageAmountDeviation) / 3;
    }

    private double calculateAmountRisk(Transactions transaction) {
        if (transaction.getAmount().compareTo(HIGH_RISK_TRANSACTION_THRESHOLD) > 0) {
            return 1.0;
        }
        return transaction.getAmount().doubleValue() / HIGH_RISK_TRANSACTION_THRESHOLD.doubleValue();
    }

    private double calculateInternationalRisk(Transactions transaction) {
        return Boolean.TRUE.equals(transaction.getIsInternational()) ? 0.7 : 0.2;
    }

    private double calculateAverageAmountDeviation(Transactions transaction, ImmutableUserRiskProfile profile) {
        if (profile.getAverageTransactionAmount() == 0) return 0.5;
        
        double currentAmount = transaction.getAmount().doubleValue();
        double averageAmount = profile.getAverageTransactionAmount();
        
        double deviation = Math.abs(currentAmount - averageAmount) / averageAmount;
        return Math.min(deviation, 1.0);
    }

    private double calculateCategoryRiskMultiplier(Transactions transaction) {
        return MERCHANT_CATEGORY_RISK_MULTIPLIERS.getOrDefault(
            transaction.getMerchantCategory(), 1.0
        );
    }

    private double calculateTemporalRiskFactor(Transactions transaction, ImmutableUserRiskProfile profile) {
        Instant lastTransactionTime = profile.getLastTransactionDate();
        if (lastTransactionTime == null) return 1.0;

        Duration timeSinceLastTransaction = Duration.between(lastTransactionTime, transaction.getCreatedAt());
        
        // Shorter time between transactions increases risk
        if (timeSinceLastTransaction.toHours() < 1) return 1.3;
        if (timeSinceLastTransaction.toHours() < 6) return 1.1;
        
        return 1.0;
    }

    private double calculateFrequencyRiskFactor(ImmutableUserRiskProfile profile) {
        int totalTransactions = profile.getTotalTransactions();
        
        if (totalTransactions > VERY_HIGH_FREQUENCY_THRESHOLD) return 0.5;
        if (totalTransactions > HIGH_FREQUENCY_THRESHOLD) return 0.7;
        if (totalTransactions < 10) return 1.3;
        
        return 1.0;
    }
}