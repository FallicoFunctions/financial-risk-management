package com.nickfallico.financialriskmanagement.controller;

import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nickfallico.financialriskmanagement.ml.ModelPerformanceTracker;
import com.nickfallico.financialriskmanagement.ml.ModelPerformanceTracker.ModelMetrics;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * REST API for ML model performance monitoring and analysis.
 *
 * Provides endpoints for:
 * - Real-time model performance metrics (ROC-AUC, Precision, Recall)
 * - Per-rule performance analysis
 * - Threshold optimization recommendations
 * - Model feedback for continuous learning
 *
 * For interview: Demonstrates ML Ops awareness and model monitoring capabilities.
 */
@RestController
@RequestMapping("/api/ml")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "ML Model Performance", description = "Machine learning model monitoring and analysis")
public class ModelPerformanceController {

    private final ModelPerformanceTracker performanceTracker;

    /**
     * Get comprehensive model performance metrics.
     * Includes ROC-AUC, AUC-PR, confusion matrix, and threshold analysis.
     */
    @GetMapping("/performance")
    @Operation(summary = "Get model performance metrics",
        description = "Returns comprehensive ML model metrics including ROC-AUC, precision, recall, and optimal threshold")
    public Mono<ResponseEntity<ModelPerformanceResponse>> getPerformanceMetrics() {
        return Mono.fromSupplier(() -> {
            ModelMetrics metrics = performanceTracker.calculateMetrics();
            return ResponseEntity.ok(ModelPerformanceResponse.from(metrics));
        });
    }

    /**
     * Get ROC curve data for visualization.
     */
    @GetMapping("/performance/roc")
    @Operation(summary = "Get ROC curve data",
        description = "Returns ROC curve points for plotting TPR vs FPR")
    public Mono<ResponseEntity<Object>> getRocCurve() {
        return Mono.fromSupplier(() -> {
            ModelMetrics metrics = performanceTracker.calculateMetrics();
            return ResponseEntity.ok(Map.of(
                "aucRoc", metrics.getAucRoc(),
                "curve", metrics.getRocCurve() != null ? metrics.getRocCurve() : java.util.List.of()
            ));
        });
    }

    /**
     * Get Precision-Recall curve data for visualization.
     */
    @GetMapping("/performance/pr")
    @Operation(summary = "Get Precision-Recall curve data",
        description = "Returns PR curve points for imbalanced classification analysis")
    public Mono<ResponseEntity<Object>> getPrCurve() {
        return Mono.fromSupplier(() -> {
            ModelMetrics metrics = performanceTracker.calculateMetrics();
            return ResponseEntity.ok(Map.of(
                "aucPr", metrics.getAucPr(),
                "curve", metrics.getPrCurve() != null ? metrics.getPrCurve() : java.util.List.of()
            ));
        });
    }

    /**
     * Get per-threshold metrics for threshold tuning.
     */
    @GetMapping("/performance/thresholds")
    @Operation(summary = "Get metrics at different thresholds",
        description = "Returns precision, recall, F1 at various thresholds for optimization")
    public Mono<ResponseEntity<Object>> getThresholdMetrics() {
        return Mono.fromSupplier(() -> {
            ModelMetrics metrics = performanceTracker.calculateMetrics();
            return ResponseEntity.ok(Map.of(
                "optimalThreshold", metrics.getOptimalThreshold(),
                "thresholds", metrics.getThresholdMetrics() != null ?
                    metrics.getThresholdMetrics() : java.util.List.of()
            ));
        });
    }

    /**
     * Get per-rule performance statistics.
     */
    @GetMapping("/performance/rules")
    @Operation(summary = "Get rule performance statistics",
        description = "Returns precision and trigger counts for each fraud detection rule")
    public Mono<ResponseEntity<Object>> getRulePerformance() {
        return Mono.fromSupplier(() -> {
            ModelMetrics metrics = performanceTracker.calculateMetrics();
            Map<String, Object> ruleStats = metrics.getRulePerformance() != null ?
                metrics.getRulePerformance().entrySet().stream()
                    .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> Map.of(
                            "precision", e.getValue().getPrecision(),
                            "truePositives", e.getValue().getTruePositives(),
                            "falsePositives", e.getValue().getFalsePositives(),
                            "total", e.getValue().getTotal()
                        )
                    )) : Map.of();

            return ResponseEntity.ok(Map.of("rules", ruleStats));
        });
    }

    /**
     * Submit feedback for a prediction (for model improvement).
     */
    @PostMapping("/feedback")
    @Operation(summary = "Submit prediction feedback",
        description = "Record whether a flagged transaction was actually fraudulent")
    public Mono<ResponseEntity<Object>> submitFeedback(@RequestBody FeedbackRequest request) {
        return Mono.fromRunnable(() ->
            performanceTracker.recordFeedback(request.transactionId(), request.actualFraud()))
            .then(Mono.just(ResponseEntity.ok(Map.of(
                "status", "recorded",
                "transactionId", request.transactionId(),
                "actualFraud", request.actualFraud()
            ))));
    }

    /**
     * Get model health summary for dashboards.
     */
    @GetMapping("/health")
    @Operation(summary = "Get model health summary",
        description = "Quick health check for model performance monitoring")
    public Mono<ResponseEntity<Object>> getModelHealth() {
        return Mono.fromSupplier(() -> {
            ModelMetrics metrics = performanceTracker.calculateMetrics();

            String status;
            if (metrics.getAucRoc() >= 0.85) status = "EXCELLENT";
            else if (metrics.getAucRoc() >= 0.75) status = "GOOD";
            else if (metrics.getAucRoc() >= 0.65) status = "FAIR";
            else if (metrics.getLabeledPredictions() < 50) status = "INSUFFICIENT_DATA";
            else status = "NEEDS_ATTENTION";

            return ResponseEntity.ok(Map.of(
                "status", status,
                "aucRoc", metrics.getAucRoc(),
                "f1Score", metrics.getF1Score(),
                "labeledSamples", metrics.getLabeledPredictions(),
                "totalPredictions", metrics.getTotalPredictions(),
                "recommendation", getRecommendation(metrics)
            ));
        });
    }

    private String getRecommendation(ModelMetrics metrics) {
        if (metrics.getLabeledPredictions() < 50) {
            return "Need more labeled samples for reliable metrics";
        }
        if (metrics.getAucRoc() < 0.65) {
            return "Model performance degraded - consider retraining";
        }
        if (metrics.getPrecision() < 0.5) {
            return "High false positive rate - raise threshold to " + metrics.getOptimalThreshold();
        }
        if (metrics.getRecall() < 0.5) {
            return "Missing fraudulent transactions - lower threshold";
        }
        return "Model performing within acceptable parameters";
    }

    // ========== Request/Response DTOs ==========

    public record FeedbackRequest(String transactionId, boolean actualFraud) {}

    public record ModelPerformanceResponse(
        int totalPredictions,
        int labeledPredictions,
        double aucRoc,
        double aucPr,
        double precision,
        double recall,
        double f1Score,
        double accuracy,
        double optimalThreshold,
        ConfusionMatrixResponse confusionMatrix
    ) {
        public static ModelPerformanceResponse from(ModelMetrics metrics) {
            return new ModelPerformanceResponse(
                metrics.getTotalPredictions(),
                metrics.getLabeledPredictions(),
                round(metrics.getAucRoc()),
                round(metrics.getAucPr()),
                round(metrics.getPrecision()),
                round(metrics.getRecall()),
                round(metrics.getF1Score()),
                round(metrics.getAccuracy()),
                metrics.getOptimalThreshold(),
                metrics.getConfusionMatrix() != null ?
                    ConfusionMatrixResponse.from(metrics.getConfusionMatrix()) : null
            );
        }

        private static double round(double value) {
            return Math.round(value * 1000) / 1000.0;
        }
    }

    public record ConfusionMatrixResponse(
        long truePositives,
        long trueNegatives,
        long falsePositives,
        long falseNegatives,
        double threshold
    ) {
        public static ConfusionMatrixResponse from(
                ModelPerformanceTracker.ConfusionMatrix cm) {
            return new ConfusionMatrixResponse(
                cm.getTp(), cm.getTn(), cm.getFp(), cm.getFn(), cm.getThreshold()
            );
        }
    }
}
