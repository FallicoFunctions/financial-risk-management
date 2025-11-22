package com.nickfallico.financialriskmanagement.controller;

import com.nickfallico.financialriskmanagement.dto.FlaggedTransactionDTO;
import com.nickfallico.financialriskmanagement.dto.FraudRuleDTO;
import com.nickfallico.financialriskmanagement.dto.TransactionReviewRequestDTO;
import com.nickfallico.financialriskmanagement.eventstore.model.EventLog;
import com.nickfallico.financialriskmanagement.eventstore.repository.EventLogRepository;
import com.nickfallico.financialriskmanagement.eventstore.service.EventStoreService;
import com.nickfallico.financialriskmanagement.model.Transactions;
import com.nickfallico.financialriskmanagement.repository.TransactionRepository;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import jakarta.validation.Valid;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    private final EventLogRepository eventLogRepository;
    private final TransactionRepository transactionRepository;
    private final EventStoreService eventStoreService;
    private final MeterRegistry meterRegistry;

    @GetMapping("/flagged-transactions")
    public Mono<ResponseEntity<List<FlaggedTransactionDTO>>> getFlaggedTransactions(
        @RequestParam(required = false) String status,
        @RequestParam(defaultValue = "50") int limit
    ) {
        log.info("Fetching flagged transactions - status: {}, limit: {}", status, limit);
        meterRegistry.counter("api.admin.flagged_transactions.requests").increment();

        return eventLogRepository.findAll()
            .filter(event -> "FRAUD_DETECTED".equals(event.getEventType()))
            .collectList()
            .flatMap(fraudEvents -> {
                Map<String, EventLog> latestFraudByTransaction = fraudEvents.stream()
                    .collect(Collectors.toMap(
                        EventLog::getAggregateId,
                        event -> event,
                        (e1, e2) -> e1.getSequenceNumber() > e2.getSequenceNumber() ? e1 : e2
                    ));

                List<String> transactionIds = new ArrayList<>(latestFraudByTransaction.keySet());

                return transactionRepository.findAllById(
                    transactionIds.stream()
                        .map(UUID::fromString)
                        .collect(Collectors.toList())
                )
                .collectList()
                .map(transactions -> {
                    List<FlaggedTransactionDTO> flagged = transactions.stream()
                        .map(tx -> buildFlaggedDTO(tx, latestFraudByTransaction.get(tx.getId().toString())))
                        .sorted((a, b) -> b.getFlaggedAt().compareTo(a.getFlaggedAt()))
                        .limit(limit)
                        .collect(Collectors.toList());

                    log.info("Retrieved {} flagged transactions", flagged.size());
                    return ResponseEntity.ok(flagged);
                });
            })
            .doOnError(error -> {
                log.error("Failed to retrieve flagged transactions", error);
                meterRegistry.counter("api.admin.flagged_transactions.errors").increment();
            });
    }

    @PutMapping("/transactions/{transactionId}/review")
    public Mono<ResponseEntity<Map<String, String>>> reviewTransaction(
        @PathVariable UUID transactionId,
        @Valid @RequestBody TransactionReviewRequestDTO request
    ) {
        log.info("Reviewing transaction {} - decision: {}, reviewer: {}",
            transactionId, request.getDecision(), request.getReviewerId());

        meterRegistry.counter("api.admin.transaction_review.requests").increment();

        return transactionRepository.findById(transactionId)
            .switchIfEmpty(Mono.error(new RuntimeException("Transaction not found: " + transactionId)))
            .flatMap(transaction -> {
                String eventType = mapDecisionToEventType(request.getDecision());
                Map<String, Object> eventData = new HashMap<>();
                eventData.put("transactionId", transactionId.toString());
                eventData.put("reviewerId", request.getReviewerId());
                eventData.put("decision", request.getDecision().toString());
                eventData.put("notes", request.getNotes());
                eventData.put("reviewedAt", Instant.now().toString());

                return eventStoreService.publishEvent(
                    transactionId.toString(),
                    "TRANSACTION",
                    eventType,
                    eventData,
                    Map.of("reviewer", request.getReviewerId())
                )
                .then(Mono.just(ResponseEntity.ok(Map.of(
                    "transactionId", transactionId.toString(),
                    "status", eventType,
                    "message", "Transaction review recorded successfully",
                    "reviewedBy", request.getReviewerId()
                ))));
            })
            .doOnSuccess(result -> {
                log.info("Transaction {} reviewed successfully", transactionId);
                meterRegistry.counter("api.admin.transaction_review.success").increment();
            })
            .doOnError(error -> {
                log.error("Failed to review transaction: {}", transactionId, error);
                meterRegistry.counter("api.admin.transaction_review.errors").increment();
            });
    }

    @GetMapping("/fraud-rules")
    public Mono<ResponseEntity<List<FraudRuleDTO>>> getFraudRules() {
        log.info("Fetching fraud detection rules");
        meterRegistry.counter("api.admin.fraud_rules.requests").increment();

        List<FraudRuleDTO> rules = Arrays.asList(
            FraudRuleDTO.builder()
                .ruleId("RULE_HIGH_AMOUNT")
                .ruleName("High Amount Transaction")
                .description("Flags transactions above $10,000")
                .riskWeight(0.7)
                .isActive(true)
                .category("AMOUNT")
                .triggerCount(0)
                .build(),
            FraudRuleDTO.builder()
                .ruleId("RULE_HIGH_RISK_MERCHANT")
                .ruleName("High Risk Merchant Category")
                .description("Flags gambling, adult content, and cryptocurrency merchants")
                .riskWeight(0.8)
                .isActive(true)
                .category("BEHAVIOR")
                .triggerCount(0)
                .build(),
            FraudRuleDTO.builder()
                .ruleId("RULE_INTERNATIONAL_NEW_USER")
                .ruleName("International Transaction - New User")
                .description("International transactions from users with limited history")
                .riskWeight(0.6)
                .isActive(true)
                .category("LOCATION")
                .triggerCount(0)
                .build(),
            FraudRuleDTO.builder()
                .ruleId("RULE_VELOCITY")
                .ruleName("Transaction Velocity")
                .description("Multiple transactions in short time period")
                .riskWeight(0.75)
                .isActive(true)
                .category("VELOCITY")
                .triggerCount(0)
                .build(),
            FraudRuleDTO.builder()
                .ruleId("RULE_GEOGRAPHIC_ANOMALY")
                .ruleName("Geographic Anomaly")
                .description("Transaction from unusual or new location")
                .riskWeight(0.65)
                .isActive(true)
                .category("LOCATION")
                .triggerCount(0)
                .build(),
            FraudRuleDTO.builder()
                .ruleId("RULE_IMPOSSIBLE_TRAVEL")
                .ruleName("Impossible Travel")
                .description("Transactions from distant locations in impossible timeframe")
                .riskWeight(0.9)
                .isActive(true)
                .category("LOCATION")
                .triggerCount(0)
                .build(),
            FraudRuleDTO.builder()
                .ruleId("RULE_AMOUNT_SPIKE")
                .ruleName("Amount Spike Detection")
                .description("Transaction significantly higher than user's average")
                .riskWeight(0.7)
                .isActive(true)
                .category("AMOUNT")
                .triggerCount(0)
                .build(),
            FraudRuleDTO.builder()
                .ruleId("RULE_ODD_HOUR")
                .ruleName("Odd Hour Activity")
                .description("Transactions during unusual hours (2 AM - 5 AM)")
                .riskWeight(0.5)
                .isActive(true)
                .category("BEHAVIOR")
                .triggerCount(0)
                .build(),
            FraudRuleDTO.builder()
                .ruleId("RULE_UNUSED_MERCHANT")
                .ruleName("Unused Merchant Category")
                .description("First time using a merchant category")
                .riskWeight(0.4)
                .isActive(true)
                .category("BEHAVIOR")
                .triggerCount(0)
                .build(),
            FraudRuleDTO.builder()
                .ruleId("RULE_DEVIATION_FROM_AVERAGE")
                .ruleName("Deviation from Average")
                .description("Transaction deviates significantly from user patterns")
                .riskWeight(0.6)
                .isActive(true)
                .category("AMOUNT")
                .triggerCount(0)
                .build(),
            FraudRuleDTO.builder()
                .ruleId("RULE_NEW_USER")
                .ruleName("New User Onboarding")
                .description("Higher scrutiny for users with minimal transaction history")
                .riskWeight(0.3)
                .isActive(true)
                .category("BEHAVIOR")
                .triggerCount(0)
                .build()
        );

        log.info("Retrieved {} fraud detection rules", rules.size());
        return Mono.just(ResponseEntity.ok(rules));
    }

    private FlaggedTransactionDTO buildFlaggedDTO(Transactions transaction, EventLog fraudEvent) {
        Map<String, Object> eventData = fraudEvent.getEventData();

        Double fraudProbability = 0.0;
        String riskLevel = "MEDIUM";
        List<String> fraudReasons = new ArrayList<>();

        if (eventData != null) {
            if (eventData.containsKey("fraudProbability")) {
                fraudProbability = Double.parseDouble(eventData.get("fraudProbability").toString());
            }
            if (eventData.containsKey("riskLevel")) {
                riskLevel = eventData.get("riskLevel").toString();
            }
            if (eventData.containsKey("reason")) {
                fraudReasons.add(eventData.get("reason").toString());
            }
        }

        long daysSince = Duration.between(fraudEvent.getCreatedAt(), Instant.now()).toDays();

        return FlaggedTransactionDTO.builder()
            .transactionId(transaction.getId())
            .userId(transaction.getUserId())
            .amount(transaction.getAmount())
            .currency(transaction.getCurrency())
            .merchantCategory(transaction.getMerchantCategory())
            .merchantName(transaction.getMerchantName())
            .flaggedAt(fraudEvent.getCreatedAt())
            .fraudProbability(fraudProbability)
            .riskLevel(riskLevel)
            .fraudReasons(fraudReasons)
            .reviewStatus("PENDING")
            .daysSinceFlagged((int) daysSince)
            .build();
    }

    private String mapDecisionToEventType(TransactionReviewRequestDTO.ReviewDecision decision) {
        return switch (decision) {
            case APPROVE -> "FRAUD_CLEARED";
            case REJECT -> "FRAUD_CONFIRMED";
            case ESCALATE -> "FRAUD_ESCALATED";
        };
    }
}