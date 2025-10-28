package com.nickfallico.financialriskmanagement;

import com.nickfallico.financialriskmanagement.ml.FraudFeatureExtractor;
import com.nickfallico.financialriskmanagement.model.Transaction;
import com.nickfallico.financialriskmanagement.model.UserRiskProfile;
import com.nickfallico.financialriskmanagement.service.FraudDetectionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class FraudDetectionServiceTest {

    private FraudDetectionService fraudDetectionService;

    @Mock
    private FraudFeatureExtractor fraudFeatureExtractor;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        fraudDetectionService = new FraudDetectionService(fraudFeatureExtractor);
    }

    @Test
    void testHighAmountTransaction() {
        Transaction highAmountTransaction = Transaction.builder()
            .amount(BigDecimal.valueOf(15000))
            .isInternational(false)
            .merchantCategory("ELECTRONICS")
            .createdAt(Instant.now())
            .build();

        UserRiskProfile profile = new UserRiskProfile();
        profile.setTotalTransactions(20);
        profile.getMerchantCategoryFrequency().put("ELECTRONICS", 5);

        when(fraudFeatureExtractor.extractFeatures(highAmountTransaction, profile))
            .thenReturn(Arrays.asList(1.0, 0.5, 0.2, 0.3, 0.2));

        boolean isPotentialFraud = fraudDetectionService.isPotentialFraud(highAmountTransaction, profile);

        assertTrue(isPotentialFraud, "High amount transaction should be flagged as potential fraud");
    }

    @Test
    void testInternationalTransaction() {
        Transaction internationalTransaction = Transaction.builder()
            .amount(BigDecimal.valueOf(5000))
            .isInternational(true)
            .merchantCategory("TRAVEL")
            .createdAt(Instant.now())
            .build();

        UserRiskProfile profile = new UserRiskProfile();
        profile.setTotalTransactions(5);
        profile.getMerchantCategoryFrequency().put("TRAVEL", 1);

        boolean isPotentialFraud = fraudDetectionService.isPotentialFraud(internationalTransaction, profile);

        assertTrue(isPotentialFraud, "International transaction with low transaction history should be flagged");
    }

    @Test
    void testLowRiskTransaction() {
        Transaction normalTransaction = Transaction.builder()
            .amount(BigDecimal.valueOf(100))
            .isInternational(false)
            .merchantCategory("GROCERIES")
            .createdAt(Instant.now())
            .build();

        UserRiskProfile profile = new UserRiskProfile();
        profile.setTotalTransactions(50);
        profile.getMerchantCategoryFrequency().put("GROCERIES", 20);

        boolean isPotentialFraud = fraudDetectionService.isPotentialFraud(normalTransaction, profile);

        assertFalse(isPotentialFraud, "Normal transaction should not be flagged as fraud");
    }

    @Test
    void testHighRiskMerchantCategory() {
        Transaction riskyCategoryTransaction = Transaction.builder()
            .amount(BigDecimal.valueOf(500))
            .isInternational(false)
            .merchantCategory("GAMBLING")
            .createdAt(Instant.now())
            .build();

        UserRiskProfile profile = new UserRiskProfile();
        profile.setTotalTransactions(10);
        profile.getMerchantCategoryFrequency().put("GAMBLING", 1);

        boolean isPotentialFraud = fraudDetectionService.isPotentialFraud(riskyCategoryTransaction, profile);

        assertTrue(isPotentialFraud, "Gambling transaction should be flagged as potential fraud");
    }

    @Test
    void testUnusualTransactionTime() {
        Transaction lateNightTransaction = Transaction.builder()
            .amount(BigDecimal.valueOf(300))
            .isInternational(false)
            .merchantCategory("ONLINE_SHOPPING")
            .createdAt(Instant.now().plusSeconds(3 * 60 * 60))  // 3 hours ahead to simulate late night
            .build();

        UserRiskProfile profile = new UserRiskProfile();
        profile.setTotalTransactions(30);
        profile.getMerchantCategoryFrequency().put("ONLINE_SHOPPING", 5);

        boolean isPotentialFraud = fraudDetectionService.isPotentialFraud(lateNightTransaction, profile);

        assertTrue(isPotentialFraud, "Late-night transaction should be flagged as potential fraud");
    }

    @Test
    void testVeryLowTransactionFrequency() {
        Transaction transaction = Transaction.builder()
            .amount(BigDecimal.valueOf(1000))
            .isInternational(false)
            .merchantCategory("ELECTRONICS")
            .createdAt(Instant.now())
            .build();

        UserRiskProfile profile = new UserRiskProfile();
        profile.setTotalTransactions(2);
        profile.getMerchantCategoryFrequency().put("ELECTRONICS", 1);

        when(fraudFeatureExtractor.extractFeatures(transaction, profile))
            .thenReturn(Arrays.asList(0.6, 0.7, 0.5, 0.9, 0.2));

        boolean isPotentialFraud = fraudDetectionService.isPotentialFraud(transaction, profile);

        assertTrue(isPotentialFraud, "Very low transaction frequency should increase fraud risk");
    }

    @Test
    void testMultiRiskFactorTransaction() {
        Transaction multiRiskTransaction = Transaction.builder()
            .amount(BigDecimal.valueOf(8000))
            .isInternational(true)
            .merchantCategory("CRYPTO")
            .createdAt(Instant.now().plusSeconds(22 * 60 * 60))  // Late evening
            .build();

        UserRiskProfile profile = new UserRiskProfile();
        profile.setTotalTransactions(15);
        profile.getMerchantCategoryFrequency().put("CRYPTO", 2);

        when(fraudFeatureExtractor.extractFeatures(multiRiskTransaction, profile))
            .thenReturn(Arrays.asList(0.8, 1.0, 1.0, 0.8, 0.7));

        boolean isPotentialFraud = fraudDetectionService.isPotentialFraud(multiRiskTransaction, profile);

        assertTrue(isPotentialFraud, "Multiple high-risk factors should flag transaction as potential fraud");
    }

    @Test
    void testBoundaryAmountTransaction() {
        Transaction boundaryTransaction = Transaction.builder()
            .amount(BigDecimal.valueOf(9999.99))  // Just below high-risk threshold
            .isInternational(false)
            .merchantCategory("ELECTRONICS")
            .createdAt(Instant.now())
            .build();

        UserRiskProfile profile = new UserRiskProfile();
        profile.setTotalTransactions(40);
        profile.getMerchantCategoryFrequency().put("ELECTRONICS", 10);

        boolean isPotentialFraud = fraudDetectionService.isPotentialFraud(boundaryTransaction, profile);

        assertFalse(isPotentialFraud, "Transaction just below high-risk threshold should not be flagged");
    }

    @Test
    void testComplexRiskProfileTransaction() {
        Transaction complexTransaction = Transaction.builder()
            .amount(BigDecimal.valueOf(5000))
            .isInternational(false)
            .merchantCategory("CRYPTO")
            .createdAt(Instant.now())
            .build();

        UserRiskProfile profile = new UserRiskProfile();
        profile.setTotalTransactions(100);
        profile.getMerchantCategoryFrequency().put("CRYPTO", 3);
        profile.setOverallRiskScore(0.7);

        when(fraudFeatureExtractor.extractFeatures(complexTransaction, profile))
            .thenReturn(Arrays.asList(0.7, 0.9, 0.3, 0.5, 0.2));

        boolean isPotentialFraud = fraudDetectionService.isPotentialFraud(complexTransaction, profile);

        assertTrue(isPotentialFraud, "Complex risk profile with high-risk merchant category should flag transaction");
    }
}