package com.nickfallico.financialriskmanagement.controller;

import com.nickfallico.financialriskmanagement.dto.TransactionDTO;
import com.nickfallico.financialriskmanagement.model.Transaction;
import com.nickfallico.financialriskmanagement.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
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
}