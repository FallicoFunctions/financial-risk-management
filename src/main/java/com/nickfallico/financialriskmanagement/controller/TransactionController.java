package com.nickfallico.financialriskmanagement.controller;

import com.nickfallico.financialriskmanagement.dto.TransactionDTO;
import com.nickfallico.financialriskmanagement.model.Transactions;
import com.nickfallico.financialriskmanagement.service.TransactionRiskWorkflow;
import com.nickfallico.financialriskmanagement.service.TransactionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Transactions", description = "Transaction creation and querying endpoints")
public class TransactionController {

    private final TransactionService transactionService;
    private final TransactionRiskWorkflow transactionRiskWorkflow;

    @Operation(
        summary = "Create a new transaction",
        description = "Submits a new transaction for risk assessment and fraud detection. " +
            "The transaction will be evaluated in real-time using ML models and rule-based engines. " +
            "Events will be published to Kafka for async processing."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Transaction created and assessed successfully",
            content = @Content(schema = @Schema(implementation = Transactions.class))),
        @ApiResponse(responseCode = "400", description = "Invalid transaction data"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PostMapping
    public Mono<ResponseEntity<Transactions>> createTransaction(
        @Parameter(description = "Transaction details", required = true)
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
            // Map geographic location fields
            .latitude(transactionDTO.getLatitude())
            .longitude(transactionDTO.getLongitude())
            .country(transactionDTO.getCountry())
            .city(transactionDTO.getCity())
            .ipAddress(transactionDTO.getIpAddress())
            .createdAt(java.time.Instant.now())
            .build();
        
        return transactionRiskWorkflow.processTransaction(transaction)
            .map(ResponseEntity::ok)
            .defaultIfEmpty(ResponseEntity.badRequest().build());
    }

    @Operation(summary = "Get user transactions", description = "Retrieves all transactions for a specific user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Transactions retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/user/{userId}")
    public Flux<Transactions> getUserTransactions(
        @Parameter(description = "User ID", required = true) @PathVariable String userId
    ) {
        return transactionService.getUserTransactions(userId);
    }

    @Operation(summary = "Get daily transaction total", description = "Calculates the total transaction amount for a user today")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Daily total calculated successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/daily-total/{userId}")
    public Mono<BigDecimal> getDailyTotal(
        @Parameter(description = "User ID", required = true) @PathVariable String userId
    ) {
        return transactionService.getDailyTotal(userId);
    }

    @Operation(summary = "Get transactions by merchant category", description = "Retrieves all transactions for a specific merchant category")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Transactions retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/merchant-category/{category}")
    public Flux<Transactions> getTransactionsByMerchantCategory(
        @Parameter(description = "Merchant category (e.g., RETAIL, GAMBLING, CRYPTOCURRENCY)", required = true)
        @PathVariable String category
    ) {
        return transactionService.getTransactionsByMerchantCategory(category);
    }

    @Operation(summary = "Count international transactions", description = "Counts international transactions for a specific user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Count retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/international-count/{userId}")
    public Mono<Long> countInternationalTransactions(
        @Parameter(description = "User ID", required = true) @PathVariable String userId
    ) {
        return transactionService.countInternationalTransactions(userId);
    }

    @Operation(summary = "Get top 5 highest transactions", description = "Retrieves the 5 highest-value transactions for a user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Transactions retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/top-5-highest/{userId}")
    public Flux<Transactions> getTop5HighestTransactions(
        @Parameter(description = "User ID", required = true) @PathVariable String userId
    ) {
        return transactionService.getTop5HighestTransactions(userId);
    }

    @Operation(summary = "Get transactions above threshold", description = "Retrieves all transactions above a specified amount threshold")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Transactions retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid threshold value"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/above-threshold/{threshold}")
    public Flux<Transactions> getTransactionsAboveThreshold(
        @Parameter(description = "Minimum transaction amount", required = true)
        @Positive(message = "Threshold must be positive")
        @PathVariable BigDecimal threshold
    ) {
        return transactionService.getTransactionsAboveThreshold(threshold);
    }

    @Operation(summary = "Get average transaction amount by category", description = "Calculates the average transaction amount for a merchant category")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Average calculated successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/avg-amount/merchant-category/{category}")
    public Mono<Double> getAverageTransactionAmountByMerchantCategory(
        @Parameter(description = "Merchant category", required = true) @PathVariable String category
    ) {
        return transactionService.getAverageTransactionAmountByMerchantCategory(category);
    }
}