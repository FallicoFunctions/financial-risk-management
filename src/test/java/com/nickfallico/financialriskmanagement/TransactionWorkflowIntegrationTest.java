package com.nickfallico.financialriskmanagement;

import com.nickfallico.financialriskmanagement.dto.TransactionDTO;
import com.nickfallico.financialriskmanagement.model.Transactions;
import com.nickfallico.financialriskmanagement.service.TransactionRiskWorkflow;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
            .transactionType(Transactions.TransactionType.PURCHASE)
            .merchantCategory("GROCERIES")
            .isInternational(false)
            .build();

        StepVerifier.create(transactionRiskWorkflow.processTransaction(
            Transactions.builder()
                .userId(transactionDTO.getUserId())
                .amount(transactionDTO.getAmount())
                .currency(transactionDTO.getCurrency())
                .transactionType(transactionDTO.getTransactionType())
                .merchantCategory(transactionDTO.getMerchantCategory())
                .isInternational(transactionDTO.getIsInternational())
                .createdAt(Instant.now())
                .build()
        ))
        .expectNextMatches(transaction -> {
            assertNotNull(transaction.getId());
            assertEquals("user123", transaction.getUserId());
            return true;
        })
        .verifyComplete();
    }

    @Test
    void testHighRiskTransactionProcessing() {
        // High-risk transaction: large amount, gambling, international
        TransactionDTO highRiskTransaction = TransactionDTO.builder()
            .userId("user456")
            .amount(BigDecimal.valueOf(50000))
            .currency("USD")
            .transactionType(Transactions.TransactionType.PURCHASE)
            .merchantCategory("GAMBLING")
            .isInternational(true)
            .build();

        // Fire-and-forget architecture: transaction should succeed immediately
        // Fraud detection happens asynchronously in background
        StepVerifier.create(transactionRiskWorkflow.processTransaction(
            Transactions.builder()
                .userId(highRiskTransaction.getUserId())
                .amount(highRiskTransaction.getAmount())
                .currency(highRiskTransaction.getCurrency())
                .transactionType(highRiskTransaction.getTransactionType())
                .merchantCategory(highRiskTransaction.getMerchantCategory())
                .isInternational(highRiskTransaction.getIsInternational())
                .createdAt(Instant.now())
                .build()
        ))
        .expectNextMatches(transaction -> {
            assertNotNull(transaction.getId());
            assertEquals("user456", transaction.getUserId());
            assertEquals(0, transaction.getAmount().compareTo(BigDecimal.valueOf(50000)));
            assertEquals("GAMBLING", transaction.getMerchantCategory());
            return true;
        })
        .verifyComplete();
        
        // Note: Fraud detection runs asynchronously and will publish
        // FraudDetected and TransactionBlocked events in the background
    }
}