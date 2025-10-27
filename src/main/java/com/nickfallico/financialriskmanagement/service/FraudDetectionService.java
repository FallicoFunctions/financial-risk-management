package com.nickfallico.financialriskmanagement.service;

import com.nickfallico.financialriskmanagement.model.Transaction;
import com.nickfallico.financialriskmanagement.model.UserRiskProfile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class FraudDetectionService {
    private static final double FRAUD_THRESHOLD = 0.7;
    private static final double HIGH_AMOUNT_THRESHOLD = 10000.0;
    private static final int INTERNATIONAL_RISK_MULTIPLIER = 2;

    public boolean isPotentialFraud(Transaction transaction, UserRiskProfile profile) {
        List<Double> features = extractFeatures(transaction, profile);
        double fraudProbability = calculateFraudProbability(features);
        
        boolean isFraudulent = fraudProbability > FRAUD_THRESHOLD;
        
        if (isFraudulent) {
            log.warn("Potential Fraud Detected - Transaction Details: [ID: {}, Amount: {}, Merchant: {}, Fraud Probability: {}]", 
                transaction.getId(), 
                transaction.getAmount(),
                transaction.getMerchantCategory(),
                fraudProbability
            );
        } else {
            log.info("Transaction Approved - Transaction Details: [ID: {}, Amount: {}, Merchant: {}]", 
                transaction.getId(), 
                transaction.getAmount(),
                transaction.getMerchantCategory()
            );
        }
        
        return isFraudulent;
    }

    private List<Double> extractFeatures(Transaction transaction, UserRiskProfile profile) {
        return Arrays.asList(
            // Transaction amount risk
            calculateAmountRisk(transaction.getAmount()),
            
            // International transaction risk
            calculateInternationalRisk(transaction),
            
            // Merchant category risk
            calculateMerchantCategoryRisk(transaction, profile),
            
            // Frequency of similar transactions
            calculateTransactionFrequencyRisk(transaction, profile),
            
            // Time of transaction risk
            calculateTimeOfDayRisk(transaction)
        );
    }

    private double calculateAmountRisk(BigDecimal amount) {
        double amountValue = amount.doubleValue();
        if (amountValue > HIGH_AMOUNT_THRESHOLD) {
            return 1.0; // High risk for large transactions
        }
        return amountValue / HIGH_AMOUNT_THRESHOLD;
    }

    private double calculateInternationalRisk(Transaction transaction) {
        return Boolean.TRUE.equals(transaction.getIsInternational()) 
            ? INTERNATIONAL_RISK_MULTIPLIER 
            : 1.0;
    }

    private double calculateMerchantCategoryRisk(Transaction transaction, UserRiskProfile profile) {
        String merchantCategory = transaction.getMerchantCategory();
        // If the profile doesn't have this merchant category, it's riskier
        if (!profile.getMerchantCategoryFrequency().containsKey(merchantCategory)) {
            return 1.5;
        }
        
        // Lower risk for frequently used merchant categories
        int frequency = profile.getMerchantCategoryFrequency().get(merchantCategory);
        return frequency > 5 ? 0.5 : 1.0;
    }

    private double calculateTransactionFrequencyRisk(Transaction transaction, UserRiskProfile profile) {
        // Check how frequently similar transactions occur
        int totalTransactions = profile.getTotalTransactions();
        return totalTransactions < 10 ? 1.5 : 1.0;
    }

    private double calculateTimeOfDayRisk(Transaction transaction) {
        int hour = transaction.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).getHour();
        
        // Higher risk for transactions during unusual hours
        if (hour < 6 || hour > 22) {
            return 1.5;
        }
        return 1.0;
    }

    private double calculateFraudProbability(List<Double> features) {
        // Weighted average of risk features
        double[] weights = {0.3, 0.2, 0.2, 0.15, 0.15};
        
        double weightedRiskScore = 0.0;
        for (int i = 0; i < features.size(); i++) {
            weightedRiskScore += features.get(i) * weights[i];
        }
        
        return Math.min(weightedRiskScore, 1.0);
    }
}