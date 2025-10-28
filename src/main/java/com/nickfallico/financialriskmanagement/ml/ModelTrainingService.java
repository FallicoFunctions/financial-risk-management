package com.nickfallico.financialriskmanagement.ml;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class ModelTrainingService {
    private final ProbabilisticFraudModel fraudModel;
    
    private List<TrainingExample> trainingData = new ArrayList<>();
    private double[] currentWeights = {0.3, 0.2, 0.2, 0.15, 0.15};

    public void addTrainingExample(List<Double> features, boolean actualFraudStatus) {
        trainingData.add(new TrainingExample(features, actualFraudStatus));
        
        // Periodically retrain model
        if (trainingData.size() % 100 == 0) {
            retrainModel();
        }
    }

    private void retrainModel() {
        log.info("Retraining model with {} examples", trainingData.size());
        
        for (TrainingExample example : trainingData) {
            currentWeights = fraudModel.adaptWeights(example.features, example.actualFraudStatus);
        }
        
        log.info("Model retrained. New weights: {}", currentWeights);
    }

    public double[] getCurrentWeights() {
        return currentWeights.clone();
    }

    private static class TrainingExample {
        List<Double> features;
        boolean actualFraudStatus;

        TrainingExample(List<Double> features, boolean actualFraudStatus) {
            this.features = features;
            this.actualFraudStatus = actualFraudStatus;
        }
    }
}