package com.nickfallico.financialriskmanagement.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.nickfallico.financialriskmanagement.service.notification.EmailService;
import com.nickfallico.financialriskmanagement.service.notification.PushNotificationService;
import com.nickfallico.financialriskmanagement.service.notification.SmsService;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private SmsService smsService;

    @Mock
    private EmailService emailService;

    @Mock
    private PushNotificationService pushNotificationService;

    private MeterRegistry meterRegistry;
    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        notificationService = new NotificationService(
            meterRegistry,
            smsService,
            emailService,
            pushNotificationService
        );

        // Default: all services return successful Mono
        when(smsService.sendSms(anyString(), anyString(), anyString())).thenReturn(Mono.empty());
        when(emailService.sendEmail(anyString(), anyString(), anyString(), anyString())).thenReturn(Mono.empty());
        when(pushNotificationService.sendPushNotification(anyString(), anyString(), anyString(), anyString()))
            .thenReturn(Mono.empty());
    }

    @Test
    void sendTransactionConfirmation_SendsSmsAndPush() {
        // Arrange
        String userId = "user123";
        UUID transactionId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("150.00");
        String currency = "USD";
        String merchantCategory = "RETAIL";

        // Act & Assert
        StepVerifier.create(
            notificationService.sendTransactionConfirmation(
                userId, transactionId, amount, currency, merchantCategory
            )
        )
        .verifyComplete();

        // Verify SMS was sent
        verify(smsService).sendSms(
            eq(userId),
            anyString(), // phone number (mocked)
            anyString()  // message containing amount and merchant
        );

        // Verify push notification was sent
        verify(pushNotificationService).sendPushNotification(
            eq(userId),
            eq("Transaction Approved"),
            anyString(), // message
            anyString()  // JSON data
        );

        // Verify metrics were recorded
        assert meterRegistry.counter("notifications.sent").count() == 1.0;
        assert meterRegistry.counter("notifications.transaction_confirmation").count() == 1.0;
    }

    @Test
    void sendSuspiciousActivityAlert_SendsEmail() {
        // Arrange
        String userId = "user456";
        UUID transactionId = UUID.randomUUID();
        String reason = "Unusual spending pattern detected";

        // Act & Assert
        StepVerifier.create(
            notificationService.sendSuspiciousActivityAlert(userId, transactionId, reason)
        )
        .verifyComplete();

        // Verify email was sent
        verify(emailService).sendEmail(
            eq(userId),
            anyString(), // email address (mocked)
            eq("Suspicious Activity Detected on Your Account"),
            anyString()  // email body
        );

        // Verify metrics were recorded
        assert meterRegistry.counter("notifications.sent").count() == 1.0;
        assert meterRegistry.counter("notifications.suspicious_activity").count() == 1.0;
    }

    @Test
    void sendTransactionBlockedNotification_SendsSmsAndEmail() {
        // Arrange
        String userId = "user789";
        UUID transactionId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("5000.00");
        String currency = "USD";
        String reason = "HIGH_FRAUD_PROBABILITY";

        // Act & Assert
        StepVerifier.create(
            notificationService.sendTransactionBlockedNotification(
                userId, transactionId, amount, currency, reason
            )
        )
        .verifyComplete();

        // Verify SMS was sent
        verify(smsService).sendSms(
            eq(userId),
            anyString(), // phone number
            anyString()  // SMS message
        );

        // Verify email was sent
        verify(emailService).sendEmail(
            eq(userId),
            anyString(), // email address
            eq("Transaction Blocked - Action Required"),
            anyString()  // email body
        );

        // Verify metrics were recorded
        assert meterRegistry.counter("notifications.sent").count() == 1.0;
        assert meterRegistry.counter("notifications.transaction_blocked").count() == 1.0;
    }

    @Test
    void sendTransactionConfirmation_HandlesServiceFailures() {
        // Arrange
        String userId = "user123";
        UUID transactionId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("100.00");
        String currency = "USD";
        String merchantCategory = "DINING";

        // Simulate SMS service failure
        when(smsService.sendSms(anyString(), anyString(), anyString()))
            .thenReturn(Mono.error(new RuntimeException("SMS service unavailable")));

        // Act & Assert - should propagate the error
        StepVerifier.create(
            notificationService.sendTransactionConfirmation(
                userId, transactionId, amount, currency, merchantCategory
            )
        )
        .expectError(RuntimeException.class)
        .verify();
    }

    @Test
    void suspiciousActivityAlert_VerifiesMessageContent() {
        // Arrange
        String userId = "user999";
        UUID transactionId = UUID.randomUUID();
        String reason = "Multiple failed authentication attempts";

        // Act
        StepVerifier.create(
            notificationService.sendSuspiciousActivityAlert(userId, transactionId, reason)
        )
        .verifyComplete();

        // Verify email contains transaction ID and reason
        verify(emailService).sendEmail(
            eq(userId),
            anyString(),
            anyString(),
            org.mockito.ArgumentMatchers.contains(transactionId.toString())
        );

        verify(emailService).sendEmail(
            eq(userId),
            anyString(),
            anyString(),
            org.mockito.ArgumentMatchers.contains(reason)
        );
    }
}
