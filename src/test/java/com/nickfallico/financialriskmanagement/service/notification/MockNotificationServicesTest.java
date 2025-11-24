package com.nickfallico.financialriskmanagement.service.notification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import reactor.test.StepVerifier;

/**
 * Tests for mock notification services.
 * Ensures mocks log correctly and track metrics.
 */
class MockNotificationServicesTest {

    private MeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
    }

    @Test
    void mockSmsService_SendsSmsSuccessfully() {
        // Arrange
        MockSmsService smsService = new MockSmsService(meterRegistry);

        // Act & Assert
        StepVerifier.create(
            smsService.sendSms("user123", "+1-555-1234", "Test SMS message")
        )
        .verifyComplete();

        // Verify metric was recorded
        assert meterRegistry.counter("notifications.sms.sent", "provider", "mock").count() == 1.0;
    }

    @Test
    void mockEmailService_SendsEmailSuccessfully() {
        // Arrange
        MockEmailService emailService = new MockEmailService(meterRegistry);

        // Act & Assert
        StepVerifier.create(
            emailService.sendEmail(
                "user456",
                "user@example.com",
                "Test Subject",
                "Test email body content"
            )
        )
        .verifyComplete();

        // Verify metric was recorded
        assert meterRegistry.counter("notifications.email.sent", "provider", "mock").count() == 1.0;
    }

    @Test
    void mockPushNotificationService_SendsNotificationSuccessfully() {
        // Arrange
        MockPushNotificationService pushService = new MockPushNotificationService(meterRegistry);

        // Act & Assert
        StepVerifier.create(
            pushService.sendPushNotification(
                "user789",
                "Test Title",
                "Test notification message",
                "{\"key\":\"value\"}"
            )
        )
        .verifyComplete();

        // Verify metric was recorded
        assert meterRegistry.counter("notifications.push.sent", "provider", "mock").count() == 1.0;
    }

    @Test
    void mockServices_TrackMultipleInvocations() {
        // Arrange
        MockSmsService smsService = new MockSmsService(meterRegistry);

        // Act - Send multiple SMS messages
        for (int i = 0; i < 5; i++) {
            smsService.sendSms("user" + i, "+1-555-000" + i, "Message " + i).block();
        }

        // Assert - Metric should track all invocations
        assert meterRegistry.counter("notifications.sms.sent", "provider", "mock").count() == 5.0;
    }
}
