package com.nickfallico.financialriskmanagement.service;

import com.nickfallico.financialriskmanagement.model.Transaction;
import com.nickfallico.financialriskmanagement.model.UserRiskProfile;
import com.nickfallico.financialriskmanagement.repository.UserRiskProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class UserRiskProfileService {
    private final UserRiskProfileRepository userRiskProfileRepository;

    public void updateUserRiskProfile(Transaction transaction) {
        // Retrieve or create user profile
        UserRiskProfile profile = userRiskProfileRepository.findById(transaction.getUserId())
            .orElse(createNewUserProfile(transaction.getUserId()));
        
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
        // Placeholder implementation
        // TODO: Implement more sophisticated risk calculation
        return 0.5; // Neutral risk
    }
    
    private double calculateTransactionRisk(Transaction transaction) {
        // Placeholder implementation
        // TODO: Implement more sophisticated risk calculation
        return 0.5; // Neutral risk
    }
}