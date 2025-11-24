package com.nickfallico.financialriskmanagement.service;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.nickfallico.financialriskmanagement.kafka.event.FraudDetectedEvent;
import com.nickfallico.financialriskmanagement.kafka.event.HighRiskUserIdentifiedEvent;
import com.nickfallico.financialriskmanagement.kafka.event.TransactionBlockedEvent;
import com.nickfallico.financialriskmanagement.service.alert.PagerDutyService;
import com.nickfallico.financialriskmanagement.service.alert.SiemService;
import com.nickfallico.financialriskmanagement.service.alert.SlackService;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FraudAlertServiceTest {

    @Mock
    private SlackService slackService;

    @Mock
    private PagerDutyService pagerDutyService;

    @Mock
    private SiemService siemService;

    private MeterRegistry meterRegistry;
    private FraudAlertService fraudAlertService;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        fraudAlertService = new FraudAlertService(
            meterRegistry,
            slackService,
            pagerDutyService,
            siemService
        );

        // Default: all services return successful Mono
        when(slackService.postToChannel(anyString(), anyString())).thenReturn(Mono.empty());
        when(pagerDutyService.triggerIncident(anyString(), anyString())).thenReturn(Mono.empty());
        when(pagerDutyService.createIncident(anyString(), anyString(), anyString(), anyString()))
            .thenReturn(Mono.just("INC-12345"));
        when(siemService.logSecurityEvent(anyString(), anyString(), anyString(), anyString()))
            .thenReturn(Mono.empty());
    }

    @Test
    void sendFraudAlert_CriticalRiskLevel_SendsToAllChannels() {
        // Arrange
        FraudDetectedEvent event = createFraudDetectedEvent("CRITICAL", 0.95);

        // Act & Assert
        StepVerifier.create(fraudAlertService.sendFraudAlert(event))
            .verifyComplete();

        // Verify Slack alert was sent
        verify(slackService).postToChannel(eq("#fraud-alerts"), anyString());

        // Verify PagerDuty incident was triggered
        verify(pagerDutyService).triggerIncident(anyString(), anyString());

        // Verify SIEM event was logged
        verify(siemService).logSecurityEvent(
            eq("FRAUD_DETECTED"),
            eq("CRITICAL"),
            eq(event.getUserId()),
            anyString()
        );

        // Verify SIEM audit log
        verify(siemService).logSecurityEvent(
            eq("FRAUD_AUDIT"),
            eq("info"),
            eq(event.getUserId()),
            anyString()
        );

        // Verify metrics
        assert meterRegistry.counter("fraud.alerts.sent").count() == 1.0;
        assert meterRegistry.counter("fraud.alerts.critical").count() == 1.0;
    }

    @Test
    void sendFraudAlert_StandardRiskLevel_SendsToMonitoringChannel() {
        // Arrange
        FraudDetectedEvent event = createFraudDetectedEvent("MEDIUM", 0.45);

        // Act & Assert
        StepVerifier.create(fraudAlertService.sendFraudAlert(event))
            .verifyComplete();

        // Verify Slack alert sent to monitoring channel
        verify(slackService).postToChannel(eq("#fraud-monitoring"), anyString());

        // Verify SIEM audit log
        verify(siemService).logSecurityEvent(eq("FRAUD_AUDIT"), anyString(), anyString(), anyString());

        // Verify metrics
        assert meterRegistry.counter("fraud.alerts.sent").count() == 1.0;
        assert meterRegistry.counter("fraud.alerts.critical").count() == 0.0; // Not critical
    }

    @Test
    void sendBlockedTransactionAlert_CriticalSeverity_CreatesPagerDutyIncident() {
        // Arrange
        TransactionBlockedEvent event = createTransactionBlockedEvent("CRITICAL");

        // Act & Assert
        StepVerifier.create(fraudAlertService.sendBlockedTransactionAlert(event))
            .verifyComplete();

        // Verify PagerDuty incident was created
        verify(pagerDutyService).createIncident(
            contains("CRITICAL"),
            eq(event.getBlockReason()),
            eq("critical"),
            anyString()
        );

        // Verify metrics
        assert meterRegistry.counter("fraud.alerts.sent").count() == 1.0;
        assert meterRegistry.counter("fraud.alerts.critical").count() == 1.0;
    }

    @Test
    void sendBlockedTransactionAlert_MediumSeverity_SendsToReviewQueue() {
        // Arrange
        TransactionBlockedEvent event = createTransactionBlockedEvent("MEDIUM");

        // Act & Assert
        StepVerifier.create(fraudAlertService.sendBlockedTransactionAlert(event))
            .verifyComplete();

        // Verify Slack message sent to review queue
        verify(slackService).postToChannel(eq("#fraud-review-queue"), anyString());

        // Verify investigation queue metric
        assert meterRegistry.counter("fraud.investigation_queue_additions", "severity", "MEDIUM").count() == 1.0;
    }

    @Test
    void sendHighRiskUserAlert_Critical_SendsToComplianceChannel() {
        // Arrange
        HighRiskUserIdentifiedEvent event = createHighRiskUserEvent("CRITICAL", 0.85);

        // Act & Assert
        StepVerifier.create(fraudAlertService.sendHighRiskUserAlert(event))
            .verifyComplete();

        // Verify Slack message sent to compliance team
        verify(slackService).postToChannel(eq("#compliance-alerts"), contains("CRITICAL"));

        // Verify metrics
        assert meterRegistry.counter("fraud.high_risk_user_alerts", "severity", "CRITICAL").count() == 1.0;
        assert meterRegistry.counter("fraud.alerts.critical").count() == 1.0;
    }

    @Test
    void sendHighRiskUserAlert_Warning_SendsToMonitoringChannel() {
        // Arrange
        HighRiskUserIdentifiedEvent event = createHighRiskUserEvent("WARNING", 0.65);

        // Act & Assert
        StepVerifier.create(fraudAlertService.sendHighRiskUserAlert(event))
            .verifyComplete();

        // Verify Slack message sent to monitoring channel
        verify(slackService).postToChannel(eq("#user-monitoring"), anyString());
    }

    @Test
    void fraudAlert_HandlesServiceFailures() {
        // Arrange
        FraudDetectedEvent event = createFraudDetectedEvent("CRITICAL", 0.92);

        // Simulate Slack service failure
        when(slackService.postToChannel(anyString(), anyString()))
            .thenReturn(Mono.error(new RuntimeException("Slack unavailable")));

        // Act & Assert - should propagate error
        StepVerifier.create(fraudAlertService.sendFraudAlert(event))
            .expectError(RuntimeException.class)
            .verify();
    }

    // Helper methods to create test events

    private FraudDetectedEvent createFraudDetectedEvent(String riskLevel, double fraudProbability) {
        return FraudDetectedEvent.builder()
            .transactionId(UUID.randomUUID())
            .userId("user123")
            .amount(new BigDecimal("5000.00"))
            .currency("USD")
            .merchantCategory("GAMBLING")
            .isInternational(true)
            .fraudProbability(fraudProbability)
            .violatedRules(List.of("HIGH_AMOUNT", "HIGH_RISK_MERCHANT"))
            .riskLevel(riskLevel)
            .action("REVIEW")
            .eventTimestamp(Instant.now())
            .eventId(UUID.randomUUID())
            .eventSource("fraud-detection-service")
            .build();
    }

    private TransactionBlockedEvent createTransactionBlockedEvent(String severity) {
        return TransactionBlockedEvent.builder()
            .transactionId(UUID.randomUUID())
            .userId("user456")
            .amount(new BigDecimal("10000.00"))
            .currency("USD")
            .merchantCategory("CRYPTOCURRENCY")
            .isInternational(true)
            .blockReason("CRITICAL_FRAUD_THRESHOLD_EXCEEDED")
            .violatedRules(List.of("IMPOSSIBLE_TRAVEL", "EXTREME_AMOUNT"))
            .fraudProbability(0.97)
            .severity(severity)
            .eventTimestamp(Instant.now())
            .eventId(UUID.randomUUID())
            .eventSource("fraud-detection-service")
            .build();
    }

    private HighRiskUserIdentifiedEvent createHighRiskUserEvent(String severity, double riskScore) {
        return HighRiskUserIdentifiedEvent.builder()
            .userId("user789")
            .overallRiskScore(riskScore)
            .riskThreshold(0.75)
            .riskFactors(List.of("MULTIPLE_HIGH_VALUE_TX", "UNUSUAL_PATTERNS"))
            .totalTransactions(45)
            .highRiskTransactions(12)
            .internationalTransactions(8)
            .totalTransactionValue(50000.0)
            .alertSeverity(severity)
            .recommendedAction("REVIEW")
            .eventTimestamp(Instant.now())
            .eventId(UUID.randomUUID())
            .eventSource("risk-assessment-service")
            .build();
    }
}
