package com.nickfallico.financialriskmanagement;

import com.nickfallico.financialriskmanagement.dto.TransactionDTO;
import com.nickfallico.financialriskmanagement.model.Transaction;
import com.nickfallico.financialriskmanagement.service.TransactionRiskWorkflow;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.Instant;

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
public class TransactionWorkflowIntegrationTest {

    @Autowired
    private TransactionRiskWorkflow transactionRiskWorkflow;

    @Test
    void testSuccessfulTransactionProcessing() {
        TransactionDTO transactionDTO = TransactionDTO.builder()
            .userId("user123")
            .amount(BigDecimal.valueOf(100.00))
            .currency("USD")
            .transactionType(Transaction.TransactionType.PURCHASE)
            .merchantCategory("GROCERIES")
            .isInternational(false)
            .build();

        StepVerifier.create(transactionRiskWorkflow.processTransaction(
            Transaction.builder()
                .userId(transactionDTO.getUserId())
                .amount(transactionDTO.getAmount())
                .currency(transactionDTO.getCurrency())
                .transactionType(transactionDTO.getTransactionType())
                .merchantCategory(transactionDTO.getMerchantCategory())
                .isInternational(transactionDTO.getIsInternational())
                .createdAt(Instant.now())
                .build()
        ))
        .expectNextMatches(transaction -> transaction.getUserId().equals("user123"))
        .verifyComplete();
    }

    @Test
    void testHighRiskTransactionRejection() {
        TransactionDTO highRiskTransaction = TransactionDTO.builder()
            .userId("user456")
            .amount(BigDecimal.valueOf(50000))
            .currency("USD")
            .transactionType(Transaction.TransactionType.PURCHASE)
            .merchantCategory("GAMBLING")
            .isInternational(true)
            .build();

        StepVerifier.create(transactionRiskWorkflow.processTransaction(
            Transaction.builder()
                .userId(highRiskTransaction.getUserId())
                .amount(highRiskTransaction.getAmount())
                .currency(highRiskTransaction.getCurrency())
                .transactionType(highRiskTransaction.getTransactionType())
                .merchantCategory(highRiskTransaction.getMerchantCategory())
                .isInternational(highRiskTransaction.getIsInternational())
                .createdAt(Instant.now())
                .build()
        ))
        .expectError()
        .verify();
    }
}