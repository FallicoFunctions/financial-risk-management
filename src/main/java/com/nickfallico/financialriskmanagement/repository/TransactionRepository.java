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

    @Query("INSERT INTO transactions (id, user_id, amount, currency, created_at, transaction_type, merchant_category, is_international, merchant_name) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9) RETURNING *")
    Mono<Transactions> saveTransaction(
        UUID id,
        String userId, 
        BigDecimal amount, 
        String currency, 
        Instant createdAt, 
        TransactionType transactionType, 
        String merchantCategory, 
        Boolean isInternational,
        String merchantName
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
}