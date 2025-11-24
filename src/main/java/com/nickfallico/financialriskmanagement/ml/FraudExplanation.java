package com.nickfallico.financialriskmanagement.ml;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.Builder;
import lombok.Data;

/**
 * Provides explainability for fraud detection decisions.
 * Similar to SHAP (SHapley Additive exPlanations) values for ML interpretability.
 *
 * Each feature contribution shows how much that feature pushed the fraud
 * probability up or down from the baseline.
 */
@Data
@Builder
public class FraudExplanation {

    /**
     * Individual feature contribution to the final fraud score.
     */
    @Data
    @Builder
    public static class FeatureContribution {
        private String featureName;
        private String featureDescription;
        private double rawValue;
        private double normalizedValue;
        private double contribution;  // Positive = increases fraud risk, Negative = decreases
        private String impact;        // HIGH_RISK, MODERATE_RISK, LOW_RISK, NEUTRAL

        /**
         * Human-readable explanation of this feature's impact.
         */
        public String getExplanation() {
            String direction = contribution >= 0 ? "increases" : "decreases";
            return String.format("%s (%.2f) %s fraud risk by %.1f%%",
                featureDescription, rawValue, direction, Math.abs(contribution * 100));
        }
    }

    /**
     * Rule-based explanation for triggered fraud rules.
     */
    @Data
    @Builder
    public static class RuleExplanation {
        private String ruleId;
        private String ruleName;
        private String description;
        private double riskScore;
        private String severity;  // CRITICAL, HIGH, MEDIUM, LOW

        public String getExplanation() {
            return String.format("[%s] %s: %s (Risk: %.0f%%)",
                severity, ruleName, description, riskScore * 100);
        }
    }

    // Base probability before any features are considered
    private double baselineProbability;

    // Final computed fraud probability
    private double finalProbability;

    // Feature contributions (SHAP-like values)
    private List<FeatureContribution> featureContributions;

    // Triggered fraud rules with explanations
    private List<RuleExplanation> ruleExplanations;

    // Top contributing factors (sorted by absolute contribution)
    private List<String> topRiskFactors;

    // Decision explanation
    private String decisionReason;

    // Confidence in the prediction (based on feature coverage)
    private double confidence;

    /**
     * Get features sorted by their contribution magnitude (highest impact first).
     */
    public List<FeatureContribution> getFeaturesByImpact() {
        return featureContributions.stream()
            .sorted(Comparator.comparingDouble(
                (FeatureContribution f) -> Math.abs(f.getContribution())).reversed())
            .collect(Collectors.toList());
    }

    /**
     * Get only high-risk contributing features.
     */
    public List<FeatureContribution> getHighRiskFeatures() {
        return featureContributions.stream()
            .filter(f -> "HIGH_RISK".equals(f.getImpact()) || "MODERATE_RISK".equals(f.getImpact()))
            .sorted(Comparator.comparingDouble(
                (FeatureContribution f) -> f.getContribution()).reversed())
            .collect(Collectors.toList());
    }

    /**
     * Generate a human-readable summary of why this transaction was flagged.
     */
    public String generateSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Fraud Probability: %.1f%% (Confidence: %.0f%%)\n",
            finalProbability * 100, confidence * 100));
        sb.append(String.format("Decision: %s\n\n", decisionReason));

        if (!ruleExplanations.isEmpty()) {
            sb.append("Triggered Rules:\n");
            for (RuleExplanation rule : ruleExplanations) {
                sb.append(String.format("  - %s\n", rule.getExplanation()));
            }
            sb.append("\n");
        }

        sb.append("Top Risk Factors:\n");
        getFeaturesByImpact().stream()
            .limit(3)
            .forEach(f -> sb.append(String.format("  - %s\n", f.getExplanation())));

        return sb.toString();
    }

    /**
     * Convert to a Map for JSON serialization in API responses.
     */
    public Map<String, Object> toMap() {
        return Map.of(
            "baselineProbability", baselineProbability,
            "finalProbability", finalProbability,
            "confidence", confidence,
            "decisionReason", decisionReason,
            "topRiskFactors", topRiskFactors,
            "featureContributions", featureContributions.stream()
                .map(f -> Map.of(
                    "feature", f.getFeatureName(),
                    "description", f.getFeatureDescription(),
                    "value", f.getRawValue(),
                    "contribution", f.getContribution(),
                    "impact", f.getImpact()
                ))
                .collect(Collectors.toList()),
            "triggeredRules", ruleExplanations.stream()
                .map(r -> Map.of(
                    "ruleId", r.getRuleId(),
                    "name", r.getRuleName(),
                    "description", r.getDescription(),
                    "riskScore", r.getRiskScore(),
                    "severity", r.getSeverity()
                ))
                .collect(Collectors.toList())
        );
    }
}
