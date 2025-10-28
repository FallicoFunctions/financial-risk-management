package com.nickfallico.financialriskmanagement.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.nickfallico.financialriskmanagement.dto.TransactionDTO;
import com.nickfallico.financialriskmanagement.model.Transaction;
import com.nickfallico.financialriskmanagement.repository.TransactionRepository;

import io.micrometer.core.instrument.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class TransactionService {
    private final TransactionRepository transactionRepository;
    private final MeterRegistry meterRegistry;

    // Create a new transaction
    public Mono<Transaction> createTransaction(@Valid TransactionDTO transactionDTO) {
        Timer.Sample sample = Timer.start(meterRegistry);
        
        Transaction transaction = Transaction.builder()
            .id(UUID.randomUUID())
            .userId(transactionDTO.getUserId())
            .amount(transactionDTO.getAmount())
            .currency(transactionDTO.getCurrency())
            .transactionType(transactionDTO.getTransactionType())
            .merchantCategory(transactionDTO.getMerchantCategory())
            .merchantName(transactionDTO.getMerchantName())
            .isInternational(transactionDTO.getIsInternational())
            .createdAt(Instant.now())
            .build();

        return transactionRepository.save(transaction)
            .doOnSuccess(savedTransaction -> {
                sample.stop(meterRegistry.timer("create_transaction_time", 
                    "merchant_category", savedTransaction.getMerchantCategory()));
                
                meterRegistry.counter("create_transaction_count", 
                    "merchant_category", savedTransaction.getMerchantCategory(),
                    "is_international", String.valueOf(savedTransaction.getIsInternational()))
                    .increment();
            });
    }

    // Get daily total for a user
    public Mono<BigDecimal> getDailyTotal(String userId) {
        Timer.Sample sample = Timer.start(meterRegistry);
        
        Instant startOfDay = Instant.now().truncatedTo(ChronoUnit.DAYS);
        Instant endOfDay = startOfDay.plus(1, ChronoUnit.DAYS);

        return transactionRepository.sumAmountByUserIdAndCreatedAtBetween(
            userId, startOfDay, endOfDay
        ).doOnSuccess(total -> {
            sample.stop(meterRegistry.timer("get_daily_total_time"));
            meterRegistry.gauge("daily_total_amount", total.doubleValue());
        });
    }

    // Get transactions for a user
    public Flux<Transaction> getUserTransactions(String userId) {
        return transactionRepository.findByUserId(userId);
    }

    // Get transactions within a date range
    public Flux<Transaction> getTransactionsBetween(String userId, Instant start, Instant end) {
        return transactionRepository.findByUserIdAndCreatedAtBetween(userId, start, end);
    }

    // Retrieve transactions by merchant category
    public Flux<Transaction> getTransactionsByMerchantCategory(String merchantCategory) {
        return transactionRepository.findByMerchantCategory(merchantCategory);
    }

    // Count international transactions for a user
    public Mono<Long> countInternationalTransactions(String userId) {
        Timer.Sample sample = Timer.start(meterRegistry);
        
        return transactionRepository.countByUserIdAndIsInternational(userId, true)
            .doOnSuccess(count -> {
                sample.stop(meterRegistry.timer("count_international_transactions_time"));
                meterRegistry.gauge("international_transactions_count", count);
            });
    }

    // Find a user's top 5 highest value transactions
    public Flux<Transaction> getTop5HighestTransactions(String userId) {
        Timer.Sample sample = Timer.start(meterRegistry);
        
        return transactionRepository.findTop5HighestTransactionsByUserId(userId)
            .doFinally(signalType -> {
                sample.stop(meterRegistry.timer("get_top_5_highest_transactions_time"));
                meterRegistry.counter("get_top_5_highest_transactions_count", 
                    "user_id", userId).increment();
            });
    }

    // Get transactions above a certain amount
    public Flux<Transaction> getTransactionsAboveThreshold(BigDecimal threshold) {
        Timer.Sample sample = Timer.start(meterRegistry);
        
        return transactionRepository.findByAmountGreaterThan(threshold)
            .doFinally(signalType -> {
                sample.stop(meterRegistry.timer("get_transactions_above_threshold_time"));
                meterRegistry.counter("get_transactions_above_threshold_count", 
                    "threshold", threshold.toString()).increment();
            });
    }

    // Calculate average transaction amount by merchant category
    public Mono<Double> getAverageTransactionAmountByMerchantCategory(String merchantCategory) {
        Timer.Sample sample = Timer.start(meterRegistry);
        
        return transactionRepository.calculateAverageAmountByMerchantCategory(merchantCategory)
            .doOnSuccess(averageAmount -> {
                sample.stop(meterRegistry.timer("average_transaction_amount_time", 
                    "merchant_category", merchantCategory));
                meterRegistry.gauge("average_transaction_amount", averageAmount);
            });
    }
}