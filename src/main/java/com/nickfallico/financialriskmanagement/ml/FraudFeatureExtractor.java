package com.nickfallico.financialriskmanagement.ml;

import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Component;
import com.nickfallico.financialriskmanagement.model.Transactions;
import com.nickfallico.financialriskmanagement.model.ImmutableUserRiskProfile;
import com.nickfallico.financialriskmanagement.model.MerchantCategoryFrequency;

/**
 * Extracts features for fraud detection from transaction and profile data.
 * Works with immutable profile and separate merchant frequency aggregate.
 */
@Component
public class FraudFeatureExtractor {
    
    /**
     * Extract normalized features for fraud model.
     * Takes both immutable profile and merchant frequencies.
     */
    public List<Double> extractFeatures(
        Transactions transaction,
        ImmutableUserRiskProfile profile,
        MerchantCategoryFrequency merchantFrequency) {
        
        return Arrays.asList(
            normalizeFeature(calculateTransactionAmountRisk(transaction, profile), 0, 1),
            normalizeFeature(calculateMerchantCategoryRisk(transaction, merchantFrequency), 0, 1),
            normalizeFeature(calculateTemporalRisk(transaction), 0, 1),
            normalizeFeature(calculateFrequencyRisk(transaction, profile), 0, 1),
            normalizeFeature(calculateInternationalRisk(transaction), 0, 1)
        );
    }
    
    private double normalizeFeature(double value, double min, double max) {
        return (value - min) / (max - min);
    }
    
    private double calculateTransactionAmountRisk(
        Transactions transaction,
        ImmutableUserRiskProfile profile) {
        
        double currentAmount = transaction.getAmount().doubleValue();
        double averageAmount = profile.getAverageTransactionAmount();
        
        if (averageAmount == 0) return 0.5;
        
        double amountDeviation = Math.abs(currentAmount - averageAmount) / averageAmount;
        return Math.min(amountDeviation * 10, 1.0);
    }
    
    /**
     * Calculate risk based on merchant category.
     * Uses MerchantCategoryFrequency to check if category is familiar to user.
     */
    private double calculateMerchantCategoryRisk(
        Transactions transaction,
        MerchantCategoryFrequency merchantFrequency) {
        
        String merchantCategory = transaction.getMerchantCategory();
        String[] highRiskCategories = {"GAMBLING", "CRYPTO", "ADULT_ENTERTAINMENT"};
        
        boolean isHighRiskCategory = Arrays.stream(highRiskCategories)
            .anyMatch(category -> category.equals(merchantCategory));
        
        if (isHighRiskCategory) return 1.0;
        
        // Check frequency from merchant aggregate (not from profile)
        if (merchantFrequency == null) return 0.7;
        
        int categoryFrequency = merchantFrequency.getFrequency(merchantCategory);
        return categoryFrequency < 3 ? 0.7 : 0.3;
    }
    
    private double calculateTemporalRisk(Transactions transaction) {
        int hour = transaction.getCreatedAt()
            .atZone(ZoneId.systemDefault())
            .getHour();
        
        // Odd hours: 0-4 AM, 23 PM
        if (hour < 5 || hour > 22) return 1.0;
        
        return 0.2;
    }
    
    private double calculateFrequencyRisk(
        Transactions transaction,
        ImmutableUserRiskProfile profile) {
        
        int totalTransactions = profile.getTotalTransactions();
        
        if (totalTransactions < 10) return 0.8;
        if (totalTransactions < 50) return 0.5;
        
        return 0.2;
    }
    
    private double calculateInternationalRisk(Transactions transaction) {
        return Boolean.TRUE.equals(transaction.getIsInternational()) ? 0.7 : 0.2;
    }
}