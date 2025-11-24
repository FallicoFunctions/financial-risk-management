package com.nickfallico.financialriskmanagement.ml;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.nickfallico.financialriskmanagement.ml.FraudExplanation.FeatureContribution;
import com.nickfallico.financialriskmanagement.ml.FraudExplanation.RuleExplanation;
import com.nickfallico.financialriskmanagement.model.ImmutableUserRiskProfile;
import com.nickfallico.financialriskmanagement.model.MerchantCategoryFrequency;
import com.nickfallico.financialriskmanagement.model.Transactions;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * Explainable fraud detection that combines ML model predictions with
 * interpretable feature contributions (SHAP-like explanations).
 *
 * Provides transparency into why a transaction was flagged as fraudulent,
 * which is critical for:
 * - Regulatory compliance (GDPR right to explanation)
 * - Fraud analyst review
 * - Model debugging and improvement
 * - Customer support (explaining blocked transactions)
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ExplainableFraudDetector {

    private static final double BASELINE_PROBABILITY = 0.05; // 5% base fraud rate

    private final FraudDetectionModel fraudDetectionModel;
    private final FraudFeatureExtractor featureExtractor;
    private final FraudRuleEngine ruleEngine;

    // Feature names for explanation
    private static final String[] FEATURE_NAMES = {
        "amount_deviation",
        "merchant_category_risk",
        "temporal_risk",
        "user_history_risk",
        "international_risk"
    };

    private static final String[] FEATURE_DESCRIPTIONS = {
        "Transaction amount deviation from user average",
        "Merchant category risk level",
        "Time of transaction risk",
        "User transaction history risk",
        "International transaction risk"
    };

    // Feature weights for contribution calculation
    private static final double[] FEATURE_WEIGHTS = {0.25, 0.20, 0.15, 0.20, 0.20};

    /**
     * Perform fraud detection with full explainability.
     * Returns both the prediction and detailed explanation.
     */
    public Mono<FraudExplanation> detectWithExplanation(
            Transactions transaction,
            ImmutableUserRiskProfile profile,
            MerchantCategoryFrequency merchantFrequency) {

        // Extract features
        List<Double> features = featureExtractor.extractFeatures(
            transaction, profile, merchantFrequency);

        // Get ML model prediction
        double mlProbability = fraudDetectionModel.predictFraudProbability(transaction, profile);

        // Create evaluation context for rule engine
        FraudRule.FraudEvaluationContext context = new FraudRule.FraudEvaluationContext(
            transaction, profile, merchantFrequency);

        // Evaluate rules and build explanation
        return ruleEngine.evaluateTransaction(context)
            .map(violations -> {
                // Calculate rule-based probability
                double ruleProbability = ruleEngine.calculateFraudProbability(violations);

                // Ensemble: weighted combination of ML and rules
                double ensembleProbability = combineScores(mlProbability, ruleProbability);

                // Build feature contributions
                List<FeatureContribution> featureContributions = buildFeatureContributions(
                    features, ensembleProbability);

                // Build rule explanations
                List<RuleExplanation> ruleExplanations = buildRuleExplanations(violations);

                // Determine action
                FraudRuleEngine.FraudAction action = ruleEngine.determineAction(ensembleProbability);

                // Calculate confidence based on feature coverage and consistency
                double confidence = calculateConfidence(features, mlProbability, ruleProbability);

                // Build top risk factors
                List<String> topRiskFactors = buildTopRiskFactors(
                    featureContributions, ruleExplanations);

                return FraudExplanation.builder()
                    .baselineProbability(BASELINE_PROBABILITY)
                    .finalProbability(ensembleProbability)
                    .featureContributions(featureContributions)
                    .ruleExplanations(ruleExplanations)
                    .topRiskFactors(topRiskFactors)
                    .decisionReason(buildDecisionReason(action, ensembleProbability))
                    .confidence(confidence)
                    .build();
            });
    }

    /**
     * Ensemble scoring: combines ML model and rule engine scores.
     * Uses weighted average with rule boost for triggered rules.
     */
    private double combineScores(double mlProbability, double ruleProbability) {
        // If rules triggered, give them more weight
        if (ruleProbability > 0) {
            // 40% ML, 60% rules when rules fire
            return 0.4 * mlProbability + 0.6 * ruleProbability;
        }
        // Pure ML score when no rules triggered
        return mlProbability;
    }

    /**
     * Build SHAP-like feature contributions.
     * Each contribution shows how much the feature moved the probability
     * from the baseline.
     */
    private List<FeatureContribution> buildFeatureContributions(
            List<Double> features, double finalProbability) {

        List<FeatureContribution> contributions = new ArrayList<>();
        double totalContribution = finalProbability - BASELINE_PROBABILITY;

        for (int i = 0; i < features.size(); i++) {
            double featureValue = features.get(i);
            double weight = FEATURE_WEIGHTS[i];

            // Calculate contribution: how much this feature adds/subtracts
            // Higher values indicate more risk, scaled by weight
            double contribution = (featureValue - 0.5) * weight * totalContribution * 2;

            String impact = categorizeImpact(contribution, featureValue);

            contributions.add(FeatureContribution.builder()
                .featureName(FEATURE_NAMES[i])
                .featureDescription(FEATURE_DESCRIPTIONS[i])
                .rawValue(featureValue)
                .normalizedValue(featureValue) // Already normalized 0-1
                .contribution(contribution)
                .impact(impact)
                .build());
        }

        return contributions;
    }

    /**
     * Categorize feature impact based on contribution and value.
     */
    private String categorizeImpact(double contribution, double featureValue) {
        if (contribution > 0.15 || featureValue > 0.8) return "HIGH_RISK";
        if (contribution > 0.05 || featureValue > 0.6) return "MODERATE_RISK";
        if (contribution > 0 || featureValue > 0.4) return "LOW_RISK";
        return "NEUTRAL";
    }

    /**
     * Convert rule violations to explanations.
     */
    private List<RuleExplanation> buildRuleExplanations(
            List<FraudRule.FraudViolation> violations) {

        return violations.stream()
            .map(v -> RuleExplanation.builder()
                .ruleId(v.ruleId())
                .ruleName(formatRuleName(v.ruleId()))
                .description(v.description())
                .riskScore(v.riskScore())
                .severity(categorizeSeverity(v.riskScore()))
                .build())
            .collect(Collectors.toList());
    }

    /**
     * Format rule ID into human-readable name.
     */
    private String formatRuleName(String ruleId) {
        return ruleId.replace("_", " ")
            .toLowerCase()
            .replaceFirst("^\\w", String.valueOf(ruleId.charAt(0)));
    }

    /**
     * Categorize severity based on risk score.
     */
    private String categorizeSeverity(double riskScore) {
        if (riskScore >= 0.9) return "CRITICAL";
        if (riskScore >= 0.7) return "HIGH";
        if (riskScore >= 0.5) return "MEDIUM";
        return "LOW";
    }

    /**
     * Calculate confidence in the prediction.
     * Higher when ML and rules agree, lower when they diverge.
     */
    private double calculateConfidence(List<Double> features,
            double mlProbability, double ruleProbability) {

        // Base confidence from feature completeness
        double featureCompleteness = features.stream()
            .filter(f -> f != 0.5) // 0.5 often indicates missing data
            .count() / (double) features.size();

        // Agreement between ML and rules
        double agreement = 1.0 - Math.abs(mlProbability - ruleProbability);

        // Combined confidence
        return 0.5 * featureCompleteness + 0.5 * agreement;
    }

    /**
     * Build list of top risk factors for quick summary.
     */
    private List<String> buildTopRiskFactors(
            List<FeatureContribution> features,
            List<RuleExplanation> rules) {

        List<String> factors = new ArrayList<>();

        // Add triggered rules first (most specific)
        rules.stream()
            .filter(r -> r.getRiskScore() >= 0.5)
            .limit(3)
            .forEach(r -> factors.add(r.getDescription()));

        // Add top feature contributions
        features.stream()
            .filter(f -> "HIGH_RISK".equals(f.getImpact()) || "MODERATE_RISK".equals(f.getImpact()))
            .sorted((a, b) -> Double.compare(b.getContribution(), a.getContribution()))
            .limit(3 - factors.size())
            .forEach(f -> factors.add(f.getFeatureDescription()));

        return factors;
    }

    /**
     * Build human-readable decision reason.
     */
    private String buildDecisionReason(FraudRuleEngine.FraudAction action, double probability) {
        return switch (action) {
            case BLOCK -> String.format(
                "Transaction BLOCKED: High fraud probability (%.0f%%) exceeds safety threshold",
                probability * 100);
            case REVIEW -> String.format(
                "Transaction requires REVIEW: Elevated fraud probability (%.0f%%) detected",
                probability * 100);
            case MONITOR -> String.format(
                "Transaction APPROVED with monitoring: Low-moderate risk (%.0f%%)",
                probability * 100);
            case APPROVE -> String.format(
                "Transaction APPROVED: Low fraud probability (%.0f%%)",
                probability * 100);
        };
    }
}
