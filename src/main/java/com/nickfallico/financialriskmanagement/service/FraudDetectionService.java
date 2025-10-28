package com.nickfallico.financialriskmanagement.service;

import com.nickfallico.financialriskmanagement.ml.FraudFeatureExtractor;
import com.nickfallico.financialriskmanagement.model.Transaction;
import com.nickfallico.financialriskmanagement.model.UserRiskProfile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class FraudDetectionService {
    private final FraudFeatureExtractor fraudFeatureExtractor;
    private static final double FRAUD_THRESHOLD = 0.7;

    public boolean isPotentialFraud(Transaction transaction, UserRiskProfile profile) {
        List<Double> features = fraudFeatureExtractor.extractFeatures(transaction, profile);
        double fraudProbability = calculateFraudProbability(features);
        
        boolean isFraudulent = fraudProbability > FRAUD_THRESHOLD;
        
        logFraudDetectionResult(transaction, fraudProbability, isFraudulent);
        
        return isFraudulent;
    }

    private double calculateFraudProbability(List<Double> features) {
        double[] weights = {0.3, 0.2, 0.2, 0.15, 0.15};
        
        double weightedRiskScore = 0.0;
        for (int i = 0; i < features.size(); i++) {
            weightedRiskScore += features.get(i) * weights[i];
        }
        
        return Math.min(weightedRiskScore, 1.0);
    }

    private void logFraudDetectionResult(Transaction transaction, double fraudProbability, boolean isFraudulent) {
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
    }
}