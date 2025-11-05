package com.nickfallico.financialriskmanagement.ml;

import java.util.List;
import java.util.stream.IntStream;

import org.springframework.stereotype.Component;

import com.nickfallico.financialriskmanagement.exception.RiskAssessmentException;

@Component
public class ProbabilisticFraudModel {
    private static final double[] DEFAULT_WEIGHTS = {0.3, 0.2, 0.2, 0.15, 0.15};
    private static final double FRAUD_THRESHOLD = 0.6;

    public double calculateFraudProbability(List<Double> features) {
        // normalize to 5 features to match DEFAULT_WEIGHTS
        List<Double> f = new java.util.ArrayList<>(features);
        while (f.size() < DEFAULT_WEIGHTS.length) f.add(0.0);
        if (f.size() > DEFAULT_WEIGHTS.length) f = f.subList(0, DEFAULT_WEIGHTS.length);
        return calculateFraudProbability(f, DEFAULT_WEIGHTS);
    }

    public double calculateFraudProbability(List<Double> features, double[] weights) {
        if (features.size() != weights.length) {
            throw new RiskAssessmentException("Features and weights must have the same length");
        }
        return IntStream.range(0, features.size())
                .mapToDouble(i -> features.get(i) * weights[i])
                .sum();
    }

    public boolean isFraudulent(double fraudProbability) {
        return fraudProbability >= FRAUD_THRESHOLD;
    }

    public double[] adaptWeights(List<Double> features, boolean actualFraudStatus) {
        // assumes features.size() == DEFAULT_WEIGHTS.length
        return IntStream.range(0, features.size())
                .mapToDouble(i -> {
                    double currentWeight = DEFAULT_WEIGHTS[i];
                    double feature = features.get(i);
                    return actualFraudStatus ? currentWeight * (1 + feature)
                                             : currentWeight * (1 - feature);
                })
                .toArray();
    }
}
