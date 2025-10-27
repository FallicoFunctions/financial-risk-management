package com.nickfallico.financialriskmanagement.service;

import com.nickfallico.financialriskmanagement.model.Transaction;
import com.nickfallico.financialriskmanagement.model.UserRiskProfile;
import com.nickfallico.financialriskmanagement.repository.UserRiskProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Arrays;

@Service
@RequiredArgsConstructor
public class UserRiskProfileService {
    private final FraudDetectionService fraudDetectionService;
    private final UserRiskProfileRepository userRiskProfileRepository;

    public void updateUserRiskProfile(Transaction transaction) {
        // Retrieve or create user profile
        UserRiskProfile profile = userRiskProfileRepository.findById(transaction.getUserId())
            .orElse(createNewUserProfile(transaction.getUserId()));
        
        // Check if transaction is potentially fraudulent
        boolean isPotentialFraud = fraudDetectionService.isPotentialFraud(transaction, profile);
        
        if (isPotentialFraud) {
            profile.setHighRiskTransactions(profile.getHighRiskTransactions() + 1);
        }
        
        // Update Aggregated Metrics
        profile.setTotalTransactions(profile.getTotalTransactions() + 1);
        profile.setTotalTransactionValue(
            profile.getTotalTransactionValue() + transaction.getAmount().doubleValue()
        );
        
        // Update merchant category frequency
        updateMerchantCategoryFrequency(profile, transaction);
        
        // Calculate risk scores
        double behavioralRiskScore = calculateBehavioralRisk(profile, transaction);
        double transactionRiskScore = calculateTransactionRisk(transaction);
        
        // Combine Scores
        profile.setBehavioralRiskScore(behavioralRiskScore);
        profile.setTransactionRiskScore(transactionRiskScore);
        profile.setOverallRiskScore(
            (behavioralRiskScore + transactionRiskScore) / 2
        );
        
        // Save updated profile
        userRiskProfileRepository.save(profile);
    }

    public UserRiskProfile getUserProfile(String userId) {
        return userRiskProfileRepository.findById(userId)
            .orElse(createNewUserProfile(userId));
    }
    
    private UserRiskProfile createNewUserProfile(String userId) {
        UserRiskProfile profile = new UserRiskProfile();
        profile.setUserId(userId);
        profile.setFirstTransactionDate(Instant.now());
        return profile;
    }
    
    private void updateMerchantCategoryFrequency(UserRiskProfile profile, Transaction transaction) {
        String merchantCategory = transaction.getMerchantCategory();
        profile.getMerchantCategoryFrequency().merge(
            merchantCategory, 
            1, 
            Integer::sum
        );
    }
    
    private double calculateBehavioralRisk(UserRiskProfile profile, Transaction transaction) {
        double riskScore = 0.5; // Base neutral risk
    
        // Transaction frequency analysis
        int totalTransactions = profile.getTotalTransactions();
        if (totalTransactions < 10) {
            riskScore += 0.2; // New users have higher inherent risk
        }
    
        // Merchant category diversity
        int merchantCategoryCount = profile.getMerchantCategoryFrequency().size();
        if (merchantCategoryCount < 3) {
            riskScore += 0.1; // Limited merchant diversity increases risk
        }
    
        // Sudden change in transaction patterns
        double averageTransactionAmount = profile.getAverageTransactionAmount();
        double currentTransactionAmount = transaction.getAmount().doubleValue();
        if (Math.abs(currentTransactionAmount - averageTransactionAmount) > averageTransactionAmount * 0.5) {
            riskScore += 0.3; // Significant deviation from average
        }
    
        return Math.min(riskScore, 1.0);
    }
    
    private double calculateTransactionRisk(Transaction transaction) {
        double riskScore = 0.5;
    
        // High amount risk
        if (transaction.getAmount().doubleValue() > 10000) {
            riskScore += 0.3;
        }
    
        // International transaction risk
        if (Boolean.TRUE.equals(transaction.getIsInternational())) {
            riskScore += 0.2;
        }
    
        // Risky merchant categories
        String[] highRiskCategories = {"GAMBLING", "CRYPTO", "ADULT_ENTERTAINMENT"};
        if (Arrays.stream(highRiskCategories).anyMatch(category -> 
            category.equals(transaction.getMerchantCategory()))) {
            riskScore += 0.4;
        }
    
        return Math.min(riskScore, 1.0);
    }
}