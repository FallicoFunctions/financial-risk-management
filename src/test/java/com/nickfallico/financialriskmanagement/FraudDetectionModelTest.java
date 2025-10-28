package com.nickfallico.financialriskmanagement;

import com.nickfallico.financialriskmanagement.ml.FraudDetectionModel;
import com.nickfallico.financialriskmanagement.model.Transaction;
import com.nickfallico.financialriskmanagement.model.UserRiskProfile;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

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
        Transaction highRiskTransaction = Transaction.builder()
            .amount(BigDecimal.valueOf(50000))
            .isInternational(true)
            .merchantCategory("GAMBLING")
            .createdAt(Instant.now())
            .build();

        UserRiskProfile riskProfile = new UserRiskProfile();
        riskProfile.setTotalTransactions(5);

        double fraudProbability = fraudDetectionModel.predictFraudProbability(highRiskTransaction, riskProfile);
        
        assertTrue(fraudProbability > 0.7, "High-risk transaction should have high fraud probability");
    }

    @Test
    void testLowRiskTransactionDetection() {
        Transaction lowRiskTransaction = Transaction.builder()
            .amount(BigDecimal.valueOf(50))
            .isInternational(false)
            .merchantCategory("GROCERIES")
            .createdAt(Instant.now())
            .build();

        UserRiskProfile riskProfile = new UserRiskProfile();
        riskProfile.setTotalTransactions(100);

        double fraudProbability = fraudDetectionModel.predictFraudProbability(lowRiskTransaction, riskProfile);
        
        assertTrue(fraudProbability < 0.3, "Low-risk transaction should have low fraud probability");
    }
}