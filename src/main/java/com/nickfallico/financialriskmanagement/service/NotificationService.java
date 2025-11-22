package com.nickfallico.financialriskmanagement.service;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.stereotype.Service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * Service for sending notifications to users about their transactions.
 * In production, this would integrate with:
 * - SMS gateways (Twilio, AWS SNS)
 * - Email services (SendGrid, AWS SES)
 * - Push notification services (Firebase, OneSignal)
 * - In-app notification systems
 */
@Service
@Slf4j
public class NotificationService {
    
    private final MeterRegistry meterRegistry;
    private final Counter notificationsSentCounter;
    
    public NotificationService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.notificationsSentCounter = Counter.builder("notifications.sent")
            .description("Total number of notifications sent to users")
            .register(meterRegistry);
    }
    
    /**
     * Send transaction confirmation notification.
     */
    public Mono<Void> sendTransactionConfirmation(
        String userId,
        UUID transactionId,
        BigDecimal amount,
        String currency,
        String merchantCategory
    ) {
        return Mono.fromRunnable(() -> {
            notificationsSentCounter.increment();
            meterRegistry.counter("notifications.transaction_confirmation").increment();
            
            log.info("""
                📱 Notification Sent: Transaction Confirmation
                User: {}
                Transaction: {}
                Amount: {} {}
                Merchant: {}
                """,
                userId,
                transactionId,
                amount,
                currency,
                merchantCategory
            );
            
            // TODO: Production implementation
            // smsService.send(userId, formatTransactionSMS(amount, currency, merchantCategory));
            // pushNotificationService.send(userId, formatPushNotification(transactionId));
        });
    }
    
    /**
     * Send suspicious activity alert to user.
     * NOTE: Be careful with this - alerting users to fraud detection
     * can tip off fraudsters. Use sparingly.
     */
    public Mono<Void> sendSuspiciousActivityAlert(
        String userId,
        UUID transactionId,
        String reason
    ) {
        return Mono.fromRunnable(() -> {
            notificationsSentCounter.increment();
            meterRegistry.counter("notifications.suspicious_activity").increment();
            
            log.warn("""
                🔐 Notification Sent: Suspicious Activity Alert
                User: {}
                Transaction: {}
                Reason: {}
                Action: User should verify this transaction
                """,
                userId,
                transactionId,
                reason
            );
            
            // TODO: Production implementation
            // emailService.sendSuspiciousActivityEmail(userId, transactionId, reason);
            // Only send if user explicitly opted in to fraud alerts
        });
    }
    
    /**
     * Send transaction blocked notification.
     */
    public Mono<Void> sendTransactionBlockedNotification(
        String userId,
        UUID transactionId,
        BigDecimal amount,
        String currency,
        String reason
    ) {
        return Mono.fromRunnable(() -> {
            notificationsSentCounter.increment();
            meterRegistry.counter("notifications.transaction_blocked").increment();
            
            log.info("""
                🛑 Notification Sent: Transaction Blocked
                User: {}
                Transaction: {}
                Amount: {} {}
                Reason: {}
                Action: Contact support if this was a legitimate transaction
                """,
                userId,
                transactionId,
                amount,
                currency,
                reason
            );
            
            // TODO: Production implementation
            // smsService.send(userId, formatBlockedTransactionSMS(amount, currency, reason));
            // emailService.sendBlockedTransactionEmail(userId, transactionId, amount, reason);
        });
    }
}