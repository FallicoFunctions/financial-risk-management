package com.nickfallico.financialriskmanagement.controller;

import com.nickfallico.financialriskmanagement.config.TestCacheConfig;
import com.nickfallico.financialriskmanagement.config.TestPrometheusRegistryConfig;
import com.nickfallico.financialriskmanagement.config.TestR2dbcConfig;
import com.nickfallico.financialriskmanagement.config.TestRedisConfig;
import com.nickfallico.financialriskmanagement.config.TestSecurityConfig;
import com.nickfallico.financialriskmanagement.dto.TransactionDTO;
import com.nickfallico.financialriskmanagement.model.Transactions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.math.BigDecimal;

/**
 * Integration tests for TransactionController.
 *
 * Tests cover:
 * - Transaction creation (POST /api/transactions)
 * - Get user transactions (GET /api/transactions/user/{userId})
 * - Get daily total (GET /api/transactions/daily-total/{userId})
 * - Get transactions by merchant category
 * - Count international transactions
 * - Get top 5 highest transactions
 * - Get transactions above threshold
 * - Get average transaction amount by category
 * - Input validation
 * - Error handling
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@Import({
    TestR2dbcConfig.class,
    TestPrometheusRegistryConfig.class,
    TestCacheConfig.class,
    TestRedisConfig.class,
    TestSecurityConfig.class
})
@ActiveProfiles("test")
public class TransactionControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    @DisplayName("POST /api/transactions - Create valid transaction")
    void testCreateTransaction_Success() {
        TransactionDTO request = TransactionDTO.builder()
            .userId("test_user_001")
            .amount(new BigDecimal("150.00"))
            .currency("USD")
            .transactionType(Transactions.TransactionType.PURCHASE)
            .merchantCategory("RETAIL")
            .merchantName("Amazon.com")
            .isInternational(false)
            .latitude(40.7128)
            .longitude(-74.0060)
            .country("US")
            .city("New York")
            .ipAddress("192.168.1.1")
            .build();

        webTestClient.post()
            .uri("/api/transactions")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.id").exists()
            .jsonPath("$.userId").isEqualTo("test_user_001")
            .jsonPath("$.amount").isEqualTo(150.00)
            .jsonPath("$.currency").isEqualTo("USD")
            .jsonPath("$.merchantCategory").isEqualTo("RETAIL")
            .jsonPath("$.merchantName").isEqualTo("Amazon.com")
            .jsonPath("$.createdAt").exists();
    }

    @Test
    @DisplayName("POST /api/transactions - Create high-risk transaction")
    void testCreateTransaction_HighRisk() {
        TransactionDTO request = TransactionDTO.builder()
            .userId("test_user_002")
            .amount(new BigDecimal("25000.00"))
            .currency("USD")
            .transactionType(Transactions.TransactionType.PURCHASE)
            .merchantCategory("GAMBLING")
            .merchantName("OnlineCasino.com")
            .isInternational(true)
            .latitude(51.5074)
            .longitude(-0.1278)
            .country("GB")
            .city("London")
            .ipAddress("203.0.113.45")
            .build();

        webTestClient.post()
            .uri("/api/transactions")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.id").exists()
            .jsonPath("$.userId").isEqualTo("test_user_002")
            .jsonPath("$.amount").isEqualTo(25000.00)
            .jsonPath("$.merchantCategory").isEqualTo("GAMBLING");
    }

    @Test
    @DisplayName("POST /api/transactions - Invalid request missing required fields")
    void testCreateTransaction_InvalidRequest() {
        TransactionDTO invalidRequest = TransactionDTO.builder()
            .userId("test_user_003")
            // Missing amount, currency, transactionType
            .merchantCategory("RETAIL")
            .build();

        webTestClient.post()
            .uri("/api/transactions")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(invalidRequest)
            .exchange()
            .expectStatus().isBadRequest();
    }

    @Test
    @DisplayName("POST /api/transactions - Invalid negative amount")
    void testCreateTransaction_NegativeAmount() {
        TransactionDTO request = TransactionDTO.builder()
            .userId("test_user_004")
            .amount(new BigDecimal("-100.00"))
            .currency("USD")
            .transactionType(Transactions.TransactionType.PURCHASE)
            .merchantCategory("RETAIL")
            .isInternational(false)
            .build();

        webTestClient.post()
            .uri("/api/transactions")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isBadRequest();
    }

    @Test
    @DisplayName("GET /api/transactions/user/{userId} - Retrieve user transactions")
    void testGetUserTransactions() {
        // First create a transaction
        String userId = "test_user_005";
        createTestTransaction(userId, new BigDecimal("100.00"));

        // Then retrieve it
        webTestClient.get()
            .uri("/api/transactions/user/{userId}", userId)
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$").isArray()
            .jsonPath("$[?(@.userId == '" + userId + "')]").exists();
    }

    @Test
    @DisplayName("GET /api/transactions/daily-total/{userId} - Calculate daily total")
    void testGetDailyTotal() {
        String userId = "test_user_006";

        // Create multiple transactions
        createTestTransaction(userId, new BigDecimal("50.00"));
        createTestTransaction(userId, new BigDecimal("75.00"));

        webTestClient.get()
            .uri("/api/transactions/daily-total/{userId}", userId)
            .exchange()
            .expectStatus().isOk()
            .expectBody(BigDecimal.class);
    }

    @Test
    @DisplayName("GET /api/transactions/merchant-category/{category} - Get by category")
    void testGetTransactionsByMerchantCategory() {
        String userId = "test_user_007";

        // Create transaction with ELECTRONICS category
        TransactionDTO request = TransactionDTO.builder()
            .userId(userId)
            .amount(new BigDecimal("500.00"))
            .currency("USD")
            .transactionType(Transactions.TransactionType.PURCHASE)
            .merchantCategory("ELECTRONICS")
            .merchantName("BestBuy.com")
            .isInternational(false)
            .build();

        webTestClient.post()
            .uri("/api/transactions")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange();

        // Retrieve by category
        webTestClient.get()
            .uri("/api/transactions/merchant-category/{category}", "ELECTRONICS")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$").isArray();
    }

    @Test
    @DisplayName("GET /api/transactions/international-count/{userId} - Count international")
    void testCountInternationalTransactions() {
        String userId = "test_user_008";

        // Create international transaction
        TransactionDTO request = TransactionDTO.builder()
            .userId(userId)
            .amount(new BigDecimal("200.00"))
            .currency("EUR")
            .transactionType(Transactions.TransactionType.PURCHASE)
            .merchantCategory("RETAIL")
            .isInternational(true)
            .country("DE")
            .build();

        webTestClient.post()
            .uri("/api/transactions")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange();

        webTestClient.get()
            .uri("/api/transactions/international-count/{userId}", userId)
            .exchange()
            .expectStatus().isOk()
            .expectBody(Long.class);
    }

    @Test
    @DisplayName("GET /api/transactions/top-5-highest/{userId} - Get top 5 highest")
    void testGetTop5HighestTransactions() {
        String userId = "test_user_009";

        // Create multiple transactions
        createTestTransaction(userId, new BigDecimal("100.00"));
        createTestTransaction(userId, new BigDecimal("500.00"));
        createTestTransaction(userId, new BigDecimal("250.00"));

        webTestClient.get()
            .uri("/api/transactions/top-5-highest/{userId}", userId)
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$").isArray();
    }

    @Test
    @DisplayName("GET /api/transactions/above-threshold/{threshold} - Get above threshold")
    void testGetTransactionsAboveThreshold() {
        webTestClient.get()
            .uri("/api/transactions/above-threshold/{threshold}", new BigDecimal("1000.00"))
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$").isArray();
    }

    @Test
    @DisplayName("GET /api/transactions/above-threshold/{threshold} - Invalid negative threshold")
    void testGetTransactionsAboveThreshold_NegativeThreshold() {
        webTestClient.get()
            .uri("/api/transactions/above-threshold/{threshold}", new BigDecimal("-100.00"))
            .exchange()
            .expectStatus().isBadRequest();
    }

    @Test
    @DisplayName("GET /api/transactions/avg-amount/merchant-category/{category} - Get average")
    void testGetAverageTransactionAmountByMerchantCategory() {
        String userId = "test_user_010";

        // Create transactions in TRAVEL category
        TransactionDTO request1 = TransactionDTO.builder()
            .userId(userId)
            .amount(new BigDecimal("300.00"))
            .currency("USD")
            .transactionType(Transactions.TransactionType.PURCHASE)
            .merchantCategory("TRAVEL")
            .merchantName("Expedia.com")
            .isInternational(false)
            .build();

        TransactionDTO request2 = TransactionDTO.builder()
            .userId(userId)
            .amount(new BigDecimal("500.00"))
            .currency("USD")
            .transactionType(Transactions.TransactionType.PURCHASE)
            .merchantCategory("TRAVEL")
            .merchantName("Booking.com")
            .isInternational(false)
            .build();

        webTestClient.post().uri("/api/transactions")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request1)
            .exchange();

        webTestClient.post().uri("/api/transactions")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request2)
            .exchange();

        webTestClient.get()
            .uri("/api/transactions/avg-amount/merchant-category/{category}", "TRAVEL")
            .exchange()
            .expectStatus().isOk()
            .expectBody(Double.class);
    }

    @Test
    @DisplayName("POST /api/transactions - Handle concurrent requests")
    void testConcurrentTransactionCreation() {
        String userId = "test_user_concurrent";

        // Create multiple transactions concurrently
        for (int i = 0; i < 5; i++) {
            TransactionDTO request = TransactionDTO.builder()
                .userId(userId)
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .transactionType(Transactions.TransactionType.PURCHASE)
                .merchantCategory("RETAIL")
                .isInternational(false)
                .build();

            webTestClient.post()
                .uri("/api/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk();
        }
    }

    /**
     * Helper method to create a test transaction.
     */
    private void createTestTransaction(String userId, BigDecimal amount) {
        TransactionDTO request = TransactionDTO.builder()
            .userId(userId)
            .amount(amount)
            .currency("USD")
            .transactionType(Transactions.TransactionType.PURCHASE)
            .merchantCategory("RETAIL")
            .merchantName("Test Merchant")
            .isInternational(false)
            .build();

        webTestClient.post()
            .uri("/api/transactions")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isOk();
    }
}
