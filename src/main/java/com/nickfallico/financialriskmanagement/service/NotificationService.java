package com.nickfallico.financialriskmanagement.service;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.nickfallico.financialriskmanagement.service.notification.EmailService;
import com.nickfallico.financialriskmanagement.service.notification.PushNotificationService;
import com.nickfallico.financialriskmanagement.service.notification.SmsService;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * Service for sending notifications to users about their transactions.
 * Integrates with SMS, Email, and Push Notification services.
 * Currently using mock implementations for demonstration.
 */
@Service
@Slf4j
public class NotificationService {

    private final MeterRegistry meterRegistry;
    private final Counter notificationsSentCounter;
    private final SmsService smsService;
    private final EmailService emailService;
    private final PushNotificationService pushNotificationService;

    public NotificationService(
        MeterRegistry meterRegistry,
        SmsService smsService,
        EmailService emailService,
        PushNotificationService pushNotificationService
    ) {
        this.meterRegistry = meterRegistry;
        this.smsService = smsService;
        this.emailService = emailService;
        this.pushNotificationService = pushNotificationService;
        this.notificationsSentCounter = Counter.builder("notifications.sent")
            .description("Total number of notifications sent to users")
            .register(meterRegistry);
    }

    /**
     * Send transaction confirmation notification via SMS and push.
     */
    public Mono<Void> sendTransactionConfirmation(
        String userId,
        UUID transactionId,
        BigDecimal amount,
        String currency,
        String merchantCategory
    ) {
        notificationsSentCounter.increment();
        meterRegistry.counter("notifications.transaction_confirmation").increment();

        log.info("Sending transaction confirmation: userId={}, transactionId={}, amount={} {}",
            userId, transactionId, amount, currency);

        // Format messages
        String smsMessage = formatTransactionSMS(amount, currency, merchantCategory);
        String pushTitle = "Transaction Approved";
        String pushMessage = String.format("%s %s at %s", amount, currency, merchantCategory);
        String pushData = String.format("{\"transactionId\":\"%s\",\"type\":\"CONFIRMATION\"}", transactionId);

        // Send via multiple channels
        return Mono.when(
            smsService.sendSms(userId, getUserPhoneNumber(userId), smsMessage),
            pushNotificationService.sendPushNotification(userId, pushTitle, pushMessage, pushData)
        );
    }

    /**
     * Send suspicious activity alert to user via email.
     * NOTE: Be careful - alerting users to fraud detection can tip off fraudsters.
     * Only send if user opted in to fraud alerts.
     */
    public Mono<Void> sendSuspiciousActivityAlert(
        String userId,
        UUID transactionId,
        String reason
    ) {
        notificationsSentCounter.increment();
        meterRegistry.counter("notifications.suspicious_activity").increment();

        log.warn("Sending suspicious activity alert: userId={}, transactionId={}, reason={}",
            userId, transactionId, reason);

        // Check if user opted in to fraud alerts (mock check)
        if (!userOptedInToFraudAlerts(userId)) {
            log.info("User {} has not opted in to fraud alerts - skipping notification", userId);
            return Mono.empty();
        }

        // Format email
        String subject = "Suspicious Activity Detected on Your Account";
        String body = formatSuspiciousActivityEmail(transactionId, reason);

        return emailService.sendEmail(userId, getUserEmail(userId), subject, body);
    }

    /**
     * Send transaction blocked notification via SMS and email.
     */
    public Mono<Void> sendTransactionBlockedNotification(
        String userId,
        UUID transactionId,
        BigDecimal amount,
        String currency,
        String reason
    ) {
        notificationsSentCounter.increment();
        meterRegistry.counter("notifications.transaction_blocked").increment();

        log.warn("Sending transaction blocked notification: userId={}, transactionId={}, amount={} {}, reason={}",
            userId, transactionId, amount, currency, reason);

        // Format messages
        String smsMessage = formatBlockedTransactionSMS(amount, currency, reason);
        String emailSubject = "Transaction Blocked - Action Required";
        String emailBody = formatBlockedTransactionEmail(transactionId, amount, currency, reason);

        // Send via multiple channels for critical notification
        return Mono.when(
            smsService.sendSms(userId, getUserPhoneNumber(userId), smsMessage),
            emailService.sendEmail(userId, getUserEmail(userId), emailSubject, emailBody)
        );
    }

    // ============================================================================
    // Message Formatting Helpers
    // ============================================================================

    /**
     * Format transaction SMS message.
     */
    private String formatTransactionSMS(BigDecimal amount, String currency, String merchantCategory) {
        return String.format(
            "Transaction Approved: %s %s at %s. If this wasn't you, contact support immediately.",
            amount, currency, merchantCategory
        );
    }

    /**
     * Format suspicious activity email body.
     */
    private String formatSuspiciousActivityEmail(UUID transactionId, String reason) {
        return String.format("""
            We detected unusual activity on your account.

            Transaction ID: %s
            Reason: %s

            If this was you, no action is needed. If you don't recognize this activity,
            please contact our fraud team immediately at 1-800-FRAUD-HELP.

            Thank you for your attention to this matter.
            """, transactionId, reason);
    }

    /**
     * Format blocked transaction SMS message.
     */
    private String formatBlockedTransactionSMS(BigDecimal amount, String currency, String reason) {
        return String.format(
            "ALERT: Transaction of %s %s was blocked due to security concerns (%s). " +
            "If this was you, call 1-800-FRAUD-HELP immediately.",
            amount, currency, reason
        );
    }

    /**
     * Format blocked transaction email body.
     */
    private String formatBlockedTransactionEmail(
        UUID transactionId,
        BigDecimal amount,
        String currency,
        String reason
    ) {
        return String.format("""
            TRANSACTION BLOCKED

            We blocked a transaction on your account for security reasons.

            Transaction Details:
            - Transaction ID: %s
            - Amount: %s %s
            - Block Reason: %s

            If this was a legitimate transaction:
            1. Call our fraud team at 1-800-FRAUD-HELP
            2. Have your transaction ID ready
            3. We'll verify your identity and unblock if appropriate

            If you didn't attempt this transaction:
            - No action needed - your account is secure
            - We'll continue monitoring for suspicious activity

            Thank you for your patience and understanding.
            """, transactionId, amount, currency, reason);
    }

    // ============================================================================
    // User Data Helpers (Mock - In production, fetch from user repository)
    // ============================================================================

    /**
     * Get user phone number (mock - in production, fetch from user profile).
     */
    private String getUserPhoneNumber(String userId) {
        // In production: userRepository.findById(userId).getPhoneNumber()
        return "+1-555-" + String.format("%04d", Math.abs(userId.hashCode() % 10000));
    }

    /**
     * Get user email (mock - in production, fetch from user profile).
     */
    private String getUserEmail(String userId) {
        // In production: userRepository.findById(userId).getEmail()
        return userId + "@example.com";
    }

    /**
     * Check if user opted in to fraud alerts (mock - in production, check user preferences).
     */
    private boolean userOptedInToFraudAlerts(String userId) {
        // In production: userPreferencesRepository.findById(userId).isFraudAlertsEnabled()
        // For demo, assume all users opted in
        return true;
    }
}
