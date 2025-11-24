package com.nickfallico.financialriskmanagement.service.alert;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import reactor.test.StepVerifier;

/**
 * Tests for mock alert services.
 * Ensures mocks log correctly and track metrics.
 */
class MockAlertServicesTest {

    private MeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
    }

    @Test
    void mockSlackService_PostsMessageSuccessfully() {
        // Arrange
        MockSlackService slackService = new MockSlackService(meterRegistry);

        // Act & Assert
        StepVerifier.create(
            slackService.postToChannel("#test-channel", "Test Slack message")
        )
        .verifyComplete();

        // Verify metric was recorded
        assert meterRegistry.counter("alerts.slack.sent", "provider", "mock").count() == 1.0;
    }

    @Test
    void mockPagerDutyService_CreatesIncidentSuccessfully() {
        // Arrange
        MockPagerDutyService pagerDutyService = new MockPagerDutyService(meterRegistry);

        // Act & Assert
        StepVerifier.create(
            pagerDutyService.createIncident(
                "Test Incident",
                "Test description",
                "critical",
                "{\"details\":\"test\"}"
            )
        )
        .expectNextMatches(incidentId -> incidentId.startsWith("INC-"))
        .verifyComplete();

        // Verify metric was recorded
        assert meterRegistry.counter("alerts.pagerduty.incidents.created", "provider", "mock").count() == 1.0;
    }

    @Test
    void mockPagerDutyService_TriggersIncidentSuccessfully() {
        // Arrange
        MockPagerDutyService pagerDutyService = new MockPagerDutyService(meterRegistry);

        // Act & Assert
        StepVerifier.create(
            pagerDutyService.triggerIncident("Critical Alert", "Immediate action required")
        )
        .verifyComplete();

        // Verify metric was recorded
        assert meterRegistry.counter("alerts.pagerduty.incidents.triggered", "provider", "mock").count() == 1.0;
    }

    @Test
    void mockSiemService_LogsSecurityEventSuccessfully() {
        // Arrange
        MockSiemService siemService = new MockSiemService(meterRegistry);

        // Act & Assert
        StepVerifier.create(
            siemService.logSecurityEvent(
                "FRAUD_DETECTED",
                "critical",
                "user123",
                "{\"transactionId\":\"tx-123\"}"
            )
        )
        .verifyComplete();

        // Verify metric was recorded
        assert meterRegistry.counter("alerts.siem.events.logged", "provider", "mock").count() == 1.0;
    }

    @Test
    void mockServices_TrackMultipleInvocations() {
        // Arrange
        MockSlackService slackService = new MockSlackService(meterRegistry);

        // Act - Post multiple messages
        for (int i = 0; i < 3; i++) {
            slackService.postToChannel("#channel-" + i, "Message " + i).block();
        }

        // Assert - Metric should track all invocations
        assert meterRegistry.counter("alerts.slack.sent", "provider", "mock").count() == 3.0;
    }
}
