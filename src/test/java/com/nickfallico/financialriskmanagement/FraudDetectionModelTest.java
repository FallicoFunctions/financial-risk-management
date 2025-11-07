package com.nickfallico.financialriskmanagement;

import com.nickfallico.financialriskmanagement.ml.FraudDetectionModel;
import com.nickfallico.financialriskmanagement.model.Transactions;
import com.nickfallico.financialriskmanagement.model.ImmutableUserRiskProfile;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("test")
@SpringBootTest(
    properties = {
        // 1. Don't try to spin up Redis-backed cache
        "spring.cache.type=NONE",
        // 2. Don't auto-configure Redis at all
        "spring.autoconfigure.exclude=" +
        "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration," +
        "org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration"
    }
)
public class FraudDetectionModelTest {

    @Autowired
    private FraudDetectionModel fraudDetectionModel;

    @Test
    void testHighRiskTransactionDetection() {
        Instant now = Instant.now();
        Transactions highRiskTransaction = Transactions.builder()
            .amount(BigDecimal.valueOf(50000))
            .isInternational(true)
            .merchantCategory("GAMBLING")
            .createdAt(Instant.now())
            .build();

        ImmutableUserRiskProfile riskProfile = ImmutableUserRiskProfile.createNew("user-high-risk")
                                                .toBuilder()
                                                .averageTransactionAmount(1000)
                                                .totalTransactions(5)
                                                .lastTransactionDate(now.minusSeconds(3600))
                                                .build();

        double fraudProbability = fraudDetectionModel.predictFraudProbability(highRiskTransaction, riskProfile);
        
        assertTrue(fraudProbability > 0.7, "High-risk Transactions should have high fraud probability");
    }

    @Test
    void testLowRiskTransactionDetection() {
        Instant now = Instant.now();
        Transactions lowRiskTransaction = Transactions.builder()
            .amount(BigDecimal.valueOf(50))
            .isInternational(false)
            .merchantCategory("GROCERIES")
            .createdAt(Instant.now())
            .build();

        ImmutableUserRiskProfile riskProfile = ImmutableUserRiskProfile.createNew("user-low-risk")
                                                .toBuilder()
                                                .averageTransactionAmount(75)
                                                .totalTransactions(100)
                                                .lastTransactionDate(now.minusSeconds(86400))
                                                .build();

        double fraudProbability = fraudDetectionModel.predictFraudProbability(lowRiskTransaction, riskProfile);
        
        assertTrue(fraudProbability < 0.3, "Low-risk Transactions should have low fraud probability");
    }
}