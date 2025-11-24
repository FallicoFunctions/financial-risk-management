package com.nickfallico.financialriskmanagement.ml;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.nickfallico.financialriskmanagement.ml.ModelPerformanceTracker.ModelMetrics;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

/**
 * Tests for ModelPerformanceTracker.
 * Verifies ROC-AUC, Precision-Recall, and confusion matrix calculations.
 */
@DisplayName("ModelPerformanceTracker Tests")
class ModelPerformanceTrackerTest {

    private ModelPerformanceTracker tracker;
    private SimpleMeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        tracker = new ModelPerformanceTracker(meterRegistry);
    }

    @Test
    @DisplayName("Should return baseline metrics with no data")
    void shouldReturnBaselineMetricsWithNoData() {
        ModelMetrics metrics = tracker.calculateMetrics();

        assertThat(metrics.getTotalPredictions()).isEqualTo(0);
        assertThat(metrics.getAucRoc()).isEqualTo(0.5); // Random baseline
    }

    @Test
    @DisplayName("Should record predictions correctly")
    void shouldRecordPredictions() {
        tracker.recordPrediction("tx-1", 0.8, List.of("HIGH_AMOUNT"));
        tracker.recordPrediction("tx-2", 0.3, List.of());
        tracker.recordPrediction("tx-3", 0.95, List.of("VELOCITY", "HIGH_AMOUNT"));

        ModelMetrics metrics = tracker.calculateMetrics();

        assertThat(metrics.getTotalPredictions()).isEqualTo(3);
    }

    @Test
    @DisplayName("Should update metrics after feedback")
    void shouldUpdateMetricsAfterFeedback() {
        // Record predictions
        tracker.recordPrediction("tx-1", 0.9, List.of("HIGH_AMOUNT"));
        tracker.recordPrediction("tx-2", 0.2, List.of());

        // Provide feedback
        tracker.recordFeedback("tx-1", true);  // True positive
        tracker.recordFeedback("tx-2", false); // True negative

        ModelMetrics metrics = tracker.calculateMetrics();

        assertThat(metrics.getLabeledPredictions()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should calculate perfect AUC-ROC for separable data")
    void shouldCalculatePerfectAucForSeparableData() {
        // All fraudulent transactions have high scores
        tracker.recordPrediction("fraud-1", 0.95, List.of());
        tracker.recordPrediction("fraud-2", 0.90, List.of());
        tracker.recordPrediction("fraud-3", 0.85, List.of());

        // All legitimate transactions have low scores
        tracker.recordPrediction("legit-1", 0.15, List.of());
        tracker.recordPrediction("legit-2", 0.10, List.of());
        tracker.recordPrediction("legit-3", 0.05, List.of());

        // Provide correct feedback
        tracker.recordFeedback("fraud-1", true);
        tracker.recordFeedback("fraud-2", true);
        tracker.recordFeedback("fraud-3", true);
        tracker.recordFeedback("legit-1", false);
        tracker.recordFeedback("legit-2", false);
        tracker.recordFeedback("legit-3", false);

        // Add more samples to meet minimum
        for (int i = 0; i < 10; i++) {
            tracker.recordPrediction("extra-fraud-" + i, 0.8 + i * 0.01, List.of());
            tracker.recordFeedback("extra-fraud-" + i, true);
            tracker.recordPrediction("extra-legit-" + i, 0.1 + i * 0.01, List.of());
            tracker.recordFeedback("extra-legit-" + i, false);
        }

        ModelMetrics metrics = tracker.calculateMetrics();

        // Perfect separation should yield AUC close to 1.0
        assertThat(metrics.getAucRoc()).isGreaterThan(0.95);
    }

    @Test
    @DisplayName("Should calculate confusion matrix correctly")
    void shouldCalculateConfusionMatrixCorrectly() {
        // TP: high score, actual fraud
        tracker.recordPrediction("tp-1", 0.8, List.of());
        tracker.recordFeedback("tp-1", true);

        // TN: low score, not fraud
        tracker.recordPrediction("tn-1", 0.2, List.of());
        tracker.recordFeedback("tn-1", false);

        // FP: high score, not fraud
        tracker.recordPrediction("fp-1", 0.7, List.of());
        tracker.recordFeedback("fp-1", false);

        // FN: low score, actual fraud
        tracker.recordPrediction("fn-1", 0.3, List.of());
        tracker.recordFeedback("fn-1", true);

        // Add more for reliable metrics
        for (int i = 0; i < 10; i++) {
            tracker.recordPrediction("tp-" + (i + 2), 0.8, List.of());
            tracker.recordFeedback("tp-" + (i + 2), true);
            tracker.recordPrediction("tn-" + (i + 2), 0.2, List.of());
            tracker.recordFeedback("tn-" + (i + 2), false);
        }

        ModelMetrics metrics = tracker.calculateMetrics();

        assertThat(metrics.getConfusionMatrix()).isNotNull();
        assertThat(metrics.getConfusionMatrix().getTp()).isGreaterThan(0);
        assertThat(metrics.getConfusionMatrix().getTn()).isGreaterThan(0);
    }

    @Test
    @DisplayName("Should calculate precision and recall")
    void shouldCalculatePrecisionAndRecall() {
        // Create scenario with known precision/recall
        // 8 TP, 2 FP, 2 FN, 8 TN at threshold 0.5
        for (int i = 0; i < 8; i++) {
            tracker.recordPrediction("tp-" + i, 0.7 + i * 0.02, List.of());
            tracker.recordFeedback("tp-" + i, true);
        }
        for (int i = 0; i < 2; i++) {
            tracker.recordPrediction("fp-" + i, 0.6 + i * 0.05, List.of());
            tracker.recordFeedback("fp-" + i, false);
        }
        for (int i = 0; i < 2; i++) {
            tracker.recordPrediction("fn-" + i, 0.3 + i * 0.05, List.of());
            tracker.recordFeedback("fn-" + i, true);
        }
        for (int i = 0; i < 8; i++) {
            tracker.recordPrediction("tn-" + i, 0.1 + i * 0.03, List.of());
            tracker.recordFeedback("tn-" + i, false);
        }

        ModelMetrics metrics = tracker.calculateMetrics();

        // Precision = TP / (TP + FP) = 8 / 10 = 0.8
        // Recall = TP / (TP + FN) = 8 / 10 = 0.8
        assertThat(metrics.getPrecision()).isBetween(0.7, 0.9);
        assertThat(metrics.getRecall()).isBetween(0.7, 0.9);
    }

    @Test
    @DisplayName("Should track per-rule performance")
    void shouldTrackPerRulePerformance() {
        // Rule A triggers for fraud
        tracker.recordPrediction("tx-1", 0.8, List.of("RULE_A"));
        tracker.recordFeedback("tx-1", true);

        // Rule A triggers for non-fraud (false positive)
        tracker.recordPrediction("tx-2", 0.7, List.of("RULE_A"));
        tracker.recordFeedback("tx-2", false);

        // Rule B only triggers for fraud
        tracker.recordPrediction("tx-3", 0.9, List.of("RULE_B"));
        tracker.recordFeedback("tx-3", true);

        // Add more samples
        for (int i = 0; i < 10; i++) {
            tracker.recordPrediction("rule-a-" + i, 0.75, List.of("RULE_A"));
            tracker.recordFeedback("rule-a-" + i, i % 2 == 0);
        }

        ModelMetrics metrics = tracker.calculateMetrics();

        assertThat(metrics.getRulePerformance()).containsKey("RULE_A");
        assertThat(metrics.getRulePerformance()).containsKey("RULE_B");

        // Rule B should have better precision (only true positives)
        assertThat(metrics.getRulePerformance().get("RULE_B").getPrecision()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Should find optimal threshold by F1 score")
    void shouldFindOptimalThreshold() {
        // Create data where threshold 0.6 is optimal
        for (int i = 0; i < 20; i++) {
            double score = 0.5 + (i * 0.025);
            boolean isFraud = score > 0.6;
            tracker.recordPrediction("tx-" + i, score, List.of());
            tracker.recordFeedback("tx-" + i, isFraud);
        }

        ModelMetrics metrics = tracker.calculateMetrics();

        assertThat(metrics.getOptimalThreshold()).isBetween(0.5, 0.7);
    }

    @Test
    @DisplayName("Should generate threshold metrics for all thresholds")
    void shouldGenerateThresholdMetrics() {
        // Add enough data
        for (int i = 0; i < 50; i++) {
            double score = i * 0.02;
            tracker.recordPrediction("tx-" + i, score, List.of());
            tracker.recordFeedback("tx-" + i, score > 0.5);
        }

        ModelMetrics metrics = tracker.calculateMetrics();

        assertThat(metrics.getThresholdMetrics()).isNotNull();
        assertThat(metrics.getThresholdMetrics()).hasSizeGreaterThan(5);

        // Each threshold should have valid metrics
        metrics.getThresholdMetrics().forEach(tm -> {
            assertThat(tm.getThreshold()).isBetween(0.0, 1.0);
            assertThat(tm.getPrecision()).isBetween(0.0, 1.0);
            assertThat(tm.getRecall()).isBetween(0.0, 1.0);
        });
    }

    @Test
    @DisplayName("Should register Micrometer gauges")
    void shouldRegisterMicrometerGauges() {
        // Gauges should be registered on construction
        assertThat(meterRegistry.find("ml.model.auc_roc").gauge()).isNotNull();
        assertThat(meterRegistry.find("ml.model.auc_pr").gauge()).isNotNull();
        assertThat(meterRegistry.find("ml.model.f1_score").gauge()).isNotNull();
        assertThat(meterRegistry.find("ml.model.precision").gauge()).isNotNull();
        assertThat(meterRegistry.find("ml.model.recall").gauge()).isNotNull();
    }

    @Test
    @DisplayName("Should handle maximum prediction storage limit")
    void shouldHandleMaxPredictionLimit() {
        // Add more than max predictions
        for (int i = 0; i < 12000; i++) {
            tracker.recordPrediction("tx-" + i, Math.random(), List.of());
        }

        ModelMetrics metrics = tracker.calculateMetrics();

        // Should be capped at max
        assertThat(metrics.getTotalPredictions()).isLessThanOrEqualTo(10000);
    }
}
