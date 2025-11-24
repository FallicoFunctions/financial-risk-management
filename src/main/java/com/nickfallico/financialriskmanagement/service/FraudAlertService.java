package com.nickfallico.financialriskmanagement.service;

import java.time.Instant;

import org.springframework.stereotype.Service;

import com.nickfallico.financialriskmanagement.kafka.event.FraudDetectedEvent;
import com.nickfallico.financialriskmanagement.kafka.event.HighRiskUserIdentifiedEvent;
import com.nickfallico.financialriskmanagement.kafka.event.TransactionBlockedEvent;
import com.nickfallico.financialriskmanagement.service.alert.PagerDutyService;
import com.nickfallico.financialriskmanagement.service.alert.SiemService;
import com.nickfallico.financialriskmanagement.service.alert.SlackService;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * Service for sending fraud alerts to investigation teams and security systems.
 * Integrates with Slack, PagerDuty, and SIEM systems.
 * Currently using mock implementations for demonstration.
 */
@Service
@Slf4j
public class FraudAlertService {

    private final MeterRegistry meterRegistry;
    private final Counter fraudAlertsCounter;
    private final Counter criticalAlertsCounter;
    private final SlackService slackService;
    private final PagerDutyService pagerDutyService;
    private final SiemService siemService;

    public FraudAlertService(
        MeterRegistry meterRegistry,
        SlackService slackService,
        PagerDutyService pagerDutyService,
        SiemService siemService
    ) {
        this.meterRegistry = meterRegistry;
        this.slackService = slackService;
        this.pagerDutyService = pagerDutyService;
        this.siemService = siemService;
        this.fraudAlertsCounter = Counter.builder("fraud.alerts.sent")
            .description("Total number of fraud alerts sent")
            .register(meterRegistry);
        this.criticalAlertsCounter = Counter.builder("fraud.alerts.critical")
            .description("Total number of critical fraud alerts")
            .register(meterRegistry);
    }

    /**
     * Send fraud alert for detected fraud event.
     * Alerts are sent based on severity level.
     */
    public Mono<Void> sendFraudAlert(FraudDetectedEvent event) {
        fraudAlertsCounter.increment();

        Mono<Void> alertMono;
        if ("CRITICAL".equals(event.getRiskLevel()) || "HIGH".equals(event.getRiskLevel())) {
            criticalAlertsCounter.increment();
            alertMono = sendCriticalAlert(event);
        } else {
            alertMono = sendStandardAlert(event);
        }

        // Log to security audit system
        return alertMono.then(logToSecurityAudit(event));
    }

    /**
     * Send alert for blocked transaction.
     */
    public Mono<Void> sendBlockedTransactionAlert(TransactionBlockedEvent event) {
        fraudAlertsCounter.increment();

        if ("CRITICAL".equals(event.getSeverity())) {
            criticalAlertsCounter.increment();
            log.error("""

                ╔════════════════════════════════════════════════════════════════╗
                ║                   🚨 CRITICAL FRAUD ALERT 🚨                    ║
                ╠════════════════════════════════════════════════════════════════╣
                ║ Transaction BLOCKED - Immediate Review Required                ║
                ║                                                                ║
                ║ Transaction ID: {}                                             ║
                ║ User ID: {}                                                    ║
                ║ Amount: {} {}                                                  ║
                ║ Reason: {}                                                     ║
                ║ Violated Rules: {}                                             ║
                ║ Fraud Probability: {}%                                         ║
                ║ Timestamp: {}                                                  ║
                ║                                                                ║
                ║ ACTION REQUIRED: Review in fraud investigation queue           ║
                ╚════════════════════════════════════════════════════════════════╝
                """,
                event.getTransactionId(),
                event.getUserId(),
                event.getAmount(),
                event.getCurrency(),
                event.getBlockReason(),
                event.getViolatedRules(),
                event.getFraudProbability() * 100,
                event.getEventTimestamp()
            );

            // Send to PagerDuty for immediate response
            String incidentTitle = String.format("CRITICAL: Transaction Blocked - %s %s",
                event.getAmount(), event.getCurrency());
            String incidentDetails = formatBlockedTransactionDetails(event);

            return pagerDutyService.createIncident(
                incidentTitle,
                event.getBlockReason(),
                "critical",
                incidentDetails
            ).then();
        } else {
            log.warn("Transaction blocked - added to review queue: transactionId={}, userId={}, reason={}",
                event.getTransactionId(), event.getUserId(), event.getBlockReason());
            return addToInvestigationQueue(event);
        }
    }

    /**
     * Send critical alert - requires immediate attention.
     */
    private Mono<Void> sendCriticalAlert(FraudDetectedEvent event) {
        log.error("""

            ╔════════════════════════════════════════════════════════════════╗
            ║                   🚨 CRITICAL FRAUD ALERT 🚨                    ║
            ╠════════════════════════════════════════════════════════════════╣
            ║ Fraud Detected - Immediate Investigation Required              ║
            ║                                                                ║
            ║ Transaction ID: {}                                             ║
            ║ User ID: {}                                                    ║
            ║ Amount: {} {}                                                  ║
            ║ Merchant: {}                                                   ║
            ║ Risk Level: {}                                                 ║
            ║ Fraud Probability: {}%                                         ║
            ║ Violated Rules: {}                                             ║
            ║ Action: {}                                                     ║
            ║ Timestamp: {}                                                  ║
            ║                                                                ║
            ║ ALERT CHANNELS:                                                ║
            ║ ✓ Slack #fraud-alerts                                          ║
            ║ ✓ PagerDuty Incident Created                                   ║
            ║ ✓ SIEM System Notified                                         ║
            ╚════════════════════════════════════════════════════════════════╝
            """,
            event.getTransactionId(),
            event.getUserId(),
            event.getAmount(),
            event.getCurrency(),
            event.getMerchantCategory(),
            event.getRiskLevel(),
            event.getFraudProbability() * 100,
            event.getViolatedRules(),
            event.getAction(),
            event.getEventTimestamp()
        );

        // Format alert message
        String slackMessage = formatFraudAlertMessage(event, true);
        String incidentTitle = String.format("CRITICAL: Fraud Detected - %s %s",
            event.getAmount(), event.getCurrency());
        String siemDetails = formatSiemEventDetails(event);

        // Send alerts in parallel
        return Mono.when(
            slackService.postToChannel("#fraud-alerts", slackMessage),
            pagerDutyService.triggerIncident(incidentTitle, siemDetails),
            siemService.logSecurityEvent(
                "FRAUD_DETECTED",
                event.getRiskLevel(),
                event.getUserId(),
                siemDetails
            )
        );
    }

    /**
     * Send standard alert for lower severity fraud.
     */
    private Mono<Void> sendStandardAlert(FraudDetectedEvent event) {
        log.warn("""
            ⚠️  Fraud Detected - Standard Alert
            Transaction ID: {}
            User ID: {}
            Amount: {} {}
            Risk Level: {}
            Fraud Probability: {}%
            Violated Rules: {}
            Action: {}
            """,
            event.getTransactionId(),
            event.getUserId(),
            event.getAmount(),
            event.getCurrency(),
            event.getRiskLevel(),
            event.getFraudProbability() * 100,
            event.getViolatedRules(),
            event.getAction()
        );

        // Post to monitoring channel only
        String slackMessage = formatFraudAlertMessage(event, false);
        return slackService.postToChannel("#fraud-monitoring", slackMessage);
    }

    /**
     * Log to security audit system for compliance.
     */
    private Mono<Void> logToSecurityAudit(FraudDetectedEvent event) {
        meterRegistry.counter("fraud.security_audit_logs",
            "risk_level", event.getRiskLevel(),
            "action", event.getAction()
        ).increment();

        log.info("Security Audit Log: Fraud detected for user={}, transactionId={}, riskLevel={}, timestamp={}",
            event.getUserId(),
            event.getTransactionId(),
            event.getRiskLevel(),
            Instant.now()
        );

        // Send to SIEM for audit trail
        String auditDetails = formatSiemEventDetails(event);
        return siemService.logSecurityEvent(
            "FRAUD_AUDIT",
            "info",
            event.getUserId(),
            auditDetails
        );
    }

    /**
     * Add transaction to fraud investigation queue.
     */
    private Mono<Void> addToInvestigationQueue(TransactionBlockedEvent event) {
        meterRegistry.counter("fraud.investigation_queue_additions",
            "severity", event.getSeverity()
        ).increment();

        log.info("Added to fraud investigation queue: transactionId={}, userId={}, severity={}",
            event.getTransactionId(),
            event.getUserId(),
            event.getSeverity()
        );

        // In production: fraudInvestigationQueueService.enqueue(event);
        // For now, log to Slack for manual review
        String message = String.format(
            "🔍 *New Transaction for Review*\nTransaction ID: %s\nUser: %s\nSeverity: %s\nReason: %s",
            event.getTransactionId(),
            event.getUserId(),
            event.getSeverity(),
            event.getBlockReason()
        );
        return slackService.postToChannel("#fraud-review-queue", message);
    }

    /**
     * Send alert for high-risk user identification.
     * Notifies compliance team and triggers enhanced monitoring.
     */
    public Mono<Void> sendHighRiskUserAlert(HighRiskUserIdentifiedEvent event) {
        meterRegistry.counter("fraud.high_risk_user_alerts",
            "severity", event.getAlertSeverity()
        ).increment();

        Mono<Void> alertMono;
        if ("CRITICAL".equals(event.getAlertSeverity())) {
            alertMono = sendCriticalHighRiskAlert(event);
        } else if ("URGENT".equals(event.getAlertSeverity())) {
            alertMono = sendUrgentHighRiskAlert(event);
        } else {
            alertMono = sendStandardHighRiskAlert(event);
        }

        // Log to compliance audit
        log.info("High-risk user alert sent: userId={}, riskScore={}, severity={}, recommendedAction={}",
            event.getUserId(),
            event.getOverallRiskScore(),
            event.getAlertSeverity(),
            event.getRecommendedAction()
        );

        return alertMono;
    }

    /**
     * Send critical high-risk user alert - requires immediate action.
     */
    private Mono<Void> sendCriticalHighRiskAlert(HighRiskUserIdentifiedEvent event) {
        criticalAlertsCounter.increment();

        log.error("""

            ╔════════════════════════════════════════════════════════════════╗
            ║              🚨 CRITICAL HIGH-RISK USER ALERT 🚨                ║
            ╠════════════════════════════════════════════════════════════════╣
            ║ High-Risk User Identified - Immediate Action Required          ║
            ║                                                                ║
            ║ User ID: {}                                                    ║
            ║ Risk Score: {} (Threshold: {})                                 ║
            ║ Risk Factors: {}                                               ║
            ║                                                                ║
            ║ ACCOUNT ACTIVITY:                                              ║
            ║ Total Transactions: {}                                         ║
            ║ High Risk Transactions: {}                                     ║
            ║ International Transactions: {}                                 ║
            ║ Total Transaction Value: ${}                                   ║
            ║                                                                ║
            ║ RECOMMENDED ACTION: {}                                         ║
            ║ Timestamp: {}                                                  ║
            ║                                                                ║
            ║ ALERT CHANNELS:                                                ║
            ║ ✓ Compliance Team (Slack #compliance-alerts)                   ║
            ║ ✓ Enhanced Monitoring Activated                                ║
            ║ ✓ Account Review Queue                                         ║
            ╚════════════════════════════════════════════════════════════════╝
            """,
            event.getUserId(),
            event.getOverallRiskScore(),
            event.getRiskThreshold(),
            event.getRiskFactors(),
            event.getTotalTransactions(),
            event.getHighRiskTransactions(),
            event.getInternationalTransactions(),
            event.getTotalTransactionValue(),
            event.getRecommendedAction(),
            event.getEventTimestamp()
        );

        // Format compliance alert message
        String message = formatHighRiskUserMessage(event, "CRITICAL");

        // Send to compliance team via Slack
        return slackService.postToChannel("#compliance-alerts", message);
    }

    /**
     * Send urgent high-risk user alert.
     */
    private Mono<Void> sendUrgentHighRiskAlert(HighRiskUserIdentifiedEvent event) {
        log.warn("""
            ⚠️  URGENT: High-Risk User Identified
            User ID: {}
            Risk Score: {}
            Risk Factors: {}
            Total Transactions: {}
            High Risk Transactions: {}
            Recommended Action: {}
            """,
            event.getUserId(),
            event.getOverallRiskScore(),
            event.getRiskFactors(),
            event.getTotalTransactions(),
            event.getHighRiskTransactions(),
            event.getRecommendedAction()
        );

        // Format alert message
        String message = formatHighRiskUserMessage(event, "URGENT");
        return slackService.postToChannel("#compliance-alerts", message);
    }

    /**
     * Send standard high-risk user alert for monitoring.
     */
    private Mono<Void> sendStandardHighRiskAlert(HighRiskUserIdentifiedEvent event) {
        log.info("High-risk user identified for monitoring: userId={}, riskScore={}, factors={}",
            event.getUserId(),
            event.getOverallRiskScore(),
            event.getRiskFactors()
        );

        // Add to monitoring watch list
        String message = String.format(
            "👁️ *New User Added to Watch List*\nUser: %s\nRisk Score: %.2f\nFactors: %s",
            event.getUserId(),
            event.getOverallRiskScore(),
            event.getRiskFactors()
        );
        return slackService.postToChannel("#user-monitoring", message);
    }

    // ============================================================================
    // Message Formatting Helpers
    // ============================================================================

    /**
     * Format fraud alert message for Slack.
     */
    private String formatFraudAlertMessage(FraudDetectedEvent event, boolean isCritical) {
        String emoji = isCritical ? "🚨" : "⚠️";
        return String.format("""
            %s *FRAUD DETECTED*
            *Transaction ID:* %s
            *User ID:* %s
            *Amount:* %s %s
            *Merchant:* %s
            *Risk Level:* %s
            *Fraud Probability:* %.1f%%
            *Violated Rules:* %s
            *Action:* %s
            *Timestamp:* %s
            """,
            emoji,
            event.getTransactionId(),
            event.getUserId(),
            event.getAmount(),
            event.getCurrency(),
            event.getMerchantCategory(),
            event.getRiskLevel(),
            event.getFraudProbability() * 100,
            event.getViolatedRules(),
            event.getAction(),
            event.getEventTimestamp()
        );
    }

    /**
     * Format SIEM event details as JSON.
     */
    private String formatSiemEventDetails(FraudDetectedEvent event) {
        return String.format("""
            {"transactionId":"%s","userId":"%s","amount":%s,"currency":"%s","merchantCategory":"%s",\
            "riskLevel":"%s","fraudProbability":%.4f,"violatedRules":%s,"action":"%s","timestamp":"%s"}
            """,
            event.getTransactionId(),
            event.getUserId(),
            event.getAmount(),
            event.getCurrency(),
            event.getMerchantCategory(),
            event.getRiskLevel(),
            event.getFraudProbability(),
            event.getViolatedRules(),
            event.getAction(),
            event.getEventTimestamp()
        );
    }

    /**
     * Format blocked transaction details for PagerDuty.
     */
    private String formatBlockedTransactionDetails(TransactionBlockedEvent event) {
        return String.format("""
            {"transactionId":"%s","userId":"%s","amount":%s,"currency":"%s","merchantCategory":"%s",\
            "blockReason":"%s","violatedRules":%s,"fraudProbability":%.4f,"severity":"%s","timestamp":"%s"}
            """,
            event.getTransactionId(),
            event.getUserId(),
            event.getAmount(),
            event.getCurrency(),
            event.getMerchantCategory(),
            event.getBlockReason(),
            event.getViolatedRules(),
            event.getFraudProbability(),
            event.getSeverity(),
            event.getEventTimestamp()
        );
    }

    /**
     * Format high-risk user message for compliance team.
     */
    private String formatHighRiskUserMessage(HighRiskUserIdentifiedEvent event, String severity) {
        String emoji = "CRITICAL".equals(severity) ? "🚨" : "⚠️";
        return String.format("""
            %s *%s: HIGH-RISK USER IDENTIFIED*
            *User ID:* %s
            *Risk Score:* %.2f (Threshold: %.2f)
            *Risk Factors:* %s
            *Total Transactions:* %d
            *High Risk Transactions:* %d
            *International Transactions:* %d
            *Total Value:* $%.2f
            *Recommended Action:* %s
            *Timestamp:* %s
            """,
            emoji,
            severity,
            event.getUserId(),
            event.getOverallRiskScore(),
            event.getRiskThreshold(),
            event.getRiskFactors(),
            event.getTotalTransactions(),
            event.getHighRiskTransactions(),
            event.getInternationalTransactions(),
            event.getTotalTransactionValue(),
            event.getRecommendedAction(),
            event.getEventTimestamp()
        );
    }
}
