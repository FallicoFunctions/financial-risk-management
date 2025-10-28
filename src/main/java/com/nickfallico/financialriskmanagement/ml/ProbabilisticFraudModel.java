package com.nickfallico.financialriskmanagement.ml;

import java.util.List;
import java.util.stream.IntStream;

import org.springframework.stereotype.Component;

import com.nickfallico.financialriskmanagement.exception.RiskAssessmentException;

@Component
public class ProbabilisticFraudModel {
    private static final double[] DEFAULT_WEIGHTS = {0.3, 0.2, 0.2, 0.15, 0.15};
    private static final double FRAUD_THRESHOLD = 0.7;

    public double calculateFraudProbability(List<Double> features, double[] weights) {
        if (features.size() != weights.length) {
            throw new RiskAssessmentException("Features and weights must have the same length");
        }

        return IntStream.range(0, features.size())
            .mapToDouble(i -> features.get(i) * weights[i])
            .sum();
    }

    public boolean isFraudulent(double fraudProbability) {
        return fraudProbability > FRAUD_THRESHOLD;
    }

    public double[] adaptWeights(List<Double> features, boolean actualFraudStatus) {
        return IntStream.range(0, features.size())
            .mapToDouble(i -> {
                double currentWeight = DEFAULT_WEIGHTS[i];
                double feature = features.get(i);
                
                return actualFraudStatus 
                    ? currentWeight * (1 + feature)  // Increase weight for fraud detection
                    : currentWeight * (1 - feature); // Decrease weight for false positive
            })
            .toArray();
    }
}