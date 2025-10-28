package com.nickfallico.financialriskmanagement.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.nickfallico.financialriskmanagement.ml.FraudFeatureExtractor;
import com.nickfallico.financialriskmanagement.ml.ProbabilisticFraudModel;
import com.nickfallico.financialriskmanagement.model.Transaction;
import com.nickfallico.financialriskmanagement.model.UserRiskProfile;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Service
@Slf4j
@RequiredArgsConstructor
public class FraudDetectionService {
    private final FraudFeatureExtractor fraudFeatureExtractor;
    private final ProbabilisticFraudModel fraudModel;
    private final MeterRegistry meterRegistry;

    public Mono<Boolean> isPotentialFraud(Transaction transaction, UserRiskProfile profile) {
        return Mono.fromCallable(() -> {
            Timer.Sample sample = Timer.start(meterRegistry);
            
            List<Double> features = fraudFeatureExtractor.extractFeatures(transaction, profile);
            double fraudProbability = fraudModel.calculateFraudProbability(features, new double[]{0.3, 0.2, 0.2, 0.15, 0.15});
            
            boolean isFraudulent = fraudModel.isFraudulent(fraudProbability);
            
            // Record timer
            sample.stop(meterRegistry.timer("fraud_detection_time", 
                "merchant_category", transaction.getMerchantCategory(),
                "is_fraudulent", String.valueOf(isFraudulent)
            ));
            
            // Count fraud occurrences
            meterRegistry.counter("fraud_detection_attempts", 
                "merchant_category", transaction.getMerchantCategory(),
                "is_fraudulent", String.valueOf(isFraudulent)
            ).increment();
            
            logFraudDetectionResult(transaction, fraudProbability, isFraudulent);
            
            return isFraudulent;
        });
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