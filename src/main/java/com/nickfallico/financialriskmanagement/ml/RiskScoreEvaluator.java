package com.nickfallico.financialriskmanagement.ml;

import java.util.List;

import org.springframework.stereotype.Component;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class RiskScoreEvaluator {
    private final ProbabilisticFraudModel fraudModel;

    @Data
    public static class ModelPerformanceMetrics {
        private double precision = 0.0;
        private double recall = 0.0;
        private double f1Score = 0.0;
        private int truePositives = 0;
        private int falsePositives = 0;
        private int falseNegatives = 0;
    }

    public ModelPerformanceMetrics evaluateModelPerformance(List<ModelEvaluationExample> evaluationData) {
        ModelPerformanceMetrics metrics = new ModelPerformanceMetrics();

        for (ModelEvaluationExample example : evaluationData) {
            boolean predicted = fraudModel.calculateFraudProbability(example.features, example.weights) > 0.7;
            
            if (predicted && example.actualFraudStatus) {
                metrics.truePositives++;
            } else if (predicted && !example.actualFraudStatus) {
                metrics.falsePositives++;
            } else if (!predicted && example.actualFraudStatus) {
                metrics.falseNegatives++;
            }
        }

        // Safe calculations with divide-by-zero protection
        metrics.precision = metrics.truePositives == 0 ? 0.0 : 
            metrics.truePositives / (double)(metrics.truePositives + metrics.falsePositives);
        
        metrics.recall = metrics.truePositives == 0 ? 0.0 : 
            metrics.truePositives / (double)(metrics.truePositives + metrics.falseNegatives);
        
        metrics.f1Score = (metrics.precision + metrics.recall) == 0 ? 0.0 :
            2 * (metrics.precision * metrics.recall) / (metrics.precision + metrics.recall);

        logModelPerformance(metrics);
        return metrics;
    }

    private void logModelPerformance(ModelPerformanceMetrics metrics) {
        log.info("Model Performance Metrics:");
        log.info("Precision: {}", metrics.precision);
        log.info("Recall: {}", metrics.recall);
        log.info("F1 Score: {}", metrics.f1Score);
        log.info("True Positives: {}", metrics.truePositives);
        log.info("False Positives: {}", metrics.falsePositives);
        log.info("False Negatives: {}", metrics.falseNegatives);
    }

    @Data
    public static class ModelEvaluationExample {
        List<Double> features;
        double[] weights;
        boolean actualFraudStatus;
    }
}