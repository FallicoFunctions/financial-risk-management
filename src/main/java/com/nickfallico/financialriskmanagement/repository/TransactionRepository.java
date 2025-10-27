package com.nickfallico.financialriskmanagement.repository;

import com.nickfallico.financialriskmanagement.model.Transaction;
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

    // Find highest value transactions for a user
    Flux<Transaction> findTop5ByUserIdOrderByAmountDesc(String userId);

    // Complex query: Find transactions exceeding a specific amount threshold
    Flux<Transaction> findByAmountGreaterThan(BigDecimal threshold);

    // Aggregate method: Calculate average transaction amount by merchant category
    Mono<Double> calculateAverageAmountByMerchantCategory(String merchantCategory);
}