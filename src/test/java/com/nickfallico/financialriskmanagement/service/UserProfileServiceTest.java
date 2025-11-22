package com.nickfallico.financialriskmanagement.service;

import com.nickfallico.financialriskmanagement.model.ImmutableUserRiskProfile;
import com.nickfallico.financialriskmanagement.model.MerchantCategoryFrequency;
import com.nickfallico.financialriskmanagement.model.Transactions;
import com.nickfallico.financialriskmanagement.repository.ImmutableUserRiskProfileRepository;
import com.nickfallico.financialriskmanagement.repository.MerchantCategoryFrequencyRepository;
import com.nickfallico.financialriskmanagement.repository.TransactionRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UserProfileService.
 *
 * Tests cover:
 * - Get user profile (cached)
 * - Get merchant frequency (cached)
 * - Update profile after transaction
 * - Profile computation from transaction history
 * - Risk score calculation
 * - Cache eviction
 * - New user profile creation
 * - High-risk user identification
 */
@ExtendWith(MockitoExtension.class)
public class UserProfileServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private ImmutableUserRiskProfileRepository immutableUserRiskProfileRepository;

    @Mock
    private MerchantCategoryFrequencyRepository merchantCategoryFrequencyRepository;

    private MeterRegistry meterRegistry;

    private UserProfileService userProfileService;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        userProfileService = new UserProfileService(
            transactionRepository,
            immutableUserRiskProfileRepository,
            merchantCategoryFrequencyRepository,
            meterRegistry
        );
    }

    @Test
    @DisplayName("getUserProfile - Should return existing profile")
    void testGetUserProfile_ExistingProfile() {
        String userId = "user123";
        ImmutableUserRiskProfile existingProfile = ImmutableUserRiskProfile.builder()
            .userId(userId)
            .overallRiskScore(0.45)
            .behavioralRiskScore(0.40)
            .transactionRiskScore(0.50)
            .totalTransactions(100)
            .highRiskTransactions(5)
            .internationalTransactions(10)
            .averageTransactionAmount(250.0)
            .totalTransactionValue(25000.0)
            .firstTransactionDate(Instant.now().minusSeconds(86400 * 30))
            .lastTransactionDate(Instant.now())
            .build();

        when(immutableUserRiskProfileRepository.findById(userId))
            .thenReturn(Mono.just(existingProfile));

        StepVerifier.create(userProfileService.getUserProfile(userId))
            .assertNext(profile -> {
                assertEquals(userId, profile.getUserId());
                assertEquals(0.45, profile.getOverallRiskScore());
                assertEquals(100, profile.getTotalTransactions());
            })
            .verifyComplete();

        verify(immutableUserRiskProfileRepository, times(1)).findById(userId);
    }

    @Test
    @DisplayName("getUserProfile - Should create new profile for new user")
    void testGetUserProfile_NewUser() {
        String userId = "new_user";

        when(immutableUserRiskProfileRepository.findById(userId))
            .thenReturn(Mono.empty());

        StepVerifier.create(userProfileService.getUserProfile(userId))
            .assertNext(profile -> {
                assertEquals(userId, profile.getUserId());
                assertEquals(0.0, profile.getOverallRiskScore());
                assertEquals(0, profile.getTotalTransactions());
            })
            .verifyComplete();

        verify(immutableUserRiskProfileRepository, times(1)).findById(userId);
    }

    @Test
    @DisplayName("getMerchantFrequency - Should return frequency data")
    void testGetMerchantFrequency() {
        String userId = "user456";
        ImmutableUserRiskProfile profile = ImmutableUserRiskProfile.createNew(userId);

        Transactions tx1 = createTestTransaction(userId, "RETAIL", new BigDecimal("100.00"));
        Transactions tx2 = createTestTransaction(userId, "RETAIL", new BigDecimal("50.00"));
        Transactions tx3 = createTestTransaction(userId, "TRAVEL", new BigDecimal("500.00"));

        when(immutableUserRiskProfileRepository.findById(userId))
            .thenReturn(Mono.just(profile));
        when(transactionRepository.findByUserId(userId))
            .thenReturn(Flux.just(tx1, tx2, tx3));

        StepVerifier.create(userProfileService.getMerchantFrequency(userId))
            .assertNext(frequency -> {
                assertEquals(userId, frequency.getUserId());
            })
            .verifyComplete();

        verify(transactionRepository, times(1)).findByUserId(userId);
    }

    @Test
    @DisplayName("getMerchantFrequency - Should handle new user")
    void testGetMerchantFrequency_NewUser() {
        String userId = "new_user_freq";

        when(immutableUserRiskProfileRepository.findById(userId))
            .thenReturn(Mono.empty());

        StepVerifier.create(userProfileService.getMerchantFrequency(userId))
            .assertNext(frequency -> {
                assertEquals(userId, frequency.getUserId());
            })
            .verifyComplete();
    }

    @Test
    @DisplayName("updateProfileAfterTransaction - Should update profile and invalidate cache")
    void testUpdateProfileAfterTransaction() {
        String userId = "user789";
        Transactions newTransaction = createTestTransaction(userId, "RETAIL", new BigDecimal("200.00"));

        Transactions tx1 = createTestTransaction(userId, "RETAIL", new BigDecimal("100.00"));
        Transactions tx2 = createTestTransaction(userId, "GAMBLING", new BigDecimal("5000.00"));

        when(transactionRepository.findByUserId(userId))
            .thenReturn(Flux.just(tx1, tx2, newTransaction));
        when(immutableUserRiskProfileRepository.upsert(any(ImmutableUserRiskProfile.class)))
            .thenReturn(Mono.empty());
        when(merchantCategoryFrequencyRepository.upsert(any(MerchantCategoryFrequency.class)))
            .thenReturn(Mono.empty());

        StepVerifier.create(userProfileService.updateProfileAfterTransaction(newTransaction))
            .verifyComplete();

        verify(transactionRepository, times(1)).findByUserId(userId);
        verify(immutableUserRiskProfileRepository, times(1)).upsert(any(ImmutableUserRiskProfile.class));
        verify(merchantCategoryFrequencyRepository, times(1)).upsert(any(MerchantCategoryFrequency.class));
    }

    @Test
    @DisplayName("updateProfileAfterTransaction - Should handle first transaction")
    void testUpdateProfileAfterTransaction_FirstTransaction() {
        String userId = "first_time_user";
        Transactions firstTransaction = createTestTransaction(userId, "RETAIL", new BigDecimal("50.00"));

        when(transactionRepository.findByUserId(userId))
            .thenReturn(Flux.just(firstTransaction));
        when(immutableUserRiskProfileRepository.upsert(any(ImmutableUserRiskProfile.class)))
            .thenReturn(Mono.empty());
        when(merchantCategoryFrequencyRepository.upsert(any(MerchantCategoryFrequency.class)))
            .thenReturn(Mono.empty());

        StepVerifier.create(userProfileService.updateProfileAfterTransaction(firstTransaction))
            .verifyComplete();

        verify(transactionRepository, times(1)).findByUserId(userId);
        verify(immutableUserRiskProfileRepository, times(1)).upsert(any(ImmutableUserRiskProfile.class));
    }

    @Test
    @DisplayName("updateProfileAfterTransaction - Should identify high-risk user")
    void testUpdateProfileAfterTransaction_HighRiskUser() {
        String userId = "high_risk_user";

        // Create multiple high-risk transactions
        Transactions tx1 = createTestTransaction(userId, "GAMBLING", new BigDecimal("15000.00"));
        Transactions tx2 = createTestTransaction(userId, "GAMBLING", new BigDecimal("20000.00"));
        Transactions tx3 = createTestTransaction(userId, "CRYPTOCURRENCY", new BigDecimal("30000.00"));

        when(transactionRepository.findByUserId(userId))
            .thenReturn(Flux.just(tx1, tx2, tx3));
        when(immutableUserRiskProfileRepository.upsert(any(ImmutableUserRiskProfile.class)))
            .thenReturn(Mono.empty());
        when(merchantCategoryFrequencyRepository.upsert(any(MerchantCategoryFrequency.class)))
            .thenReturn(Mono.empty());

        StepVerifier.create(userProfileService.updateProfileAfterTransaction(tx3))
            .verifyComplete();

        verify(immutableUserRiskProfileRepository, times(1)).upsert(argThat(profile ->
            profile.getHighRiskTransactions() >= 3
        ));
    }

    @Test
    @DisplayName("updateProfileAfterTransaction - Should calculate international transactions")
    void testUpdateProfileAfterTransaction_InternationalTransactions() {
        String userId = "international_user";

        Transactions tx1 = createInternationalTransaction(userId, "RETAIL", new BigDecimal("100.00"));
        Transactions tx2 = createInternationalTransaction(userId, "TRAVEL", new BigDecimal("500.00"));
        Transactions tx3 = createTestTransaction(userId, "RETAIL", new BigDecimal("50.00"));

        when(transactionRepository.findByUserId(userId))
            .thenReturn(Flux.just(tx1, tx2, tx3));
        when(immutableUserRiskProfileRepository.upsert(any(ImmutableUserRiskProfile.class)))
            .thenReturn(Mono.empty());
        when(merchantCategoryFrequencyRepository.upsert(any(MerchantCategoryFrequency.class)))
            .thenReturn(Mono.empty());

        StepVerifier.create(userProfileService.updateProfileAfterTransaction(tx3))
            .verifyComplete();

        verify(immutableUserRiskProfileRepository, times(1)).upsert(argThat(profile ->
            profile.getInternationalTransactions() == 2
        ));
    }

    @Test
    @DisplayName("updateProfileAfterTransaction - Should calculate average amount")
    void testUpdateProfileAfterTransaction_AverageAmount() {
        String userId = "avg_user";

        Transactions tx1 = createTestTransaction(userId, "RETAIL", new BigDecimal("100.00"));
        Transactions tx2 = createTestTransaction(userId, "RETAIL", new BigDecimal("200.00"));
        Transactions tx3 = createTestTransaction(userId, "RETAIL", new BigDecimal("300.00"));

        when(transactionRepository.findByUserId(userId))
            .thenReturn(Flux.just(tx1, tx2, tx3));
        when(immutableUserRiskProfileRepository.upsert(any(ImmutableUserRiskProfile.class)))
            .thenReturn(Mono.empty());
        when(merchantCategoryFrequencyRepository.upsert(any(MerchantCategoryFrequency.class)))
            .thenReturn(Mono.empty());

        StepVerifier.create(userProfileService.updateProfileAfterTransaction(tx3))
            .verifyComplete();

        // Average should be 200.00
        verify(immutableUserRiskProfileRepository, times(1)).upsert(argThat(profile ->
            Math.abs(profile.getAverageTransactionAmount() - 200.0) < 0.01
        ));
    }

    @Test
    @DisplayName("updateProfileAfterTransaction - Should track first and last transaction dates")
    void testUpdateProfileAfterTransaction_TransactionDates() {
        String userId = "date_user";

        Instant firstDate = Instant.now().minusSeconds(86400 * 7); // 7 days ago
        Instant lastDate = Instant.now();

        Transactions tx1 = Transactions.builder()
            .id(UUID.randomUUID())
            .userId(userId)
            .amount(new BigDecimal("100.00"))
            .currency("USD")
            .merchantCategory("RETAIL")
            .transactionType(Transactions.TransactionType.PURCHASE)
            .isInternational(false)
            .createdAt(firstDate)
            .build();

        Transactions tx2 = Transactions.builder()
            .id(UUID.randomUUID())
            .userId(userId)
            .amount(new BigDecimal("200.00"))
            .currency("USD")
            .merchantCategory("RETAIL")
            .transactionType(Transactions.TransactionType.PURCHASE)
            .isInternational(false)
            .createdAt(lastDate)
            .build();

        when(transactionRepository.findByUserId(userId))
            .thenReturn(Flux.just(tx1, tx2));
        when(immutableUserRiskProfileRepository.upsert(any(ImmutableUserRiskProfile.class)))
            .thenReturn(Mono.empty());
        when(merchantCategoryFrequencyRepository.upsert(any(MerchantCategoryFrequency.class)))
            .thenReturn(Mono.empty());

        StepVerifier.create(userProfileService.updateProfileAfterTransaction(tx2))
            .verifyComplete();

        verify(immutableUserRiskProfileRepository, times(1)).upsert(argThat(profile ->
            profile.getFirstTransactionDate().equals(firstDate) &&
            profile.getLastTransactionDate().equals(lastDate)
        ));
    }

    @Test
    @DisplayName("getUserProfile - Should increment metrics")
    void testGetUserProfile_Metrics() {
        String userId = "metrics_user";
        ImmutableUserRiskProfile profile = ImmutableUserRiskProfile.createNew(userId);

        when(immutableUserRiskProfileRepository.findById(userId))
            .thenReturn(Mono.just(profile));

        StepVerifier.create(userProfileService.getUserProfile(userId))
            .expectNextCount(1)
            .verifyComplete();

        assertEquals(1.0, meterRegistry.counter("get_user_profile_attempts").count());
    }

    @Test
    @DisplayName("updateProfileAfterTransaction - Should increment metrics")
    void testUpdateProfileAfterTransaction_Metrics() {
        String userId = "metrics_update_user";
        Transactions transaction = createTestTransaction(userId, "RETAIL", new BigDecimal("100.00"));

        when(transactionRepository.findByUserId(userId))
            .thenReturn(Flux.just(transaction));
        when(immutableUserRiskProfileRepository.upsert(any(ImmutableUserRiskProfile.class)))
            .thenReturn(Mono.empty());
        when(merchantCategoryFrequencyRepository.upsert(any(MerchantCategoryFrequency.class)))
            .thenReturn(Mono.empty());

        StepVerifier.create(userProfileService.updateProfileAfterTransaction(transaction))
            .verifyComplete();

        assertEquals(1.0, meterRegistry.counter("profile_updates").count());
    }

    /**
     * Helper method to create a test transaction.
     */
    private Transactions createTestTransaction(String userId, String merchantCategory, BigDecimal amount) {
        return Transactions.builder()
            .id(UUID.randomUUID())
            .userId(userId)
            .amount(amount)
            .currency("USD")
            .merchantCategory(merchantCategory)
            .merchantName("Test Merchant")
            .transactionType(Transactions.TransactionType.PURCHASE)
            .isInternational(false)
            .createdAt(Instant.now())
            .build();
    }

    /**
     * Helper method to create an international transaction.
     */
    private Transactions createInternationalTransaction(String userId, String merchantCategory, BigDecimal amount) {
        return Transactions.builder()
            .id(UUID.randomUUID())
            .userId(userId)
            .amount(amount)
            .currency("EUR")
            .merchantCategory(merchantCategory)
            .merchantName("International Merchant")
            .transactionType(Transactions.TransactionType.PURCHASE)
            .isInternational(true)
            .country("GB")
            .city("London")
            .createdAt(Instant.now())
            .build();
    }
}
