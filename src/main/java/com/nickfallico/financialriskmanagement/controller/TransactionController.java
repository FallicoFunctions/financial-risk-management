package com.nickfallico.financialriskmanagement.controller;

import com.nickfallico.financialriskmanagement.dto.TransactionDTO;
import com.nickfallico.financialriskmanagement.model.Transactions;
import com.nickfallico.financialriskmanagement.service.TransactionRiskWorkflow;
import com.nickfallico.financialriskmanagement.service.TransactionService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
@Validated
public class TransactionController {

    private final TransactionService transactionService;
    private final TransactionRiskWorkflow transactionRiskWorkflow;

    @PostMapping
    public Mono<ResponseEntity<Transactions>> createTransaction(
        @Valid @RequestBody TransactionDTO transactionDTO
    ) {
        // Build transaction from DTO
        Transactions transaction = Transactions.builder()
            .userId(transactionDTO.getUserId())
            .amount(transactionDTO.getAmount())
            .currency(transactionDTO.getCurrency())
            .transactionType(transactionDTO.getTransactionType())
            .merchantCategory(transactionDTO.getMerchantCategory())
            .merchantName(transactionDTO.getMerchantName())
            .isInternational(transactionDTO.getIsInternational())
            .createdAt(java.time.Instant.now())
            .build();
        
        return transactionRiskWorkflow.processTransaction(transaction)
            .map(ResponseEntity::ok)
            .defaultIfEmpty(ResponseEntity.badRequest().build());
    }

    @GetMapping("/user/{userId}")
    public Flux<Transactions> getUserTransactions(@PathVariable String userId) {
        return transactionService.getUserTransactions(userId);
    }

    @GetMapping("/daily-total/{userId}")
    public Mono<BigDecimal> getDailyTotal(@PathVariable String userId) {
        return transactionService.getDailyTotal(userId);
    }

    @GetMapping("/merchant-category/{category}")
    public Flux<Transactions> getTransactionsByMerchantCategory(@PathVariable String category) {
        return transactionService.getTransactionsByMerchantCategory(category);
    }

    @GetMapping("/international-count/{userId}")
    public Mono<Long> countInternationalTransactions(@PathVariable String userId) {
        return transactionService.countInternationalTransactions(userId);
    }

    @GetMapping("/top-5-highest/{userId}")
    public Flux<Transactions> getTop5HighestTransactions(@PathVariable String userId) {
        return transactionService.getTop5HighestTransactions(userId);
    }

    @GetMapping("/above-threshold/{threshold}")
    public Flux<Transactions> getTransactionsAboveThreshold(
        @Positive(message = "Threshold must be positive") 
        @PathVariable BigDecimal threshold) {
        return transactionService.getTransactionsAboveThreshold(threshold);
    }

    @GetMapping("/avg-amount/merchant-category/{category}")
    public Mono<Double> getAverageTransactionAmountByMerchantCategory(@PathVariable String category) {
        return transactionService.getAverageTransactionAmountByMerchantCategory(category);
    }
}