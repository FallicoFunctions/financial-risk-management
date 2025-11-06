package com.nickfallico.financialriskmanagement.service;

import com.nickfallico.financialriskmanagement.model.ImmutableUserRiskProfile;
import com.nickfallico.financialriskmanagement.model.MerchantCategoryFrequency;
import com.nickfallico.financialriskmanagement.model.Transaction;
import com.nickfallico.financialriskmanagement.repository.MerchantCategoryFrequencyRepository;
import com.nickfallico.financialriskmanagement.repository.TransactionRepository;
import com.nickfallico.financialriskmanagement.repository.UserRiskProfileRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

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
    private final UserRiskProfileRepository userRiskProfileRepository;
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
        
        return userRiskProfileRepository.findById(userId)
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
        return userRiskProfileRepository.findById(userId)
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
    public Mono<Void> updateProfileAfterTransaction(Transaction transaction) {
        Timer.Sample sample = Timer.start(meterRegistry);
        
        // Load all transactions for this user
        return transactionRepository.findByUserId(transaction.getUserId())
            .collectList()
            .flatMap(allTransactions -> {
                // Compute profile from transaction history
                ImmutableUserRiskProfile newProfile = 
                    computeProfileFromTransactionHistory(
                        transaction.getUserId(),
                        allTransactions
                    );
                
                // Compute merchant frequencies
                MerchantCategoryFrequency frequencies = 
                    computeMerchantFrequencies(transaction.getUserId(), allTransactions);
                
                // Persist both profile and frequencies
                return userRiskProfileRepository.save(newProfile)
                    .then(merchantCategoryFrequencyRepository.save(frequencies))
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
        List<Transaction> allTransactions) {
        
        if (allTransactions.isEmpty()) {
            return ImmutableUserRiskProfile.createNew(userId);
        }
        
        // Compute metrics using streams (functional approach, no loops)
        double avgAmount = allTransactions.stream()
            .map(Transaction::getAmount)
            .map(BigDecimal::doubleValue)
            .mapToDouble(Double::doubleValue)
            .average()
            .orElse(0.0);
        
        double totalValue = allTransactions.stream()
            .map(Transaction::getAmount)
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
            .map(Transaction::getCreatedAt)
            .min(Comparator.naturalOrder())
            .orElse(Instant.now());
        
        Instant lastTx = allTransactions.stream()
            .map(Transaction::getCreatedAt)
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
        List<Transaction> allTransactions) {
        
        Map<String, Integer> frequencies = allTransactions.stream()
            .map(Transaction::getMerchantCategory)
            .filter(cat -> cat != null && !cat.isEmpty())
            .collect(Collectors.groupingBy(
                String::toString,
                Collectors.summingInt(cat -> 1)
            ));
        
        return MerchantCategoryFrequency.builder()
            .frequencyId(UUID.randomUUID().toString())
            .userId(userId)
            .categoryFrequencies(Collections.unmodifiableMap(frequencies))
            .lastUpdated(Instant.now())
            .build();
    }
    
    /**
     * Pure function: Compute behavioral risk from transaction patterns.
     */
    private double computeBehavioralRisk(List<Transaction> transactions) {
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
            .map(Transaction::getMerchantCategory)
            .filter(cat -> cat != null && !cat.isEmpty())
            .distinct()
            .count();
        if (uniqueMerchants < 3) baseRisk += 0.1;
        
        return Math.min(Math.max(baseRisk, 0.0), 1.0);
    }
    
    /**
     * Pure function: Compute transaction risk from amount patterns.
     */
    private double computeTransactionRisk(List<Transaction> transactions) {
        if (transactions.isEmpty()) return 0.5;
        
        double avgAmount = transactions.stream()
            .map(Transaction::getAmount)
            .map(BigDecimal::doubleValue)
            .mapToDouble(Double::doubleValue)
            .average()
            .orElse(0.0);
        
        double maxAmount = transactions.stream()
            .map(Transaction::getAmount)
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
    
    private boolean isHighRiskTransaction(Transaction tx) {
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
}