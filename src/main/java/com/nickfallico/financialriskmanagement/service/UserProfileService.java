package com.nickfallico.financialriskmanagement.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.nickfallico.financialriskmanagement.model.ImmutableUserRiskProfile;
import com.nickfallico.financialriskmanagement.model.MerchantCategoryFrequency;
import com.nickfallico.financialriskmanagement.model.Transactions;
import com.nickfallico.financialriskmanagement.repository.ImmutableUserRiskProfileRepository;
import com.nickfallico.financialriskmanagement.repository.MerchantCategoryFrequencyRepository;
import com.nickfallico.financialriskmanagement.repository.TransactionRepository;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * User profile service using event sourcing.
 * Profile state is computed from transaction history (immutable source of truth).
 * No direct mutations; all updates create new immutable profiles.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class UserProfileService {
    
    private final TransactionRepository transactionRepository;
    private final ImmutableUserRiskProfileRepository immutableUserRiskProfileRepository;
    private final MerchantCategoryFrequencyRepository merchantCategoryFrequencyRepository;
    private final MeterRegistry meterRegistry;
    
    /**
     * Get or create user profile.
     * Cached at 30 minutes to avoid recomputation.
     */
    @Cacheable(
        value = "userProfiles",
        key = "#userId",
        cacheManager = "redisCacheManager"
    )
    public Mono<ImmutableUserRiskProfile> getUserProfile(String userId) {
        Timer.Sample sample = Timer.start(meterRegistry);
        
        return immutableUserRiskProfileRepository.findById(userId)
            .switchIfEmpty(Mono.fromSupplier(() -> 
                ImmutableUserRiskProfile.createNew(userId)
            ))
            .doOnSuccess(profile -> {
                sample.stop(meterRegistry.timer("get_user_profile_time"));
                meterRegistry.counter("get_user_profile_attempts").increment();
            });
    }
    
    /**
     * Get or create merchant frequency aggregate.
     */
    @Cacheable(
        value = "merchantFrequencies",
        key = "#userId",
        cacheManager = "redisCacheManager"
    )
    public Mono<MerchantCategoryFrequency> getMerchantFrequency(String userId) {
        return immutableUserRiskProfileRepository.findById(userId)
            .flatMapMany(profile -> transactionRepository.findByUserId(userId))
            .collectList()
            .map(transactions -> computeMerchantFrequencies(userId, transactions))
            .switchIfEmpty(Mono.fromSupplier(() -> 
                MerchantCategoryFrequency.createNew(userId)
            ));
    }
    
    /**
     * Update user profile after transaction.
     * Invalidates cache; triggers recomputation from transaction history.
     */
    @CacheEvict(
        value = {"userProfiles", "merchantFrequencies"},
        key = "#transaction.userId"
    )
    public Mono<Void> updateProfileAfterTransaction(Transactions transaction) {
        Timer.Sample sample = Timer.start(meterRegistry);
        
        return transactionRepository.findByUserId(transaction.getUserId())
            .collectList()
            .flatMap(allTransactions -> {
                ImmutableUserRiskProfile newProfile = 
                    computeProfileFromTransactionHistory(
                        transaction.getUserId(),
                        allTransactions
                    );
                
                MerchantCategoryFrequency frequencies = 
                    computeMerchantFrequencies(transaction.getUserId(), allTransactions);
                
                return immutableUserRiskProfileRepository.upsert(newProfile)
                    .then(merchantCategoryFrequencyRepository.upsert(frequencies))
                    .then(Mono.defer(() -> {
                        sample.stop(meterRegistry.timer("update_profile_time"));
                        meterRegistry.counter("profile_updates").increment();
                        log.debug("Profile updated for user: {}", transaction.getUserId());
                        return Mono.empty();
                    }));
            });
    }
    
    /**
     * Pure function: Compute profile from transaction history.
     * No side effects; deterministic from input list.
     */
    private ImmutableUserRiskProfile computeProfileFromTransactionHistory(
        String userId,
        List<Transactions> allTransactions) {
        
        if (allTransactions.isEmpty()) {
            return ImmutableUserRiskProfile.createNew(userId);
        }
        
        // Compute metrics using streams (functional approach, no loops)
        double avgAmount = allTransactions.stream()
            .map(Transactions::getAmount)
            .map(BigDecimal::doubleValue)
            .mapToDouble(Double::doubleValue)
            .average()
            .orElse(0.0);
        
        double totalValue = allTransactions.stream()
            .map(Transactions::getAmount)
            .map(BigDecimal::doubleValue)
            .mapToDouble(Double::doubleValue)
            .sum();
        
        int highRiskCount = (int) allTransactions.stream()
            .filter(tx -> isHighRiskTransaction(tx))
            .count();
        
        int internationalCount = (int) allTransactions.stream()
            .filter(tx -> Boolean.TRUE.equals(tx.getIsInternational()))
            .count();
        
        Instant firstTx = allTransactions.stream()
            .map(Transactions::getCreatedAt)
            .min(Comparator.naturalOrder())
            .orElse(Instant.now());
        
        Instant lastTx = allTransactions.stream()
            .map(Transactions::getCreatedAt)
            .max(Comparator.naturalOrder())
            .orElse(Instant.now());
        
        // Compute risk scores
        double behavioralRiskScore = computeBehavioralRisk(allTransactions);
        double transactionRiskScore = computeTransactionRisk(allTransactions);
        double overallRiskScore = (behavioralRiskScore + transactionRiskScore) / 2;
        
        return ImmutableUserRiskProfile.builder()
            .userId(userId)
            .averageTransactionAmount(avgAmount)
            .totalTransactions(allTransactions.size())
            .totalTransactionValue(totalValue)
            .highRiskTransactions(highRiskCount)
            .internationalTransactions(internationalCount)
            .behavioralRiskScore(behavioralRiskScore)
            .transactionRiskScore(transactionRiskScore)
            .overallRiskScore(overallRiskScore)
            .firstTransactionDate(firstTx)
            .lastTransactionDate(lastTx)
            .build();
    }
    
    /**
     * Pure function: Compute merchant frequencies from transaction history.
     */
    private MerchantCategoryFrequency computeMerchantFrequencies(
        String userId,
        List<Transactions> allTransactions) {
        
        Map<String, Integer> frequencies = allTransactions.stream()
            .map(Transactions::getMerchantCategory)
            .filter(cat -> cat != null && !cat.isEmpty())
            .collect(Collectors.groupingBy(
                String::toString,
                Collectors.summingInt(cat -> 1)
            ));
        
        return MerchantCategoryFrequency.builder()
            .frequencyId(UUID.randomUUID().toString())
            .userId(userId)
            .categoryFrequencies(frequencies)
            .lastUpdated(Instant.now())
            .build();
    }
    
    /**
     * Pure function: Compute behavioral risk from transaction patterns.
     */
    private double computeBehavioralRisk(List<Transactions> transactions) {
        if (transactions.isEmpty()) return 0.5;
        
        double baseRisk = 0.5;
        
        // Factor: Transaction frequency
        int count = transactions.size();
        if (count < 10) baseRisk += 0.2;
        else if (count > 100) baseRisk -= 0.1;
        
        // Factor: International transactions
        long intlCount = transactions.stream()
            .filter(tx -> Boolean.TRUE.equals(tx.getIsInternational()))
            .count();
        if (intlCount > count * 0.3) baseRisk += 0.15;
        
        // Factor: Merchant diversity
        long uniqueMerchants = transactions.stream()
            .map(Transactions::getMerchantCategory)
            .filter(cat -> cat != null && !cat.isEmpty())
            .distinct()
            .count();
        if (uniqueMerchants < 3) baseRisk += 0.1;
        
        return Math.min(Math.max(baseRisk, 0.0), 1.0);
    }
    
    /**
     * Pure function: Compute transaction risk from amount patterns.
     */
    private double computeTransactionRisk(List<Transactions> transactions) {
        if (transactions.isEmpty()) return 0.5;
        
        double avgAmount = transactions.stream()
            .map(Transactions::getAmount)
            .map(BigDecimal::doubleValue)
            .mapToDouble(Double::doubleValue)
            .average()
            .orElse(0.0);
        
        double maxAmount = transactions.stream()
            .map(Transactions::getAmount)
            .map(BigDecimal::doubleValue)
            .mapToDouble(Double::doubleValue)
            .max()
            .orElse(0.0);
        
        // If max is significantly higher than average, higher risk
        if (avgAmount == 0) return 0.3;
        
        double amountDeviation = (maxAmount - avgAmount) / avgAmount;
        double riskFromDeviation = Math.min(amountDeviation * 0.3, 0.5);
        
        return Math.min(riskFromDeviation + 0.3, 1.0);
    }
    
    private boolean isHighRiskTransaction(Transactions tx) {
        String category = tx.getMerchantCategory();
        BigDecimal amount = tx.getAmount();
        
        boolean highRiskCategory = category != null &&
            ("GAMBLING".equalsIgnoreCase(category) ||
             "CRYPTO".equalsIgnoreCase(category) ||
             "ADULT_ENTERTAINMENT".equalsIgnoreCase(category));
        
        boolean highAmount = amount.compareTo(BigDecimal.valueOf(10000)) >= 0;
        boolean international = Boolean.TRUE.equals(tx.getIsInternational());
        
        return highRiskCategory || highAmount || international;
    }

    /**
     * Update user risk score after fraud detection.
     * Increases risk score and invalidates cache.
     */
    @CacheEvict(
        value = {"userProfiles", "merchantFrequencies"},
        key = "#userId"
    )
    public Mono<ImmutableUserRiskProfile> increaseRiskScoreForFraud(
        String userId,
        Double fraudProbability,
        String riskLevel
    ) {
        Timer.Sample sample = Timer.start(meterRegistry);
        
        return immutableUserRiskProfileRepository.findById(userId)
            .switchIfEmpty(Mono.fromSupplier(() -> ImmutableUserRiskProfile.createNew(userId)))
            .flatMap(currentProfile -> {
                // Calculate new risk score based on fraud detection
                Double currentScore = currentProfile.getOverallRiskScore();
                Double increment = calculateRiskScoreIncrement(fraudProbability, riskLevel);
                Double newScore = Math.min(1.0, currentScore + increment);
                
                // Increment high risk transaction count
                int newHighRiskCount = currentProfile.getHighRiskTransactions() + 1;
                
                // Create updated profile with increased risk score
                ImmutableUserRiskProfile updatedProfile = currentProfile.toBuilder()
                    .overallRiskScore(newScore)
                    .highRiskTransactions(newHighRiskCount)
                    .behavioralRiskScore(Math.min(1.0, currentProfile.getBehavioralRiskScore() + (increment * 0.5)))
                    .lastTransactionDate(Instant.now())
                    .build();
                
                return immutableUserRiskProfileRepository.save(updatedProfile)
                    .doOnSuccess(profile -> {
                        sample.stop(meterRegistry.timer("update_risk_score_time"));
                        meterRegistry.counter("risk_score_updates",
                            "risk_level", riskLevel
                        ).increment();
                        
                        log.warn("Risk score updated for user {}: {} -> {} (fraud detected, level: {})",
                            userId, currentScore, newScore, riskLevel);
                    });
            });
    }
    
    /**
     * Calculate risk score increment based on fraud severity.
     * Returns a value between 0.0 and 1.0 to add to the risk score.
     */
    private Double calculateRiskScoreIncrement(Double fraudProbability, String riskLevel) {
        return switch (riskLevel) {
            case "CRITICAL" -> 0.3 + (fraudProbability * 0.2);
            case "HIGH" -> 0.2 + (fraudProbability * 0.15);
            case "MEDIUM" -> 0.1 + (fraudProbability * 0.1);
            case "LOW" -> 0.05 + (fraudProbability * 0.05);
            default -> fraudProbability * 0.1;
        };
    }
}