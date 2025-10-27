package com.nickfallico.financialriskmanagement.ml;

import com.nickfallico.financialriskmanagement.model.Transaction;
import org.springframework.stereotype.Component;

@Component
public class FraudDetectionModel {
    // Placeholder for ML model
    public double predictFraudProbability(Transaction transaction) {
        // Simple risk calculation based on transaction attributes
        double riskScore = 0.0;
        
        if (transaction.getAmount().doubleValue() > 10000) {
            riskScore += 0.5;
        }
        
        if (Boolean.TRUE.equals(transaction.getIsInternational())) {
            riskScore += 0.3;
        }
        
        return Math.min(riskScore, 1.0);
    }
}