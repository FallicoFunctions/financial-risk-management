package com.nickfallico.financialriskmanagement.service;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;

import com.nickfallico.financialriskmanagement.kafka.event.TransactionCreatedEvent;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * Service for processing transaction analytics and ML model training data.
 * Collects features and patterns for:
 * - ML model training and retraining
 * - User behavior analytics
 * - Fraud pattern detection
 * - Business intelligence
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AnalyticsService {
    
    private final MeterRegistry meterRegistry;
    
    /**
     * Process transaction for analytics and ML training.
     */
    public Mono<Void> processTransactionAnalytics(TransactionCreatedEvent event) {
        return Mono.fromRunnable(() -> {
            // Record metrics for analytics
            recordTransactionMetrics(event);
            
            // Extract features for ML model training
            extractMLFeatures(event);
            
            // Update user behavior patterns
            updateBehaviorPatterns(event);
            
            log.debug("Analytics processed for transaction: {}", event.getTransactionId());
        });
    }
    
    /**
     * Record transaction metrics for business intelligence.
     */
    private void recordTransactionMetrics(TransactionCreatedEvent event) {
        // Transaction volume by merchant category
        meterRegistry.counter("analytics.transactions.by_merchant_category",
            "category", event.getMerchantCategory()
        ).increment();
        
        // Transaction value distribution
        meterRegistry.summary("analytics.transaction.amount",
            "currency", event.getCurrency()
        ).record(event.getAmount().doubleValue());
        
        // International vs domestic transactions
        meterRegistry.counter("analytics.transactions.by_location",
            "international", String.valueOf(event.getIsInternational())
        ).increment();
        
        // Hourly transaction patterns
        int hour = java.time.Instant.now().atZone(java.time.ZoneId.systemDefault()).getHour();
        meterRegistry.counter("analytics.transactions.by_hour",
            "hour", String.valueOf(hour)
        ).increment();
        
        log.trace("Transaction metrics recorded: transactionId={}, amount={}, category={}",
            event.getTransactionId(),
            event.getAmount(),
            event.getMerchantCategory()
        );
    }
    
    /**
     * Extract features for ML model training.
     */
    private void extractMLFeatures(TransactionCreatedEvent event) {
        // In production, this would:
        // 1. Extract transaction features
        // 2. Store in training data warehouse
        // 3. Trigger batch ML model retraining
        
        meterRegistry.counter("analytics.ml_features_extracted").increment();
        
        log.trace("ML features extracted for transaction: transactionId={}, userId={}",
            event.getTransactionId(),
            event.getUserId()
        );
        
        // TODO: Production implementation
        // mlDataWarehouse.storeTrainingData(extractFeatures(event));
        // featureStore.updateUserFeatures(event.getUserId(), features);
    }
    
    /**
     * Update user behavior patterns for fraud detection.
     */
    private void updateBehaviorPatterns(TransactionCreatedEvent event) {
        // Track user spending patterns
        meterRegistry.counter("analytics.user_patterns.updated",
            "user_id", event.getUserId()
        ).increment();
        
        // Detect anomalies in user behavior
        if (isAnomalousTransaction(event)) {
            meterRegistry.counter("analytics.anomalies.detected",
                "type", "spending_pattern"
            ).increment();
            
            log.info("Anomalous spending pattern detected: userId={}, amount={}, category={}",
                event.getUserId(),
                event.getAmount(),
                event.getMerchantCategory()
            );
        }
        
        // TODO: Production implementation
        // behaviorAnalyticsEngine.updatePatterns(event);
        // anomalyDetector.checkForAnomalies(event);
    }
    
    /**
     * Check if transaction is anomalous based on historical patterns.
     */
    private boolean isAnomalousTransaction(TransactionCreatedEvent event) {
        // Simplified anomaly detection
        // In production, this would use more sophisticated algorithms
        
        BigDecimal largeAmountThreshold = new BigDecimal("10000");
        boolean isLargeAmount = event.getAmount().compareTo(largeAmountThreshold) > 0;
        
        // Flag large international transactions as potentially anomalous
        return isLargeAmount && Boolean.TRUE.equals(event.getIsInternational());
    }
}