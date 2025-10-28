package com.nickfallico.financialriskmanagement.service;

import com.nickfallico.financialriskmanagement.model.Transaction;
import com.nickfallico.financialriskmanagement.dto.TransactionDTO;
import com.nickfallico.financialriskmanagement.repository.TransactionRepository;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;

    // Create a new transaction
    public Mono<Transaction> createTransaction(@Valid TransactionDTO transactionDTO) {
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

        return transactionRepository.save(transaction);
    }

    // Get daily total for a user
    public Mono<BigDecimal> getDailyTotal(String userId) {
        Instant startOfDay = Instant.now().truncatedTo(ChronoUnit.DAYS);
        Instant endOfDay = startOfDay.plus(1, ChronoUnit.DAYS);

        return transactionRepository.sumAmountByUserIdAndCreatedAtBetween(
            userId, startOfDay, endOfDay
        );
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
        return transactionRepository.countByUserIdAndIsInternational(userId, true);
    }

    // Find a user's top 5 highest value transactions
    public Flux<Transaction> getTop5HighestTransactions(String userId) {
        return transactionRepository.findTop5HighestTransactionsByUserId(userId);
    }

    // Get transactions above a certain amount
    public Flux<Transaction> getTransactionsAboveThreshold(BigDecimal threshold) {
        return transactionRepository.findByAmountGreaterThan(threshold);
    }

    // Calculate average transaction amount by merchant category
    public Mono<Double> getAverageTransactionAmountByMerchantCategory(String merchantCategory) {
        return transactionRepository.calculateAverageAmountByMerchantCategory(merchantCategory);
    }
}