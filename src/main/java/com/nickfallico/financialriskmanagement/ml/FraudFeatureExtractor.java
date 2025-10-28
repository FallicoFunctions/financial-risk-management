package com.nickfallico.financialriskmanagement.ml;

import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Component;

import com.nickfallico.financialriskmanagement.model.Transaction;
import com.nickfallico.financialriskmanagement.model.UserRiskProfile;

@Component
public class FraudFeatureExtractor {

    public List<Double> extractFeatures(Transaction transaction, UserRiskProfile profile) {
        return Arrays.asList(
            normalizeFeature(calculateTransactionAmountRisk(transaction, profile), 0, 10000),
            normalizeFeature(calculateMerchantCategoryRisk(transaction, profile), 0, 1),
            normalizeFeature(calculateTemporalRisk(transaction), 0, 1),
            normalizeFeature(calculateFrequencyRisk(transaction, profile), 0, 1),
            normalizeFeature(calculateInternationalRisk(transaction), 0, 1)
        );
    }

    private double normalizeFeature(double value, double min, double max) {
        return (value - min) / (max - min);
    }

    private double calculateTransactionAmountRisk(Transaction transaction, UserRiskProfile profile) {
        double currentAmount = transaction.getAmount().doubleValue();
        double averageAmount = profile.getAverageTransactionAmount();
        
        if (averageAmount == 0) return 0.5;
        
        double amountDeviation = Math.abs(currentAmount - averageAmount) / averageAmount;
        return Math.min(amountDeviation * 10, 1.0);
    }

    private double calculateMerchantCategoryRisk(Transaction transaction, UserRiskProfile profile) {
        String merchantCategory = transaction.getMerchantCategory();
        
        String[] highRiskCategories = {"GAMBLING", "CRYPTO", "ADULT_ENTERTAINMENT"};
        
        boolean isHighRiskCategory = Arrays.stream(highRiskCategories)
            .anyMatch(category -> category.equals(merchantCategory));
        
        if (isHighRiskCategory) return 1.0;
        
        int categoryFrequency = profile.getMerchantCategoryFrequency()
            .getOrDefault(merchantCategory, 0);
        
        return categoryFrequency < 3 ? 0.7 : 0.3;
    }

    private double calculateTemporalRisk(Transaction transaction) {
        int hour = transaction.getCreatedAt()
            .atZone(ZoneId.systemDefault())
            .getHour();
        
        if (hour < 5 || hour > 22) return 1.0;
        
        return 0.2;
    }

    private double calculateFrequencyRisk(Transaction transaction, UserRiskProfile profile) {
        int totalTransactions = profile.getTotalTransactions();
        
        if (totalTransactions < 10) return 0.8;
        if (totalTransactions < 50) return 0.5;
        
        return 0.2;
    }

    private double calculateInternationalRisk(Transaction transaction) {
        return Boolean.TRUE.equals(transaction.getIsInternational()) ? 0.7 : 0.2;
    }
}