package com.nickfallico.financialriskmanagement.ml;

import com.nickfallico.financialriskmanagement.model.Transaction;
import com.nickfallico.financialriskmanagement.model.UserRiskProfile;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;

@Component
public class FraudFeatureExtractor {

    public List<Double> extractFeatures(Transaction transaction, UserRiskProfile profile) {
        return Arrays.asList(
            calculateTransactionAmountRisk(transaction, profile),
            calculateMerchantCategoryRisk(transaction, profile),
            calculateTemporalRisk(transaction),
            calculateFrequencyRisk(transaction, profile),
            calculateInternationalRisk(transaction)
        );
    }

    private double calculateTransactionAmountRisk(Transaction transaction, UserRiskProfile profile) {
        double currentAmount = transaction.getAmount().doubleValue();
        double averageAmount = profile.getAverageTransactionAmount();
        
        if (averageAmount == 0) return 0.5; // Neutral risk for new users
        
        double amountDeviation = Math.abs(currentAmount - averageAmount) / averageAmount;
        return Math.min(amountDeviation, 1.0);
    }

    private double calculateMerchantCategoryRisk(Transaction transaction, UserRiskProfile profile) {
        String merchantCategory = transaction.getMerchantCategory();
        
        // High-risk categories
        String[] highRiskCategories = {"GAMBLING", "CRYPTO", "ADULT_ENTERTAINMENT"};
        
        // Check if in high-risk categories
        boolean isHighRiskCategory = Arrays.stream(highRiskCategories)
            .anyMatch(category -> category.equals(merchantCategory));
        
        if (isHighRiskCategory) return 1.0;
        
        // Check frequency of this merchant category
        int categoryFrequency = profile.getMerchantCategoryFrequency()
            .getOrDefault(merchantCategory, 0);
        
        return categoryFrequency < 3 ? 0.7 : 0.3;
    }

    private double calculateTemporalRisk(Transaction transaction) {
        int hour = transaction.getCreatedAt()
            .atZone(ZoneId.systemDefault())
            .getHour();
        
        // Higher risk for late-night or very early morning transactions
        if (hour < 5 || hour > 22) return 1.0;
        
        return 0.2;
    }

    private double calculateFrequencyRisk(Transaction transaction, UserRiskProfile profile) {
        int totalTransactions = profile.getTotalTransactions();
        
        // Higher risk for users with few transactions
        if (totalTransactions < 10) return 0.8;
        if (totalTransactions < 50) return 0.5;
        
        return 0.2;
    }

    private double calculateInternationalRisk(Transaction transaction) {
        return Boolean.TRUE.equals(transaction.getIsInternational()) ? 0.7 : 0.2;
    }
}