package com.nickfallico.financialriskmanagement.controller;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nickfallico.financialriskmanagement.dto.TransactionStatisticsDTO;
import com.nickfallico.financialriskmanagement.dto.UserRiskProfileDTO;
import com.nickfallico.financialriskmanagement.exception.ResourceNotFoundException;
import com.nickfallico.financialriskmanagement.model.ImmutableUserRiskProfile;
import com.nickfallico.financialriskmanagement.model.Transactions;
import com.nickfallico.financialriskmanagement.repository.TransactionRepository;
import com.nickfallico.financialriskmanagement.service.UserProfileService;

import io.micrometer.core.instrument.MeterRegistry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * REST API for user risk profiles and transaction statistics.
 * Provides endpoints for viewing user risk assessments and behavior patterns.
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Risk Assessment", description = "User risk profiles and assessment endpoints")
public class UserRiskController {
    
    private final UserProfileService userProfileService;
    private final TransactionRepository transactionRepository;
    private final MeterRegistry meterRegistry;
    
    @Operation(
        summary = "Get user risk profile",
        description = "Retrieves comprehensive risk assessment for a user including behavioral and transaction risk scores, " +
            "risk level classification, and transaction statistics. The profile is continuously updated based on user activity."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Risk profile retrieved successfully",
            content = @Content(schema = @Schema(implementation = UserRiskProfileDTO.class))),
        @ApiResponse(responseCode = "404", description = "User risk profile not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/{userId}/risk-profile")
    public Mono<ResponseEntity<UserRiskProfileDTO>> getUserRiskProfile(
        @Parameter(description = "User ID", required = true) @PathVariable String userId
    ) {
        log.info("API request: Get risk profile for user: {}", userId);
        meterRegistry.counter("api.risk_profile.requests", "user_id", userId).increment();
        
        return userProfileService.getUserProfile(userId)
            .map(profile -> {
                UserRiskProfileDTO dto = convertToDTO(profile);
                
                log.info("Risk profile retrieved: userId={}, riskLevel={}, overallScore={}", 
                    userId, dto.getRiskLevel(), dto.getOverallRiskScore());
                
                meterRegistry.counter("api.risk_profile.success",
                    "risk_level", dto.getRiskLevel()
                ).increment();
                
                return ResponseEntity.ok(dto);
            })
            .switchIfEmpty(Mono.error(
                new ResourceNotFoundException("User risk profile not found for userId: " + userId)
            ))
            .doOnError(error -> {
                log.error("Failed to retrieve risk profile for user: {}", userId, error);
                meterRegistry.counter("api.risk_profile.errors").increment();
            });
    }
    
    @Operation(
        summary = "Get transaction statistics",
        description = "Retrieves detailed transaction statistics for a user including totals, averages, breakdowns by category, " +
            "country, type, and recent activity metrics."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully",
            content = @Content(schema = @Schema(implementation = TransactionStatisticsDTO.class))),
        @ApiResponse(responseCode = "404", description = "No transactions found for user"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/{userId}/transaction-statistics")
    public Mono<ResponseEntity<TransactionStatisticsDTO>> getTransactionStatistics(
        @Parameter(description = "User ID", required = true) @PathVariable String userId
    ) {
        log.info("API request: Get transaction statistics for user: {}", userId);
        meterRegistry.counter("api.transaction_statistics.requests", "user_id", userId).increment();
        
        return transactionRepository.findByUserId(userId)
            .collectList()
            .map(transactions -> {
                if (transactions.isEmpty()) {
                    throw new ResourceNotFoundException("No transactions found for userId: " + userId);
                }
                
                TransactionStatisticsDTO stats = calculateStatistics(transactions);
                
                log.info("Transaction statistics retrieved: userId={}, totalTransactions={}", 
                    userId, stats.getTotalTransactions());
                
                return ResponseEntity.ok(stats);
            })
            .doOnError(error -> {
                log.error("Failed to retrieve transaction statistics for user: {}", userId, error);
                meterRegistry.counter("api.transaction_statistics.errors").increment();
            });
    }
    
    @Operation(
        summary = "Get user risk level",
        description = "Retrieves a lightweight risk level summary for a user. Returns risk level classification " +
            "(LOW/MEDIUM/HIGH/CRITICAL) and key risk scores."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Risk level retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "User risk profile not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/{userId}/risk-level")
    public Mono<ResponseEntity<Map<String, Object>>> getUserRiskLevel(
        @Parameter(description = "User ID", required = true) @PathVariable String userId
    ) {
        log.info("API request: Get risk level for user: {}", userId);
        
        return userProfileService.getUserProfile(userId)
            .map(profile -> {
                String riskLevel = UserRiskProfileDTO.calculateRiskLevel(profile.getOverallRiskScore());
                
                Map<String, Object> response = Map.of(
                    "userId", userId,
                    "riskLevel", riskLevel,
                    "overallRiskScore", profile.getOverallRiskScore(),
                    "behavioralRiskScore", profile.getBehavioralRiskScore(),
                    "transactionRiskScore", profile.getTransactionRiskScore(),
                    "highRiskTransactions", profile.getHighRiskTransactions(),
                    "timestamp", Instant.now()
                );
                
                return ResponseEntity.ok(response);
            })
            .switchIfEmpty(Mono.error(
                new ResourceNotFoundException("User risk profile not found for userId: " + userId)
            ));
    }
    
    /**
     * Convert ImmutableUserRiskProfile to UserRiskProfileDTO.
     */
    private UserRiskProfileDTO convertToDTO(ImmutableUserRiskProfile profile) {
        return UserRiskProfileDTO.builder()
            .userId(profile.getUserId())
            .overallRiskScore(profile.getOverallRiskScore())
            .behavioralRiskScore(profile.getBehavioralRiskScore())
            .transactionRiskScore(profile.getTransactionRiskScore())
            .riskLevel(UserRiskProfileDTO.calculateRiskLevel(profile.getOverallRiskScore()))
            .totalTransactions(profile.getTotalTransactions())
            .highRiskTransactions(profile.getHighRiskTransactions())
            .internationalTransactions(profile.getInternationalTransactions())
            .averageTransactionAmount(profile.getAverageTransactionAmount())
            .totalTransactionValue(profile.getTotalTransactionValue())
            .firstTransactionDate(profile.getFirstTransactionDate())
            .lastTransactionDate(profile.getLastTransactionDate())
            .accountAgeDays(UserRiskProfileDTO.calculateAccountAgeDays(
                profile.getFirstTransactionDate(),
                profile.getLastTransactionDate()
            ))
            .userType(UserRiskProfileDTO.calculateUserType(profile.getTotalTransactions()))
            .profileLastUpdated(Instant.now())
            .build();
    }
    
    /**
     * Calculate detailed transaction statistics from transaction list.
     */
    private TransactionStatisticsDTO calculateStatistics(List<Transactions> transactions) {
        Instant now = Instant.now();
        Instant sevenDaysAgo = now.minus(7, ChronoUnit.DAYS);
        Instant thirtyDaysAgo = now.minus(30, ChronoUnit.DAYS);
        
        // Transaction counts
        int totalTransactions = transactions.size();
        int domesticCount = (int) transactions.stream()
            .filter(tx -> !Boolean.TRUE.equals(tx.getIsInternational()))
            .count();
        int internationalCount = totalTransactions - domesticCount;
        
        // Value calculations
        BigDecimal totalValue = transactions.stream()
            .map(Transactions::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal averageAmount = totalTransactions > 0 
            ? totalValue.divide(BigDecimal.valueOf(totalTransactions), 2, java.math.RoundingMode.HALF_UP)
            : BigDecimal.ZERO;
        
        // Median calculation
        List<BigDecimal> sortedAmounts = transactions.stream()
            .map(Transactions::getAmount)
            .sorted()
            .collect(Collectors.toList());
        BigDecimal medianAmount = sortedAmounts.isEmpty() 
            ? BigDecimal.ZERO
            : sortedAmounts.get(sortedAmounts.size() / 2);
        
        // Breakdown by merchant category
        Map<String, Integer> merchantBreakdown = transactions.stream()
            .collect(Collectors.groupingBy(
                tx -> tx.getMerchantCategory() != null ? tx.getMerchantCategory() : "UNKNOWN",
                Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
            ));
        
        // Breakdown by country
        Map<String, Integer> countryBreakdown = transactions.stream()
            .filter(tx -> tx.getCountry() != null)
            .collect(Collectors.groupingBy(
                Transactions::getCountry,
                Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
            ));
        
        // Breakdown by transaction type
        Map<String, Integer> typeBreakdown = transactions.stream()
            .collect(Collectors.groupingBy(
                tx -> tx.getTransactionType() != null ? tx.getTransactionType().toString() : "UNKNOWN",
                Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
            ));
        
        // Recent activity (last 7 days)
        long transactionsLast7Days = transactions.stream()
            .filter(tx -> tx.getCreatedAt().isAfter(sevenDaysAgo))
            .count();
        
        BigDecimal valueLast7Days = transactions.stream()
            .filter(tx -> tx.getCreatedAt().isAfter(sevenDaysAgo))
            .map(Transactions::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Recent activity (last 30 days)
        long transactionsLast30Days = transactions.stream()
            .filter(tx -> tx.getCreatedAt().isAfter(thirtyDaysAgo))
            .count();
        
        BigDecimal valueLast30Days = transactions.stream()
            .filter(tx -> tx.getCreatedAt().isAfter(thirtyDaysAgo))
            .map(Transactions::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // High risk transactions (amount > 10000 or international with high amount)
        int highRiskCount = (int) transactions.stream()
            .filter(tx -> {
                boolean isLargeAmount = tx.getAmount().compareTo(new BigDecimal("10000")) > 0;
                boolean isHighRiskInternational = Boolean.TRUE.equals(tx.getIsInternational()) 
                    && tx.getAmount().compareTo(new BigDecimal("5000")) > 0;
                return isLargeAmount || isHighRiskInternational;
            })
            .count();
        
        double highRiskPercentage = totalTransactions > 0
            ? (highRiskCount * 100.0) / totalTransactions
            : 0.0;
        
        return TransactionStatisticsDTO.builder()
            .totalTransactions(totalTransactions)
            .totalValue(totalValue)
            .averageAmount(averageAmount)
            .medianAmount(medianAmount)
            .transactionsByType(typeBreakdown)
            .transactionsByMerchantCategory(merchantBreakdown)
            .domesticTransactions(domesticCount)
            .internationalTransactions(internationalCount)
            .transactionsByCountry(countryBreakdown)
            .highRiskTransactions(highRiskCount)
            .highRiskPercentage(highRiskPercentage)
            .transactionsLast7Days((int) transactionsLast7Days)
            .transactionsLast30Days((int) transactionsLast30Days)
            .valueLast7Days(valueLast7Days)
            .valueLast30Days(valueLast30Days)
            .build();
    }
}