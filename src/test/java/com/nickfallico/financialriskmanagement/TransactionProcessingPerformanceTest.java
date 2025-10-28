package com.nickfallico.financialriskmanagement;

import com.nickfallico.financialriskmanagement.model.Transaction;
import com.nickfallico.financialriskmanagement.service.TransactionRiskWorkflow;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Flux;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
public class TransactionProcessingPerformanceTest {

    @Autowired
    private TransactionRiskWorkflow transactionRiskWorkflow;

    @Test
    void testHighConcurrencyTransactionProcessing() {
        int totalTransactions = 1000;
        long startTime = System.currentTimeMillis();

        Flux.range(0, totalTransactions)
            .flatMap(i -> transactionRiskWorkflow.processTransaction(
                Transaction.builder()
                    .id(UUID.randomUUID())
                    .userId("perf_user_" + i)
                    .amount(BigDecimal.valueOf(Math.random() * 1000))
                    .currency("USD")
                    .transactionType(Transaction.TransactionType.PURCHASE)
                    .merchantCategory(i % 2 == 0 ? "GROCERIES" : "ELECTRONICS")
                    .isInternational(false)
                    .createdAt(Instant.now())
                    .build()
            ), 100) // Concurrency limit
            .blockLast();

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // Ensure processing takes less than 10 seconds
        assertTrue(duration < 10000, "Transaction processing took too long: " + duration + "ms");
    }
}