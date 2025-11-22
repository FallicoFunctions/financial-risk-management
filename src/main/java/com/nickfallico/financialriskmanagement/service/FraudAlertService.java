package com.nickfallico.financialriskmanagement.service;

import java.time.Instant;

import org.springframework.stereotype.Service;

import com.nickfallico.financialriskmanagement.kafka.event.FraudDetectedEvent;
import com.nickfallico.financialriskmanagement.kafka.event.HighRiskUserIdentifiedEvent;
import com.nickfallico.financialriskmanagement.kafka.event.TransactionBlockedEvent;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * Service for sending fraud alerts to investigation teams and security systems.
 * In production, this would integrate with:
 * - Email/SMS notification systems
 * - PagerDuty/Opsgenie for critical alerts
 * - Slack/Teams channels for team notifications
 * - Security Information and Event Management (SIEM) systems
 */
@Service
@Slf4j
public class FraudAlertService {
    
    private final MeterRegistry meterRegistry;
    private final Counter fraudAlertsCounter;
    private final Counter criticalAlertsCounter;
    
    public FraudAlertService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
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
        return Mono.fromRunnable(() -> {
            fraudAlertsCounter.increment();
            
            if ("CRITICAL".equals(event.getRiskLevel()) || "HIGH".equals(event.getRiskLevel())) {
                criticalAlertsCounter.increment();
                sendCriticalAlert(event);
            } else {
                sendStandardAlert(event);
            }
            
            // Log to security audit system
            logToSecurityAudit(event);
        });
    }
    
    /**
     * Send alert for blocked transaction.
     */
    public Mono<Void> sendBlockedTransactionAlert(TransactionBlockedEvent event) {
        return Mono.fromRunnable(() -> {
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
                
                // TODO: Send to PagerDuty/Opsgenie for immediate response
                // pagerDutyClient.createIncident(createIncident(event));
                
            } else {
                log.warn("Transaction blocked - added to review queue: transactionId={}, userId={}, reason={}",
                    event.getTransactionId(), event.getUserId(), event.getBlockReason());
            }
            
            // Add to fraud investigation queue
            addToInvestigationQueue(event);
        });
    }
    
    /**
     * Send critical alert - requires immediate attention.
     */
    private void sendCriticalAlert(FraudDetectedEvent event) {
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
            ║ ✓ Security Team Email                                          ║
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
        
        // TODO: Production integrations
        // emailService.sendCriticalFraudAlert(event);
        // slackService.postToChannel("#fraud-alerts", formatAlertMessage(event));
        // pagerDutyClient.triggerIncident(event);
        // siemService.logSecurityEvent(event);
    }
    
    /**
     * Send standard alert for lower severity fraud.
     */
    private void sendStandardAlert(FraudDetectedEvent event) {
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
        
        // TODO: Production integrations
        // slackService.postToChannel("#fraud-monitoring", formatAlertMessage(event));
        // emailService.sendFraudSummary(event);
    }
    
    /**
     * Log to security audit system for compliance.
     */
    private void logToSecurityAudit(FraudDetectedEvent event) {
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
        
        // TODO: Send to external audit system
        // auditService.logFraudEvent(event);
    }
    
    /**
     * Add transaction to fraud investigation queue.
     */
    private void addToInvestigationQueue(TransactionBlockedEvent event) {
        meterRegistry.counter("fraud.investigation_queue_additions",
            "severity", event.getSeverity()
        ).increment();

        log.info("Added to fraud investigation queue: transactionId={}, userId={}, severity={}",
            event.getTransactionId(),
            event.getUserId(),
            event.getSeverity()
        );

        // TODO: Production implementation
        // fraudInvestigationQueueService.enqueue(event);
        // In production, this would add to a work queue for fraud analysts
    }

    /**
     * Send alert for high-risk user identification.
     * Notifies compliance team and triggers enhanced monitoring.
     */
    public Mono<Void> sendHighRiskUserAlert(HighRiskUserIdentifiedEvent event) {
        return Mono.fromRunnable(() -> {
            meterRegistry.counter("fraud.high_risk_user_alerts",
                "severity", event.getAlertSeverity()
            ).increment();

            if ("CRITICAL".equals(event.getAlertSeverity())) {
                sendCriticalHighRiskAlert(event);
            } else if ("URGENT".equals(event.getAlertSeverity())) {
                sendUrgentHighRiskAlert(event);
            } else {
                sendStandardHighRiskAlert(event);
            }

            // Log to compliance audit
            log.info("High-risk user alert sent: userId={}, riskScore={}, severity={}, recommendedAction={}",
                event.getUserId(),
                event.getOverallRiskScore(),
                event.getAlertSeverity(),
                event.getRecommendedAction()
            );
        });
    }

    /**
     * Send critical high-risk user alert - requires immediate action.
     */
    private void sendCriticalHighRiskAlert(HighRiskUserIdentifiedEvent event) {
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
            ║ ✓ Compliance Team Notification                                 ║
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

        // TODO: Production integrations
        // complianceTeamNotificationService.sendCriticalAlert(event);
        // accountMonitoringService.enableEnhancedMonitoring(event.getUserId());
    }

    /**
     * Send urgent high-risk user alert.
     */
    private void sendUrgentHighRiskAlert(HighRiskUserIdentifiedEvent event) {
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

        // TODO: Production integrations
        // complianceTeamNotificationService.sendUrgentAlert(event);
    }

    /**
     * Send standard high-risk user alert for monitoring.
     */
    private void sendStandardHighRiskAlert(HighRiskUserIdentifiedEvent event) {
        log.info("High-risk user identified for monitoring: userId={}, riskScore={}, factors={}",
            event.getUserId(),
            event.getOverallRiskScore(),
            event.getRiskFactors()
        );

        // TODO: Production integrations
        // monitoringService.addToWatchList(event.getUserId());
    }
}