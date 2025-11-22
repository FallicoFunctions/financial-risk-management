package com.nickfallico.financialriskmanagement.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Service for tracking custom business metrics for Prometheus.
 * Provides metrics for fraud detection, transactions, and risk assessment.
 */
@Service
public class MetricsService {

    private final MeterRegistry meterRegistry;

    // Fraud Detection Metrics
    private final Counter fraudDetectedCounter;
    private final Counter transactionsProcessedCounter;
    private final Counter transactionsBlockedCounter;
    private final Counter transactionsApprovedCounter;

    // Risk Score Metrics
    private final DistributionSummary riskScoreDistribution;
    private final AtomicLong currentAverageRiskScore;

    // Performance Metrics
    private final Timer fraudDetectionDuration;
    private final Timer transactionProcessingDuration;

    // Business Metrics
    private final AtomicLong transactionsProcessedLastHour;
    private final AtomicLong transactionsBlockedLastHour;

    public MetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        // Initialize fraud detection counters
        this.fraudDetectedCounter = Counter.builder("fraud.detected.total")
            .description("Total number of fraudulent transactions detected")
            .tag("component", "fraud-detection")
            .register(meterRegistry);

        this.transactionsProcessedCounter = Counter.builder("transactions.processed.total")
            .description("Total number of transactions processed")
            .tag("component", "transaction-service")
            .register(meterRegistry);

        this.transactionsBlockedCounter = Counter.builder("transactions.blocked.total")
            .description("Total number of transactions blocked")
            .tag("component", "fraud-detection")
            .register(meterRegistry);

        this.transactionsApprovedCounter = Counter.builder("transactions.approved.total")
            .description("Total number of transactions approved")
            .tag("component", "fraud-detection")
            .register(meterRegistry);

        // Initialize risk score distribution
        this.riskScoreDistribution = DistributionSummary.builder("risk.score.distribution")
            .description("Distribution of risk scores")
            .tag("component", "risk-assessment")
            .baseUnit("score")
            .register(meterRegistry);

        this.currentAverageRiskScore = new AtomicLong(0);
        Gauge.builder("risk.score.average", currentAverageRiskScore, AtomicLong::get)
            .description("Current average risk score")
            .tag("component", "risk-assessment")
            .register(meterRegistry);

        // Initialize timers
        this.fraudDetectionDuration = Timer.builder("fraud.detection.duration")
            .description("Time taken to detect fraud")
            .tag("component", "fraud-detection")
            .register(meterRegistry);

        this.transactionProcessingDuration = Timer.builder("transaction.processing.duration")
            .description("Time taken to process a transaction")
            .tag("component", "transaction-service")
            .register(meterRegistry);

        // Initialize hourly counters
        this.transactionsProcessedLastHour = new AtomicLong(0);
        Gauge.builder("transactions.processed.hour", transactionsProcessedLastHour, AtomicLong::get)
            .description("Number of transactions processed in the last hour")
            .tag("component", "transaction-service")
            .register(meterRegistry);

        this.transactionsBlockedLastHour = new AtomicLong(0);
        Gauge.builder("transactions.blocked.hour", transactionsBlockedLastHour, AtomicLong::get)
            .description("Number of transactions blocked in the last hour")
            .tag("component", "fraud-detection")
            .register(meterRegistry);
    }

    // ========== Fraud Detection Metrics ==========

    /**
     * Record a fraud detection event
     */
    public void recordFraudDetected() {
        fraudDetectedCounter.increment();
    }

    /**
     * Record a fraud detection event with specific action
     * @param action The action taken (BLOCK, REVIEW, APPROVE)
     */
    public void recordFraudDetected(String action) {
        Counter.builder("fraud.detected.by.action")
            .description("Fraudulent transactions by action")
            .tag("action", action)
            .tag("component", "fraud-detection")
            .register(meterRegistry)
            .increment();
    }

    /**
     * Record fraud detection execution time
     * @param durationMillis Duration in milliseconds
     */
    public void recordFraudDetectionDuration(long durationMillis) {
        fraudDetectionDuration.record(java.time.Duration.ofMillis(durationMillis));
    }

    // ========== Transaction Metrics ==========

    /**
     * Record a processed transaction
     */
    public void recordTransactionProcessed() {
        transactionsProcessedCounter.increment();
        transactionsProcessedLastHour.incrementAndGet();
    }

    /**
     * Record a blocked transaction
     */
    public void recordTransactionBlocked() {
        transactionsBlockedCounter.increment();
        transactionsBlockedLastHour.incrementAndGet();
    }

    /**
     * Record an approved transaction
     */
    public void recordTransactionApproved() {
        transactionsApprovedCounter.increment();
    }

    /**
     * Record transaction processing time
     * @param durationMillis Duration in milliseconds
     */
    public void recordTransactionProcessingDuration(long durationMillis) {
        transactionProcessingDuration.record(java.time.Duration.ofMillis(durationMillis));
    }

    /**
     * Record a transaction by type
     * @param type Transaction type (PURCHASE, TRANSFER, WITHDRAWAL, etc.)
     */
    public void recordTransactionByType(String type) {
        Counter.builder("transactions.by.type")
            .description("Transactions by type")
            .tag("type", type)
            .tag("component", "transaction-service")
            .register(meterRegistry)
            .increment();
    }

    // ========== Risk Score Metrics ==========

    /**
     * Record a risk score
     * @param riskScore The risk score (0.0 to 1.0)
     */
    public void recordRiskScore(double riskScore) {
        riskScoreDistribution.record(riskScore);

        // Update moving average (simple implementation)
        long currentAvg = currentAverageRiskScore.get();
        long newAvg = (long) ((currentAvg + (riskScore * 100)) / 2);
        currentAverageRiskScore.set(newAvg);
    }

    /**
     * Record high-risk transaction
     */
    public void recordHighRiskTransaction() {
        Counter.builder("transactions.high.risk")
            .description("Number of high-risk transactions")
            .tag("component", "risk-assessment")
            .register(meterRegistry)
            .increment();
    }

    // ========== Business Metrics ==========

    /**
     * Calculate and return fraud detection rate
     * @return Fraud detection rate (frauds detected / total transactions)
     */
    public double getFraudDetectionRate() {
        double totalProcessed = transactionsProcessedCounter.count();
        double totalFraud = fraudDetectedCounter.count();

        if (totalProcessed == 0) {
            return 0.0;
        }

        return (totalFraud / totalProcessed) * 100;
    }

    /**
     * Record fraud detection rate as a gauge
     */
    public void updateFraudDetectionRate() {
        double rate = getFraudDetectionRate();
        Gauge.builder("fraud.detection.rate", () -> rate)
            .description("Percentage of transactions flagged as fraudulent")
            .tag("component", "fraud-detection")
            .baseUnit("percent")
            .register(meterRegistry);
    }

    /**
     * Reset hourly counters (should be called by a scheduled task)
     */
    public void resetHourlyCounters() {
        transactionsProcessedLastHour.set(0);
        transactionsBlockedLastHour.set(0);
    }

    // ========== Rule-based Metrics ==========

    /**
     * Record violation of a specific fraud rule
     * @param ruleId The fraud rule that was violated
     */
    public void recordRuleViolation(String ruleId) {
        Counter.builder("fraud.rule.violations")
            .description("Violations by fraud rule")
            .tag("rule_id", ruleId)
            .tag("component", "fraud-detection")
            .register(meterRegistry)
            .increment();
    }

    /**
     * Record user risk profile update
     * @param userId User ID
     * @param previousRiskLevel Previous risk level
     * @param newRiskLevel New risk level
     */
    public void recordUserRiskProfileUpdate(String userId, String previousRiskLevel, String newRiskLevel) {
        Counter.builder("user.risk.profile.updates")
            .description("User risk profile updates")
            .tag("previous_level", previousRiskLevel)
            .tag("new_level", newRiskLevel)
            .tag("component", "user-profile")
            .register(meterRegistry)
            .increment();
    }
}
