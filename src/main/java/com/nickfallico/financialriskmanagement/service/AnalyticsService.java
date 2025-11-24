package com.nickfallico.financialriskmanagement.service;

import org.springframework.stereotype.Service;

import com.nickfallico.financialriskmanagement.kafka.event.HighRiskUserIdentifiedEvent;
import com.nickfallico.financialriskmanagement.kafka.event.TransactionCreatedEvent;
import com.nickfallico.financialriskmanagement.kafka.event.UserProfileUpdatedEvent;
import com.nickfallico.financialriskmanagement.service.analytics.AnomalyDetectorService;
import com.nickfallico.financialriskmanagement.service.analytics.BehaviorAnalyticsService;
import com.nickfallico.financialriskmanagement.service.analytics.BusinessIntelligenceService;
import com.nickfallico.financialriskmanagement.service.analytics.FeatureStoreService;
import com.nickfallico.financialriskmanagement.service.analytics.MlModelTrainerService;
import com.nickfallico.financialriskmanagement.service.analytics.UserProfileCacheService;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * Service for processing transaction analytics and ML model training data.
 * Integrates with feature stores, behavior analytics, and BI systems.
 * Currently using mock implementations for demonstration.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AnalyticsService {

    private final MeterRegistry meterRegistry;
    private final FeatureStoreService featureStoreService;
    private final BehaviorAnalyticsService behaviorAnalyticsService;
    private final AnomalyDetectorService anomalyDetectorService;
    private final MlModelTrainerService mlModelTrainerService;
    private final UserProfileCacheService userProfileCacheService;
    private final BusinessIntelligenceService businessIntelligenceService;

    /**
     * Process transaction for analytics and ML training.
     */
    public Mono<Void> processTransactionAnalytics(TransactionCreatedEvent event) {
        // Record metrics for analytics
        recordTransactionMetrics(event);

        // Extract and store features
        String features = extractFeatures(event);

        // Build transaction data JSON
        String transactionData = buildTransactionDataJson(event);

        // Process in parallel: store features, update patterns, check anomalies
        return Mono.when(
            featureStoreService.storeTransactionFeatures(
                event.getTransactionId().toString(),
                event.getUserId(),
                features
            ),
            behaviorAnalyticsService.updateBehaviorPatterns(
                event.getUserId(),
                transactionData
            ),
            anomalyDetectorService.checkForAnomalies(
                event.getTransactionId().toString(),
                event.getUserId(),
                transactionData
            ).doOnNext(isAnomaly -> {
                if (isAnomaly) {
                    meterRegistry.counter("analytics.anomalies.detected",
                        "type", "spending_pattern"
                    ).increment();
                    log.info("Anomaly detected: userId={}, transactionId={}",
                        event.getUserId(), event.getTransactionId());
                }
            })
        ).then(
            featureStoreService.updateUserFeatures(event.getUserId(), features)
        ).doOnSuccess(v ->
            log.debug("Analytics processed for transaction: {}", event.getTransactionId())
        );
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
    private String extractFeatures(TransactionCreatedEvent event) {
        meterRegistry.counter("analytics.ml_features_extracted").increment();

        // Build feature JSON (simplified for demo)
        return String.format("""
            {"transactionId":"%s","userId":"%s","amount":%s,"currency":"%s",\
            "merchantCategory":"%s","isInternational":%b,"timestamp":"%s",\
            "hour":%d,"dayOfWeek":%d}
            """,
            event.getTransactionId(),
            event.getUserId(),
            event.getAmount(),
            event.getCurrency(),
            event.getMerchantCategory(),
            event.getIsInternational(),
            event.getEventTimestamp(),
            event.getEventTimestamp().atZone(java.time.ZoneId.systemDefault()).getHour(),
            event.getEventTimestamp().atZone(java.time.ZoneId.systemDefault()).getDayOfWeek().getValue()
        );
    }

    /**
     * Build transaction data JSON for analytics.
     */
    private String buildTransactionDataJson(TransactionCreatedEvent event) {
        return String.format("""
            {"transactionId":"%s","amount":%s,"currency":"%s","merchantCategory":"%s",\
            "merchantName":"%s","isInternational":%b,"timestamp":"%s"}
            """,
            event.getTransactionId(),
            event.getAmount(),
            event.getCurrency(),
            event.getMerchantCategory(),
            event.getMerchantName(),
            event.getIsInternational(),
            event.getEventTimestamp()
        );
    }

    /**
     * Process high-risk user analytics for ML training and pattern detection.
     */
    public Mono<Void> processHighRiskUserAnalytics(HighRiskUserIdentifiedEvent event) {
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

        // Build high-risk patterns JSON
        String patterns = buildHighRiskPatternsJson(event);

        // Process in parallel: update ML patterns, analyze behavior, update thresholds
        return Mono.when(
            mlModelTrainerService.updateHighRiskPatterns(event.getUserId(), patterns),
            behaviorAnalyticsService.analyzeHighRiskUser(
                event.getUserId(),
                event.getOverallRiskScore(),
                event.getRiskFactors().toString()
            ),
            anomalyDetectorService.updateRiskThresholds(
                event.getUserId(),
                event.getOverallRiskScore()
            )
        );
    }

    /**
     * Build high-risk patterns JSON for ML training.
     */
    private String buildHighRiskPatternsJson(HighRiskUserIdentifiedEvent event) {
        return String.format("""
            {"userId":"%s","riskScore":%s,"riskFactors":%s,\
            "totalTransactions":%d,"highRiskTransactions":%d,\
            "internationalTransactions":%d,"totalValue":%s}
            """,
            event.getUserId(),
            event.getOverallRiskScore(),
            event.getRiskFactors(),
            event.getTotalTransactions(),
            event.getHighRiskTransactions(),
            event.getInternationalTransactions(),
            event.getTotalTransactionValue()
        );
    }

    /**
     * Process user profile update analytics for business intelligence.
     */
    public Mono<Void> processUserProfileUpdateAnalytics(UserProfileUpdatedEvent event) {
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

        // Build profile data JSON
        String profileData = buildProfileDataJson(event);

        // Process in parallel: invalidate cache, update dashboard, check retraining, feed BI
        Mono<Void> cacheAndDashboard = Mono.when(
            userProfileCacheService.invalidateUserProfile(event.getUserId()),
            businessIntelligenceService.updateDashboardMetrics(
                event.getUserId(),
                event.getNewOverallRiskScore()
            )
        );

        Mono<Void> mlAndBi = mlModelTrainerService.checkForRetrainingTrigger(
            event.getUserId(),
            event.getPreviousOverallRiskScore(),
            event.getNewOverallRiskScore()
        ).flatMap(shouldRetrain -> {
            if (shouldRetrain) {
                log.info("ML model retraining triggered for user: {}", event.getUserId());
            }
            return businessIntelligenceService.feedProfileUpdate(event.getUserId(), profileData);
        });

        return Mono.when(cacheAndDashboard, mlAndBi);
    }

    /**
     * Build profile data JSON for BI system.
     */
    private String buildProfileDataJson(UserProfileUpdatedEvent event) {
        return String.format("""
            {"userId":"%s","previousRiskScore":%s,"newRiskScore":%s,\
            "totalTransactions":%d,"highRiskTransactions":%d,\
            "totalValue":%s,"updateReason":"%s","timestamp":"%s"}
            """,
            event.getUserId(),
            event.getPreviousOverallRiskScore(),
            event.getNewOverallRiskScore(),
            event.getTotalTransactions(),
            event.getHighRiskTransactions(),
            event.getTotalTransactionValue(),
            event.getUpdateReason(),
            event.getEventTimestamp()
        );
    }
}
