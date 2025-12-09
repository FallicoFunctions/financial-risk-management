package com.nickfallico.financialriskmanagement.repository;

import com.nickfallico.financialriskmanagement.model.Transactions;
import com.nickfallico.financialriskmanagement.model.Transactions.TransactionType;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Repository
public interface TransactionRepository extends R2dbcRepository<Transactions, UUID> {
    
    Flux<Transactions> findByUserId(String userId);
    
    Flux<Transactions> findByUserIdAndCreatedAtBetween(
        String userId, 
        Instant start, 
        Instant end
    );
    
    @Query("SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE user_id = :userId AND created_at >= :start AND created_at < :end")
    Mono<BigDecimal> sumAmountByUserIdAndCreatedAtBetween(
        String userId, 
        Instant start, 
        Instant end
    );

    @Query("INSERT INTO transactions (id, user_id, amount, currency, created_at, transaction_type, merchant_category, is_international, merchant_name, latitude, longitude, country, city, ip_address) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13, $14) RETURNING *")
    Mono<Transactions> saveTransaction(
        UUID id,
        String userId,
        BigDecimal amount,
        String currency,
        Instant createdAt,
        TransactionType transactionType,
        String merchantCategory,
        Boolean isInternational,
        String merchantName,
        Double latitude,
        Double longitude,
        String country,
        String city,
        String ipAddress
    );

    // Find transactions by merchant category
    Flux<Transactions> findByMerchantCategory(String merchantCategory);

    // Count transactions by user and international status
    Mono<Long> countByUserIdAndIsInternational(String userId, Boolean isInternational);

    // Find top 5 highest value transactions for a user
    @Query("SELECT * FROM transactions WHERE user_id = :userId ORDER BY amount DESC LIMIT 5")
    Flux<Transactions> findTop5HighestTransactionsByUserId(String userId);

    // Find transactions exceeding a specific amount threshold
    Flux<Transactions> findByAmountGreaterThan(BigDecimal threshold);

    // Calculate average transaction amount by merchant category
    @Query("SELECT AVG(amount) FROM transactions WHERE merchant_category = :merchantCategory")
    Mono<Double> calculateAverageAmountByMerchantCategory(String merchantCategory);

    Mono<Long> countByUserIdAndCreatedAtBetween(String userId, Instant start, Instant end);

    // ========== Velocity Check Queries ==========
    /**
     * Count transactions for a user in the last N minutes
     * Used for velocity fraud detection (e.g., 10 transactions in 5 minutes)
     */
    @Query("SELECT COUNT(*) FROM transactions WHERE user_id = :userId AND created_at >= :since")
    Mono<Long> countRecentTransactions(String userId, Instant since);

    /**
     * Find recent transactions for detailed velocity analysis
     * Ordered by most recent first
     */
    @Query("SELECT * FROM transactions WHERE user_id = :userId AND created_at >= :since ORDER BY created_at DESC")
    Flux<Transactions> findRecentTransactions(String userId, Instant since);

    /**
     * Find transactions with same amount in a time window
     * Detects possible card testing or automated fraud
     */
    @Query("SELECT * FROM transactions WHERE user_id = :userId AND amount = :amount AND created_at >= :since ORDER BY created_at DESC")
    Flux<Transactions> findTransactionsWithSameAmount(String userId, BigDecimal amount, Instant since);

    // ========== Geographic Anomaly Queries ==========
    /**
     * Find user's most recent transaction with location data
     * Used for impossible travel detection
     */
    @Query("SELECT * FROM transactions WHERE user_id = :userId AND latitude IS NOT NULL AND longitude IS NOT NULL ORDER BY created_at DESC LIMIT 1")
    Mono<Transactions> findMostRecentTransactionWithLocation(String userId);

    /**
     * Find the latest transaction with location data that occurred before the supplied timestamp.
     * Helps ImpossibleTravelRule compare against the previous transaction instead of the current one.
     */
    @Query("""
        SELECT * FROM transactions
        WHERE user_id = :userId
          AND latitude IS NOT NULL
          AND longitude IS NOT NULL
          AND id <> :transactionId
          AND created_at <= :before
        ORDER BY created_at DESC
        LIMIT 1
    """)
    Mono<Transactions> findPreviousTransactionWithLocation(
        String userId,
        java.util.UUID transactionId,
        Instant before
    );

    /**
     * Find the most recent transaction with location data excluding the given transaction ID.
     * Used as a fallback when the current transaction does not yet have a createdAt timestamp.
     */
    @Query("""
        SELECT * FROM transactions
        WHERE user_id = :userId
          AND latitude IS NOT NULL
          AND longitude IS NOT NULL
          AND id <> :transactionId
        ORDER BY created_at DESC
        LIMIT 1
    """)
    Mono<Transactions> findLatestOtherTransactionWithLocation(
        String userId,
        java.util.UUID transactionId
    );

    /**
     * Find recent transactions with geographic data
     * Used for location pattern analysis
     */
    @Query("SELECT * FROM transactions WHERE user_id = :userId AND latitude IS NOT NULL AND longitude IS NOT NULL AND created_at >= :since ORDER BY created_at DESC")
    Flux<Transactions> findRecentTransactionsWithLocation(String userId, Instant since);

    /**
     * Count distinct countries used by user
     * High count may indicate account compromise
     */
    @Query("SELECT COUNT(DISTINCT country) FROM transactions WHERE user_id = :userId AND country IS NOT NULL")
    Mono<Long> countDistinctCountries(String userId);

    /**
     * Find all transactions from a specific country for a user
     * Used to establish country usage patterns
     */
    @Query("SELECT * FROM transactions WHERE user_id = :userId AND country = :country ORDER BY created_at DESC")
    Flux<Transactions> findTransactionsByUserAndCountry(String userId, String country);

    /**
     * Check if user has ever transacted from a specific country
     * Used to detect first-time country usage
     */
    @Query("SELECT EXISTS(SELECT 1 FROM transactions WHERE user_id = :userId AND country = :country)")
    Mono<Boolean> hasUserTransactedInCountry(String userId, String country);

    // ========== Amount Pattern Queries ==========
    /**
     * Find transactions with amounts significantly higher than user's average
     * Used for amount spike detection
     */
    @Query("SELECT * FROM transactions WHERE user_id = :userId AND amount > :threshold AND created_at >= :since ORDER BY created_at DESC")
    Flux<Transactions> findTransactionsAboveThreshold(String userId, BigDecimal threshold, Instant since);

    /**
     * Calculate user's average transaction amount over last N days
     * Used as baseline for anomaly detection
     */
    @Query("SELECT AVG(amount) FROM transactions WHERE user_id = :userId AND created_at >= :since")
    Mono<Double> calculateAverageAmountSince(String userId, Instant since);

    /**
     * Calculate user's standard deviation of transaction amounts
     * Used for statistical anomaly detection
     */
    @Query("SELECT STDDEV(amount) FROM transactions WHERE user_id = :userId AND created_at >= :since")
    Mono<Double> calculateStdDevAmountSince(String userId, Instant since);
}
