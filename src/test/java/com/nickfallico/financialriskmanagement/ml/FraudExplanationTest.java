package com.nickfallico.financialriskmanagement.ml;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.nickfallico.financialriskmanagement.ml.FraudExplanation.FeatureContribution;
import com.nickfallico.financialriskmanagement.ml.FraudExplanation.RuleExplanation;

/**
 * Tests for FraudExplanation model.
 * Verifies explainability features work correctly.
 */
@DisplayName("FraudExplanation Tests")
class FraudExplanationTest {

    @Test
    @DisplayName("Should build complete fraud explanation")
    void shouldBuildCompleteFraudExplanation() {
        FraudExplanation explanation = FraudExplanation.builder()
            .baselineProbability(0.05)
            .finalProbability(0.85)
            .confidence(0.92)
            .decisionReason("Transaction BLOCKED: High fraud probability")
            .topRiskFactors(List.of("High transaction amount", "New merchant category"))
            .featureContributions(List.of(
                FeatureContribution.builder()
                    .featureName("amount_deviation")
                    .featureDescription("Transaction amount deviation")
                    .rawValue(0.95)
                    .contribution(0.25)
                    .impact("HIGH_RISK")
                    .build()
            ))
            .ruleExplanations(List.of(
                RuleExplanation.builder()
                    .ruleId("HIGH_AMOUNT")
                    .ruleName("High Amount Rule")
                    .description("Transaction exceeds $10,000 threshold")
                    .riskScore(0.8)
                    .severity("HIGH")
                    .build()
            ))
            .build();

        assertThat(explanation.getBaselineProbability()).isEqualTo(0.05);
        assertThat(explanation.getFinalProbability()).isEqualTo(0.85);
        assertThat(explanation.getConfidence()).isEqualTo(0.92);
        assertThat(explanation.getTopRiskFactors()).hasSize(2);
        assertThat(explanation.getFeatureContributions()).hasSize(1);
        assertThat(explanation.getRuleExplanations()).hasSize(1);
    }

    @Test
    @DisplayName("Should sort features by impact")
    void shouldSortFeaturesByImpact() {
        FraudExplanation explanation = FraudExplanation.builder()
            .baselineProbability(0.05)
            .finalProbability(0.7)
            .featureContributions(List.of(
                FeatureContribution.builder()
                    .featureName("low_impact")
                    .featureDescription("Low impact feature")
                    .rawValue(0.3)
                    .contribution(0.05)
                    .impact("LOW_RISK")
                    .build(),
                FeatureContribution.builder()
                    .featureName("high_impact")
                    .featureDescription("High impact feature")
                    .rawValue(0.9)
                    .contribution(0.30)
                    .impact("HIGH_RISK")
                    .build(),
                FeatureContribution.builder()
                    .featureName("medium_impact")
                    .featureDescription("Medium impact feature")
                    .rawValue(0.6)
                    .contribution(0.15)
                    .impact("MODERATE_RISK")
                    .build()
            ))
            .ruleExplanations(List.of())
            .topRiskFactors(List.of())
            .decisionReason("Test")
            .confidence(0.8)
            .build();

        List<FeatureContribution> sorted = explanation.getFeaturesByImpact();

        assertThat(sorted.get(0).getFeatureName()).isEqualTo("high_impact");
        assertThat(sorted.get(1).getFeatureName()).isEqualTo("medium_impact");
        assertThat(sorted.get(2).getFeatureName()).isEqualTo("low_impact");
    }

    @Test
    @DisplayName("Should filter high risk features")
    void shouldFilterHighRiskFeatures() {
        FraudExplanation explanation = FraudExplanation.builder()
            .baselineProbability(0.05)
            .finalProbability(0.7)
            .featureContributions(List.of(
                FeatureContribution.builder()
                    .featureName("neutral")
                    .featureDescription("Neutral feature")
                    .rawValue(0.2)
                    .contribution(-0.02)
                    .impact("NEUTRAL")
                    .build(),
                FeatureContribution.builder()
                    .featureName("high_risk")
                    .featureDescription("High risk feature")
                    .rawValue(0.9)
                    .contribution(0.25)
                    .impact("HIGH_RISK")
                    .build(),
                FeatureContribution.builder()
                    .featureName("moderate_risk")
                    .featureDescription("Moderate risk feature")
                    .rawValue(0.7)
                    .contribution(0.12)
                    .impact("MODERATE_RISK")
                    .build()
            ))
            .ruleExplanations(List.of())
            .topRiskFactors(List.of())
            .decisionReason("Test")
            .confidence(0.8)
            .build();

        List<FeatureContribution> highRisk = explanation.getHighRiskFeatures();

        assertThat(highRisk).hasSize(2);
        assertThat(highRisk.stream().map(FeatureContribution::getFeatureName))
            .containsExactlyInAnyOrder("high_risk", "moderate_risk");
    }

    @Test
    @DisplayName("Should generate human-readable summary")
    void shouldGenerateHumanReadableSummary() {
        FraudExplanation explanation = FraudExplanation.builder()
            .baselineProbability(0.05)
            .finalProbability(0.85)
            .confidence(0.90)
            .decisionReason("Transaction BLOCKED: High fraud probability (85%)")
            .topRiskFactors(List.of("Unusual amount"))
            .featureContributions(List.of(
                FeatureContribution.builder()
                    .featureName("amount")
                    .featureDescription("Transaction amount deviation")
                    .rawValue(0.95)
                    .contribution(0.20)
                    .impact("HIGH_RISK")
                    .build()
            ))
            .ruleExplanations(List.of(
                RuleExplanation.builder()
                    .ruleId("HIGH_AMOUNT")
                    .ruleName("High Amount")
                    .description("Amount exceeds threshold")
                    .riskScore(0.8)
                    .severity("HIGH")
                    .build()
            ))
            .build();

        String summary = explanation.generateSummary();

        assertThat(summary).contains("Fraud Probability: 85.0%");
        assertThat(summary).contains("Confidence: 90%");
        assertThat(summary).contains("Triggered Rules:");
        assertThat(summary).contains("Top Risk Factors:");
    }

    @Test
    @DisplayName("Should convert to map for JSON serialization")
    void shouldConvertToMap() {
        FraudExplanation explanation = FraudExplanation.builder()
            .baselineProbability(0.05)
            .finalProbability(0.75)
            .confidence(0.85)
            .decisionReason("Review required")
            .topRiskFactors(List.of("Factor 1", "Factor 2"))
            .featureContributions(List.of(
                FeatureContribution.builder()
                    .featureName("feature1")
                    .featureDescription("Description 1")
                    .rawValue(0.8)
                    .contribution(0.15)
                    .impact("HIGH_RISK")
                    .build()
            ))
            .ruleExplanations(List.of(
                RuleExplanation.builder()
                    .ruleId("RULE_1")
                    .ruleName("Rule 1")
                    .description("Rule description")
                    .riskScore(0.7)
                    .severity("HIGH")
                    .build()
            ))
            .build();

        Map<String, Object> map = explanation.toMap();

        assertThat(map).containsKey("baselineProbability");
        assertThat(map).containsKey("finalProbability");
        assertThat(map).containsKey("featureContributions");
        assertThat(map).containsKey("triggeredRules");
        assertThat(map.get("finalProbability")).isEqualTo(0.75);
    }

    @Test
    @DisplayName("FeatureContribution should generate explanation text")
    void featureContributionShouldGenerateExplanation() {
        FeatureContribution contribution = FeatureContribution.builder()
            .featureName("amount_deviation")
            .featureDescription("Transaction amount is 5x average")
            .rawValue(0.95)
            .normalizedValue(0.95)
            .contribution(0.25)
            .impact("HIGH_RISK")
            .build();

        String explanation = contribution.getExplanation();

        assertThat(explanation).contains("Transaction amount is 5x average");
        assertThat(explanation).contains("increases fraud risk");
        assertThat(explanation).contains("25.0%");
    }

    @Test
    @DisplayName("FeatureContribution with negative contribution decreases risk")
    void featureContributionWithNegativeContributionDecreasesRisk() {
        FeatureContribution contribution = FeatureContribution.builder()
            .featureName("user_history")
            .featureDescription("Established user with 200+ transactions")
            .rawValue(0.2)
            .normalizedValue(0.2)
            .contribution(-0.15)
            .impact("NEUTRAL")
            .build();

        String explanation = contribution.getExplanation();

        assertThat(explanation).contains("decreases fraud risk");
    }

    @Test
    @DisplayName("RuleExplanation should format correctly")
    void ruleExplanationShouldFormatCorrectly() {
        RuleExplanation rule = RuleExplanation.builder()
            .ruleId("VELOCITY_5MIN")
            .ruleName("Velocity 5 Min")
            .description("More than 3 transactions in 5 minutes")
            .riskScore(0.9)
            .severity("CRITICAL")
            .build();

        String explanation = rule.getExplanation();

        assertThat(explanation).contains("[CRITICAL]");
        assertThat(explanation).contains("Velocity 5 Min");
        assertThat(explanation).contains("More than 3 transactions in 5 minutes");
        assertThat(explanation).contains("90%");
    }
}
