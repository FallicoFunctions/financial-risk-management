package com.nickfallico.financialriskmanagement.ml;

import com.nickfallico.financialriskmanagement.config.TestR2dbcConfig;
import com.nickfallico.financialriskmanagement.model.ImmutableUserRiskProfile;
import com.nickfallico.financialriskmanagement.model.MerchantCategoryFrequency;
import com.nickfallico.financialriskmanagement.model.Transactions;
import com.nickfallico.financialriskmanagement.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for advanced fraud detection rules.
 * Tests velocity checks, geographic anomalies, impossible travel, and amount spikes.
 */
@SpringBootTest
@Import(TestR2dbcConfig.class)
@ActiveProfiles("test")
class AdvancedFraudDetectionIntegrationTest {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private VelocityRule velocityRule;

    @Autowired
    private GeographicAnomalyRule geographicAnomalyRule;

    @Autowired
    private ImpossibleTravelRule impossibleTravelRule;

    @Autowired
    private AmountSpikeRule amountSpikeRule;

    @BeforeEach
    void setUp() {
        // Clean up test data
        transactionRepository.deleteAll().block();
    }

    // ========== Velocity Rule Tests ==========

    @Test
    void testVelocityRule_DetectsExcessiveTransactionsIn5Minutes() {
        String userId = "velocity-user-1";
        Instant now = Instant.now();

        // Create 4 transactions in 5 minutes (exceeds limit of 3)
        for (int i = 0; i < 4; i++) {
            Transactions tx = createTransaction(userId, BigDecimal.valueOf(100), now.minusSeconds(i * 60));
            transactionRepository.save(tx).block();
        }

        // Current transaction (5th one)
        Transactions currentTx = createTransaction(userId, BigDecimal.valueOf(100), now);
        var profile = ImmutableUserRiskProfile.createNew(userId);
        var context = new FraudRule.FraudEvaluationContext(currentTx, profile, null);

        // Evaluate
        Optional<FraudRule.FraudViolation> violation = velocityRule.evaluate(context);

        // Assert
        assertThat(violation).isPresent();
        assertThat(violation.get().ruleId()).isEqualTo("VELOCITY_5MIN");
        assertThat(violation.get().riskScore()).isGreaterThan(0.85);
    }

    @Test
    void testVelocityRule_PassesWithNormalFrequency() {
        String userId = "velocity-user-2";
        Instant now = Instant.now();

        // Create 2 transactions in 5 minutes (within limit of 3)
        for (int i = 0; i < 2; i++) {
            Transactions tx = createTransaction(userId, BigDecimal.valueOf(100), now.minusSeconds(i * 120));
            transactionRepository.save(tx).block();
        }

        // Current transaction
        Transactions currentTx = createTransaction(userId, BigDecimal.valueOf(100), now);
        var profile = ImmutableUserRiskProfile.createNew(userId);
        var context = new FraudRule.FraudEvaluationContext(currentTx, profile, null);

        // Evaluate
        Optional<FraudRule.FraudViolation> violation = velocityRule.evaluate(context);

        // Assert
        assertThat(violation).isEmpty();
    }

    // ========== Geographic Anomaly Rule Tests ==========

    @Test
    void testGeographicAnomalyRule_DetectsNewCountryForNewUser() {
        String userId = "geo-user-1";
        Instant now = Instant.now();

        // No previous transactions (new user)
        
        // Current transaction from France (new country)
        Transactions currentTx = createTransactionWithLocation(
            userId, 
            BigDecimal.valueOf(100), 
            now,
            48.8566,  // Paris latitude
            2.3522,   // Paris longitude
            "FR",     // France
            "Paris"
        );

        var profile = ImmutableUserRiskProfile.createNew(userId);
        var context = new FraudRule.FraudEvaluationContext(currentTx, profile, null);

        // Evaluate
        Optional<FraudRule.FraudViolation> violation = geographicAnomalyRule.evaluate(context);

        // Assert
        assertThat(violation).isPresent();
        assertThat(violation.get().ruleId()).isEqualTo("GEOGRAPHIC_NEW_USER_NEW_COUNTRY");
        assertThat(violation.get().riskScore()).isGreaterThanOrEqualTo(0.75);
    }

    @Test
    void testGeographicAnomalyRule_DetectsCountryHopping() {
        String userId = "geo-user-2";
        Instant now = Instant.now();

        // Create transactions in 6 different countries (exceeds limit of 5)
        String[] countries = {"US", "GB", "FR", "DE", "JP", "AU"};
        for (int i = 0; i < countries.length; i++) {
            Transactions tx = createTransactionWithLocation(
                userId,
                BigDecimal.valueOf(100),
                now.minus(Duration.ofDays(i)),
                40.0 + i,
                -70.0 + i,
                countries[i],
                "City" + i
            );
            transactionRepository.save(tx).block();
        }

        // Current transaction
        Transactions currentTx = createTransactionWithLocation(
            userId, 
            BigDecimal.valueOf(100), 
            now,
            40.0,
            -70.0,
            "CA",  // Canada - 7th country
            "Toronto"
        );

        var profile = createEstablishedProfile(userId);
        var context = new FraudRule.FraudEvaluationContext(currentTx, profile, null);

        // Evaluate
        Optional<FraudRule.FraudViolation> violation = geographicAnomalyRule.evaluate(context);

        // Assert
        assertThat(violation).isPresent();
        assertThat(violation.get().ruleId()).isEqualTo("GEOGRAPHIC_COUNTRY_HOPPING");
        assertThat(violation.get().riskScore()).isGreaterThanOrEqualTo(0.65);
    }

    // ========== Impossible Travel Rule Tests ==========

    @Test
    void testImpossibleTravelRule_DetectsImpossibleTravel() {
        String userId = "travel-user-1";
        Instant now = Instant.now();

        // Previous transaction in New York
        Transactions previousTx = createTransactionWithLocation(
            userId,
            BigDecimal.valueOf(100),
            now.minus(Duration.ofHours(2)),  // 2 hours ago
            40.7128,   // NYC latitude
            -74.0060,  // NYC longitude
            "US",
            "New York"
        );
        transactionRepository.save(previousTx).block();

        // Current transaction in London (5,570 km away, 2 hours later = impossible)
        Transactions currentTx = createTransactionWithLocation(
            userId,
            BigDecimal.valueOf(100),
            now,
            51.5074,   // London latitude
            -0.1278,   // London longitude
            "GB",
            "London"
        );

        var profile = ImmutableUserRiskProfile.createNew(userId);
        var context = new FraudRule.FraudEvaluationContext(currentTx, profile, null);

        // Evaluate
        Optional<FraudRule.FraudViolation> violation = impossibleTravelRule.evaluate(context);

        // Assert
        assertThat(violation).isPresent();
        assertThat(violation.get().ruleId()).isEqualTo("IMPOSSIBLE_TRAVEL");
        assertThat(violation.get().riskScore()).isGreaterThan(0.5);
    }

    @Test
    void testImpossibleTravelRule_PassesRealisticTravel() {
        String userId = "travel-user-2";
        Instant now = Instant.now();

        // Previous transaction in San Francisco
        Transactions previousTx = createTransactionWithLocation(
            userId,
            BigDecimal.valueOf(100),
            now.minus(Duration.ofDays(2)),  // 2 days ago
            37.7749,
            -122.4194,
            "US",
            "San Francisco"
        );
        transactionRepository.save(previousTx).block();

        // Current transaction in Los Angeles (600 km, 2 days later = realistic)
        Transactions currentTx = createTransactionWithLocation(
            userId,
            BigDecimal.valueOf(100),
            now,
            34.0522,
            -118.2437,
            "US",
            "Los Angeles"
        );

        var profile = ImmutableUserRiskProfile.createNew(userId);
        var context = new FraudRule.FraudEvaluationContext(currentTx, profile, null);

        // Evaluate
        Optional<FraudRule.FraudViolation> violation = impossibleTravelRule.evaluate(context);

        // Assert
        assertThat(violation).isEmpty();
    }

    // ========== Amount Spike Rule Tests ==========

    @Test
    void testAmountSpikeRule_DetectsExtremeSpike() {
        String userId = "spike-user-1";
        Instant now = Instant.now();

        // Create 30 days of normal transactions ($50 average)
        for (int i = 1; i <= 30; i++) {
            Transactions tx = createTransaction(
                userId,
                BigDecimal.valueOf(50),
                now.minus(Duration.ofDays(i))
            );
            transactionRepository.save(tx).block();
        }

        // Current transaction - $500 (10x average)
        Transactions currentTx = createTransaction(userId, BigDecimal.valueOf(500), now);

        var profile = createEstablishedProfile(userId);
        var context = new FraudRule.FraudEvaluationContext(currentTx, profile, null);

        // Evaluate
        Optional<FraudRule.FraudViolation> violation = amountSpikeRule.evaluate(context);

        // Assert
        assertThat(violation).isPresent();
        assertThat(violation.get().ruleId()).isEqualTo("AMOUNT_EXTREME_SPIKE");
        assertThat(violation.get().riskScore()).isGreaterThanOrEqualTo(0.85);
    }

    @Test
    void testAmountSpikeRule_PassesNormalAmount() {
        String userId = "spike-user-2";
        Instant now = Instant.now();

        // Create 30 days of normal transactions ($100 average)
        for (int i = 1; i <= 30; i++) {
            Transactions tx = createTransaction(
                userId,
                BigDecimal.valueOf(100),
                now.minus(Duration.ofDays(i))
            );
            transactionRepository.save(tx).block();
        }

        // Current transaction - $150 (1.5x average - normal variation)
        Transactions currentTx = createTransaction(userId, BigDecimal.valueOf(150), now);

        var profile = createEstablishedProfile(userId);
        var context = new FraudRule.FraudEvaluationContext(currentTx, profile, null);

        // Evaluate
        Optional<FraudRule.FraudViolation> violation = amountSpikeRule.evaluate(context);

        // Assert
        assertThat(violation).isEmpty();
    }

    @Test
    void testAmountSpikeRule_SkipsNewUsers() {
        String userId = "spike-user-3";
        Instant now = Instant.now();

        // No historical transactions (new user)

        // Current transaction - $1000
        Transactions currentTx = createTransaction(userId, BigDecimal.valueOf(1000), now);

        var profile = ImmutableUserRiskProfile.createNew(userId);
        var context = new FraudRule.FraudEvaluationContext(currentTx, profile, null);

        // Evaluate
        Optional<FraudRule.FraudViolation> violation = amountSpikeRule.evaluate(context);

        // Assert - should be empty because new users are skipped
        assertThat(violation).isEmpty();
    }

    // ========== Helper Methods ==========

    private Transactions createTransaction(String userId, BigDecimal amount, Instant createdAt) {
        return Transactions.builder()
            .id(UUID.randomUUID())
            .userId(userId)
            .amount(amount)
            .currency("USD")
            .createdAt(createdAt)
            .transactionType(Transactions.TransactionType.PURCHASE)
            .merchantCategory("RETAIL")
            .merchantName("Test Merchant")
            .isInternational(false)
            .build();
    }

    private Transactions createTransactionWithLocation(
        String userId, 
        BigDecimal amount, 
        Instant createdAt,
        Double latitude,
        Double longitude,
        String country,
        String city
    ) {
        return Transactions.builder()
            .id(UUID.randomUUID())
            .userId(userId)
            .amount(amount)
            .currency("USD")
            .createdAt(createdAt)
            .transactionType(Transactions.TransactionType.PURCHASE)
            .merchantCategory("RETAIL")
            .merchantName("Test Merchant")
            .isInternational(!country.equals("US"))
            .latitude(latitude)
            .longitude(longitude)
            .country(country)
            .city(city)
            .ipAddress("192.168.1.1")
            .build();
    }

    private ImmutableUserRiskProfile createEstablishedProfile(String userId) {
        return ImmutableUserRiskProfile.builder()
            .userId(userId)
            .totalTransactions(100)  // Established user
            .averageTransactionAmount(100.0)
            .totalTransactionValue(10000.0)
            .highRiskTransactions(0)
            .internationalTransactions(5)
            .behavioralRiskScore(0.3)
            .transactionRiskScore(0.3)
            .overallRiskScore(0.3)
            .firstTransactionDate(Instant.now().minus(Duration.ofDays(365)))
            .lastTransactionDate(Instant.now())
            .build();
    }
}