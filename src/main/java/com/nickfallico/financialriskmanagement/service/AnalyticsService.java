package com.nickfallico.financialriskmanagement.service;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;

import com.nickfallico.financialriskmanagement.kafka.event.HighRiskUserIdentifiedEvent;
import com.nickfallico.financialriskmanagement.kafka.event.TransactionCreatedEvent;
import com.nickfallico.financialriskmanagement.kafka.event.UserProfileUpdatedEvent;

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

    /**
     * Process high-risk user analytics for ML training and pattern detection.
     */
    public Mono<Void> processHighRiskUserAnalytics(HighRiskUserIdentifiedEvent event) {
        return Mono.fromRunnable(() -> {
            // Record high-risk user metrics
            meterRegistry.counter("analytics.high_risk_users.identified",
                "severity", event.getAlertSeverity(),
                "recommended_action", event.getRecommendedAction()
            ).increment();

            // Track risk score distribution
            meterRegistry.summary("analytics.user_risk_score.distribution",
                "severity", event.getAlertSeverity()
            ).record(event.getOverallRiskScore());

            // Track high-risk transaction ratio
            if (event.getTotalTransactions() != null && event.getTotalTransactions() > 0) {
                double highRiskRatio = (double) event.getHighRiskTransactions() / event.getTotalTransactions();
                meterRegistry.gauge("analytics.user.high_risk_transaction_ratio",
                    highRiskRatio
                );
            }

            // Track international transaction patterns
            meterRegistry.counter("analytics.high_risk_users.international_activity",
                "has_international", String.valueOf(event.getInternationalTransactions() > 0)
            ).increment();

            // Log risk factors for pattern analysis
            if (event.getRiskFactors() != null && !event.getRiskFactors().isEmpty()) {
                event.getRiskFactors().forEach(factor ->
                    meterRegistry.counter("analytics.risk_factors",
                        "factor", factor
                    ).increment()
                );
            }

            log.debug("High-risk user analytics processed: userId={}, riskScore={}, factors={}",
                event.getUserId(),
                event.getOverallRiskScore(),
                event.getRiskFactors()
            );

            // TODO: Production implementation
            // mlModelTrainer.updateHighRiskPatterns(event);
            // behaviorAnalyticsEngine.analyzeHighRiskUser(event);
            // anomalyDetector.updateRiskThresholds(event);
        });
    }

    /**
     * Process user profile update analytics for business intelligence.
     */
    public Mono<Void> processUserProfileUpdateAnalytics(UserProfileUpdatedEvent event) {
        return Mono.fromRunnable(() -> {
            // Record profile update metrics
            meterRegistry.counter("analytics.user_profile.updates",
                "update_reason", event.getUpdateReason()
            ).increment();

            // Track risk score changes
            if (event.getPreviousOverallRiskScore() != null && event.getNewOverallRiskScore() != null) {
                double riskChange = event.getNewOverallRiskScore() - event.getPreviousOverallRiskScore();

                meterRegistry.summary("analytics.user_risk_score.change",
                    "update_reason", event.getUpdateReason()
                ).record(riskChange);

                // Track significant risk increases
                if (riskChange > 0.2) {
                    meterRegistry.counter("analytics.user_risk_score.significant_increase").increment();
                } else if (riskChange < -0.2) {
                    meterRegistry.counter("analytics.user_risk_score.significant_decrease").increment();
                }
            }

            // Track user transaction growth
            if (event.getTotalTransactions() != null) {
                meterRegistry.gauge("analytics.user.total_transactions",
                    event.getTotalTransactions()
                );
            }

            // Track high-risk transaction patterns
            if (event.getHighRiskTransactions() != null) {
                meterRegistry.gauge("analytics.user.high_risk_transactions",
                    event.getHighRiskTransactions()
                );
            }

            log.debug("User profile update analytics processed: userId={}, previousScore={}, newScore={}, reason={}",
                event.getUserId(),
                event.getPreviousOverallRiskScore(),
                event.getNewOverallRiskScore(),
                event.getUpdateReason()
            );

            // TODO: Production implementation
            // userProfileCache.invalidate(event.getUserId());
            // dashboardService.updateUserRiskMetrics(event);
            // mlModelTrainer.checkForRetrainingTrigger(event);
            // businessIntelligenceSystem.feedProfileUpdate(event);
        });
    }
}