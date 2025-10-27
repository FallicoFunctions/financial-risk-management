package com.nickfallico.financialriskmanagement.repository;

import com.nickfallico.financialriskmanagement.model.Transaction;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Repository
public interface TransactionRepository extends R2dbcRepository<Transaction, UUID> {
    
    Flux<Transaction> findByUserId(String userId);
    
    Flux<Transaction> findByUserIdAndCreatedAtBetween(
        String userId, 
        Instant start, 
        Instant end
    );
    
    Mono<BigDecimal> sumAmountByUserIdAndCreatedAtBetween(
        String userId, 
        Instant start, 
        Instant end
    );

    // Find transactions by merchant category
    Flux<Transaction> findByMerchantCategory(String merchantCategory);

    // Count transactions by user and international status
    Mono<Long> countByUserIdAndIsInternational(String userId, Boolean isInternational);

    // Find top 5 highest value transactions for a user
    @Query("SELECT * FROM transactions WHERE user_id = :userId ORDER BY amount DESC LIMIT 5")
    Flux<Transaction> findTop5HighestTransactionsByUserId(String userId);

    // Find transactions exceeding a specific amount threshold
    Flux<Transaction> findByAmountGreaterThan(BigDecimal threshold);

    // Calculate average transaction amount by merchant category
    @Query("SELECT AVG(amount) FROM transactions WHERE merchant_category = :merchantCategory")
    Mono<Double> calculateAverageAmountByMerchantCategory(String merchantCategory);
}