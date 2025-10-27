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
}