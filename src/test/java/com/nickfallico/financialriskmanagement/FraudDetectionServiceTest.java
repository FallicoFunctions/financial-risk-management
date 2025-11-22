package com.nickfallico.financialriskmanagement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.nickfallico.financialriskmanagement.ml.FraudRule;
import com.nickfallico.financialriskmanagement.ml.FraudRuleEngine;
import com.nickfallico.financialriskmanagement.model.ImmutableUserRiskProfile;
import com.nickfallico.financialriskmanagement.model.MerchantCategoryFrequency;
import com.nickfallico.financialriskmanagement.model.Transactions;
import com.nickfallico.financialriskmanagement.service.FraudDetectionService;
import com.nickfallico.financialriskmanagement.service.MetricsService;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class FraudDetectionServiceTest {

    @Mock
    private FraudRuleEngine fraudRuleEngine;

    private MeterRegistry meterRegistry;
    private MetricsService metricsService;

    private FraudDetectionService fraudDetectionService;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        metricsService = new MetricsService(meterRegistry);
        fraudDetectionService = new FraudDetectionService(fraudRuleEngine, meterRegistry, metricsService);
    }

    @Test
    void assessFraudReturnsBlockingAssessmentWhenEngineFlagsHighRisk() {
        Transactions transaction = Transactions.builder()
            .id(UUID.randomUUID())
            .userId("user-risky")
            .amount(BigDecimal.valueOf(12_500))
            .currency("USD")
            .createdAt(Instant.parse("2025-01-01T12:00:00Z"))
            .isInternational(true)
            .merchantCategory("GAMBLING")
            .build();

        ImmutableUserRiskProfile riskProfile = ImmutableUserRiskProfile.createNew("user-risky")
            .toBuilder()
            .totalTransactions(3)
            .overallRiskScore(0.45)
            .lastTransactionDate(Instant.parse("2024-12-31T20:15:00Z"))
            .build();

        MerchantCategoryFrequency merchantFrequency = MerchantCategoryFrequency.createNew("user-risky")
            .toBuilder()
            .categoryFrequencies(Map.of("GAMBLING", 1))
            .build();

        List<FraudRule.FraudViolation> violations = List.of(
            new FraudRule.FraudViolation("HIGH_AMOUNT", "Amount above threshold", 0.85)
        );

        when(fraudRuleEngine.evaluateTransaction(any())).thenReturn(Mono.just(violations));
        when(fraudRuleEngine.calculateFraudProbability(violations)).thenReturn(0.92);
        when(fraudRuleEngine.determineAction(0.92)).thenReturn(FraudRuleEngine.FraudAction.BLOCK);

        FraudDetectionService.FraudAssessment assessment = fraudDetectionService
            .assessFraud(transaction, riskProfile, merchantFrequency)
            .block();

        assertNotNull(assessment);
        assertEquals(transaction.getId(), assessment.transactionId());
        assertEquals(0.92, assessment.fraudProbability());
        assertTrue(assessment.shouldBlock());
        assertTrue(assessment.getViolationSummary().contains("HIGH_AMOUNT"));

        ArgumentCaptor<FraudRule.FraudEvaluationContext> contextCaptor =
            ArgumentCaptor.forClass(FraudRule.FraudEvaluationContext.class);
        verify(fraudRuleEngine).evaluateTransaction(contextCaptor.capture());

        FraudRule.FraudEvaluationContext capturedContext = contextCaptor.getValue();
        assertEquals(transaction, capturedContext.transaction());
        assertEquals(riskProfile, capturedContext.profile());
        assertEquals(merchantFrequency, capturedContext.merchantFrequency());
    }

    @Test
    void assessFraudReturnsApprovalWhenNoViolationsFound() {
        Transactions transaction = Transactions.builder()
            .id(UUID.randomUUID())
            .userId("user-safe")
            .amount(BigDecimal.valueOf(125))
            .currency("USD")
            .createdAt(Instant.parse("2025-01-02T09:30:00Z"))
            .isInternational(false)
            .merchantCategory("GROCERIES")
            .build();

        ImmutableUserRiskProfile riskProfile = ImmutableUserRiskProfile.createNew("user-safe")
            .toBuilder()
            .totalTransactions(120)
            .averageTransactionAmount(110)
            .overallRiskScore(0.12)
            .lastTransactionDate(Instant.parse("2025-01-01T18:45:00Z"))
            .build();
    

        MerchantCategoryFrequency merchantFrequency = MerchantCategoryFrequency.createNew("user-safe")
            .toBuilder()
            .categoryFrequencies(Map.of("GROCERIES", 18, "DINING", 12))
            .build();

        List<FraudRule.FraudViolation> violations = List.of();

        when(fraudRuleEngine.evaluateTransaction(any())).thenReturn(Mono.just(violations));
        when(fraudRuleEngine.calculateFraudProbability(violations)).thenReturn(0.05);
        when(fraudRuleEngine.determineAction(0.05)).thenReturn(FraudRuleEngine.FraudAction.APPROVE);

        FraudDetectionService.FraudAssessment assessment = fraudDetectionService
            .assessFraud(transaction, riskProfile, merchantFrequency)
            .block();

            assertNotNull(assessment);
            assertEquals(transaction.getId(), assessment.transactionId());
            assertEquals(0.05, assessment.fraudProbability());
            assertFalse(assessment.shouldBlock());
            assertFalse(assessment.needsReview());
            assertEquals("", assessment.getViolationSummary());
    }
}