package com.nickfallico.financialriskmanagement.controller;

import com.nickfallico.financialriskmanagement.dto.TransactionDTO;
import com.nickfallico.financialriskmanagement.model.Transaction;
import com.nickfallico.financialriskmanagement.service.TransactionService;

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

    @PostMapping
    public Mono<ResponseEntity<Transaction>> createTransaction(@RequestBody TransactionDTO transactionDTO) {
        return transactionService.createTransaction(transactionDTO)
            .map(ResponseEntity::ok)
            .defaultIfEmpty(ResponseEntity.badRequest().build());
    }

    @GetMapping("/user/{userId}")
    public Flux<Transaction> getUserTransactions(@PathVariable String userId) {
        return transactionService.getUserTransactions(userId);
    }

    @GetMapping("/daily-total/{userId}")
    public Mono<BigDecimal> getDailyTotal(@PathVariable String userId) {
        return transactionService.getDailyTotal(userId);
    }

    @GetMapping("/merchant-category/{category}")
    public Flux<Transaction> getTransactionsByMerchantCategory(@PathVariable String category) {
        return transactionService.getTransactionsByMerchantCategory(category);
    }

    @GetMapping("/international-count/{userId}")
    public Mono<Long> countInternationalTransactions(@PathVariable String userId) {
        return transactionService.countInternationalTransactions(userId);
    }

    @GetMapping("/top-5-highest/{userId}")
    public Flux<Transaction> getTop5HighestTransactions(@PathVariable String userId) {
        return transactionService.getTop5HighestTransactions(userId);
    }

    @GetMapping("/above-threshold/{threshold}")
    public Flux<Transaction> getTransactionsAboveThreshold(
        @Positive(message = "Threshold must be positive") 
        @PathVariable BigDecimal threshold) {
        return transactionService.getTransactionsAboveThreshold(threshold);
    }

    @GetMapping("/avg-amount/merchant-category/{category}")
    public Mono<Double> getAverageTransactionAmountByMerchantCategory(@PathVariable String category) {
        return transactionService.getAverageTransactionAmountByMerchantCategory(category);
    }
}