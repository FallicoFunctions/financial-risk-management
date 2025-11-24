package com.nickfallico.financialriskmanagement.websocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.nickfallico.financialriskmanagement.service.MetricsService;
import com.nickfallico.financialriskmanagement.websocket.handler.FraudAlertWebSocketHandler;
import com.nickfallico.financialriskmanagement.websocket.handler.MetricsWebSocketHandler;
import com.nickfallico.financialriskmanagement.websocket.handler.RiskScoreWebSocketHandler;
import com.nickfallico.financialriskmanagement.websocket.handler.TransactionEventWebSocketHandler;
import com.nickfallico.financialriskmanagement.websocket.message.DashboardMessage;
import com.nickfallico.financialriskmanagement.websocket.message.FraudAlertMessage;
import com.nickfallico.financialriskmanagement.websocket.message.RiskScoreUpdateMessage;
import com.nickfallico.financialriskmanagement.websocket.message.TransactionEventMessage;
import com.nickfallico.financialriskmanagement.websocket.service.DashboardEventPublisher;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import reactor.test.StepVerifier;

/**
 * Unit tests for WebSocket handlers.
 * Tests handler initialization, connection tracking, and message serialization.
 */
@DisplayName("WebSocket Handler Tests")
class WebSocketHandlerTest {

    private DashboardEventPublisher eventPublisher;
    private ObjectMapper objectMapper;
    private SimpleMeterRegistry meterRegistry;
    private MetricsService metricsService;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        eventPublisher = new DashboardEventPublisher(meterRegistry);
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        metricsService = mock(MetricsService.class);
        when(metricsService.getFraudDetectionRate()).thenReturn(1.5);
    }

    // ========== FraudAlertWebSocketHandler Tests ==========

    @Test
    @DisplayName("FraudAlertWebSocketHandler should track active connections")
    void fraudAlertHandlerShouldTrackConnections() {
        // Given
        FraudAlertWebSocketHandler handler = new FraudAlertWebSocketHandler(
            eventPublisher, objectMapper, meterRegistry);

        // Then
        assertThat(handler.getActiveConnectionCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("FraudAlertWebSocketHandler should serialize messages to JSON")
    void fraudAlertHandlerShouldSerializeMessages() throws Exception {
        // Given
        FraudAlertMessage message = FraudAlertMessage.builder()
            .messageType(DashboardMessage.MessageType.FRAUD_DETECTED)
            .transactionId(UUID.randomUUID())
            .userId("user-123")
            .amount(new BigDecimal("1000.00"))
            .currency("USD")
            .fraudProbability(0.85)
            .riskLevel("HIGH")
            .action("BLOCK")
            .violatedRules(List.of("RULE_001"))
            .eventTimestamp(Instant.now())
            .publishedAt(Instant.now())
            .build();

        // When
        String json = objectMapper.writeValueAsString(message);

        // Then
        assertThat(json).contains("\"messageType\":\"FRAUD_DETECTED\"");
        assertThat(json).contains("\"userId\":\"user-123\"");
        assertThat(json).contains("\"fraudProbability\":0.85");
        assertThat(json).contains("\"riskLevel\":\"HIGH\"");
    }

    // ========== TransactionEventWebSocketHandler Tests ==========

    @Test
    @DisplayName("TransactionEventWebSocketHandler should track active connections")
    void transactionEventHandlerShouldTrackConnections() {
        // Given
        TransactionEventWebSocketHandler handler = new TransactionEventWebSocketHandler(
            eventPublisher, objectMapper, meterRegistry);

        // Then
        assertThat(handler.getActiveConnectionCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("TransactionEventWebSocketHandler should serialize messages to JSON")
    void transactionEventHandlerShouldSerializeMessages() throws Exception {
        // Given
        TransactionEventMessage message = TransactionEventMessage.builder()
            .messageType(DashboardMessage.MessageType.TRANSACTION_CREATED)
            .transactionId(UUID.randomUUID())
            .userId("user-456")
            .amount(new BigDecimal("75.50"))
            .currency("EUR")
            .transactionType("PURCHASE")
            .merchantCategory("RESTAURANT")
            .merchantName("Cafe Paris")
            .isInternational(true)
            .country("FR")
            .city("Paris")
            .eventTimestamp(Instant.now())
            .publishedAt(Instant.now())
            .build();

        // When
        String json = objectMapper.writeValueAsString(message);

        // Then
        assertThat(json).contains("\"messageType\":\"TRANSACTION_CREATED\"");
        assertThat(json).contains("\"userId\":\"user-456\"");
        assertThat(json).contains("\"merchantName\":\"Cafe Paris\"");
        assertThat(json).contains("\"country\":\"FR\"");
    }

    // ========== RiskScoreWebSocketHandler Tests ==========

    @Test
    @DisplayName("RiskScoreWebSocketHandler should track active connections")
    void riskScoreHandlerShouldTrackConnections() {
        // Given
        RiskScoreWebSocketHandler handler = new RiskScoreWebSocketHandler(
            eventPublisher, objectMapper, meterRegistry);

        // Then
        assertThat(handler.getActiveConnectionCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("RiskScoreWebSocketHandler should serialize messages to JSON")
    void riskScoreHandlerShouldSerializeMessages() throws Exception {
        // Given
        RiskScoreUpdateMessage message = RiskScoreUpdateMessage.builder()
            .messageType(DashboardMessage.MessageType.HIGH_RISK_USER_IDENTIFIED)
            .userId("user-risky")
            .overallRiskScore(0.92)
            .riskThreshold(0.75)
            .riskFactors(List.of("Multiple blocked transactions", "Unusual pattern"))
            .alertSeverity("CRITICAL")
            .recommendedAction("SUSPEND")
            .eventTimestamp(Instant.now())
            .publishedAt(Instant.now())
            .build();

        // When
        String json = objectMapper.writeValueAsString(message);

        // Then
        assertThat(json).contains("\"messageType\":\"HIGH_RISK_USER_IDENTIFIED\"");
        assertThat(json).contains("\"userId\":\"user-risky\"");
        assertThat(json).contains("\"overallRiskScore\":0.92");
        assertThat(json).contains("\"alertSeverity\":\"CRITICAL\"");
    }

    // ========== MetricsWebSocketHandler Tests ==========

    @Test
    @DisplayName("MetricsWebSocketHandler should track active connections")
    void metricsHandlerShouldTrackConnections() {
        // Given
        MetricsWebSocketHandler handler = new MetricsWebSocketHandler(
            eventPublisher, meterRegistry, metricsService, objectMapper);

        // Then
        assertThat(handler.getActiveConnectionCount()).isEqualTo(0);
    }

    // ========== Message Type Tests ==========

    @Test
    @DisplayName("All message types should be serializable")
    void allMessageTypesShouldBeSerializable() throws Exception {
        // Test that all MessageType enum values can be serialized
        for (DashboardMessage.MessageType type : DashboardMessage.MessageType.values()) {
            FraudAlertMessage message = FraudAlertMessage.builder()
                .messageType(type)
                .transactionId(UUID.randomUUID())
                .userId("test-user")
                .eventTimestamp(Instant.now())
                .publishedAt(Instant.now())
                .build();

            String json = objectMapper.writeValueAsString(message);
            assertThat(json).contains("\"messageType\":\"" + type.name() + "\"");
        }
    }

    // ========== Metrics Registration Tests ==========

    @Test
    @DisplayName("Handlers should register metrics on initialization")
    void handlersShouldRegisterMetrics() {
        // Given
        new FraudAlertWebSocketHandler(eventPublisher, objectMapper, meterRegistry);
        new TransactionEventWebSocketHandler(eventPublisher, objectMapper, meterRegistry);
        new RiskScoreWebSocketHandler(eventPublisher, objectMapper, meterRegistry);
        new MetricsWebSocketHandler(eventPublisher, meterRegistry, metricsService, objectMapper);

        // Then - verify gauges are registered
        assertThat(meterRegistry.find("websocket.connections.active")
            .tag("endpoint", "fraud-alerts").gauge()).isNotNull();
        assertThat(meterRegistry.find("websocket.connections.active")
            .tag("endpoint", "transactions").gauge()).isNotNull();
        assertThat(meterRegistry.find("websocket.connections.active")
            .tag("endpoint", "risk-scores").gauge()).isNotNull();
        assertThat(meterRegistry.find("websocket.connections.active")
            .tag("endpoint", "metrics").gauge()).isNotNull();

        // Verify counters are registered
        assertThat(meterRegistry.find("websocket.messages.sent")
            .tag("endpoint", "fraud-alerts").counter()).isNotNull();
        assertThat(meterRegistry.find("websocket.messages.sent")
            .tag("endpoint", "transactions").counter()).isNotNull();
        assertThat(meterRegistry.find("websocket.messages.sent")
            .tag("endpoint", "risk-scores").counter()).isNotNull();
        assertThat(meterRegistry.find("websocket.messages.sent")
            .tag("endpoint", "metrics").counter()).isNotNull();
    }

    // ========== Stream Integration Tests ==========

    @Test
    @DisplayName("Event publisher streams should be accessible from handlers")
    void eventPublisherStreamsShouldBeAccessible() {
        // Given
        FraudAlertWebSocketHandler fraudHandler = new FraudAlertWebSocketHandler(
            eventPublisher, objectMapper, meterRegistry);

        // Then - verify streams are not null and can be subscribed to
        assertThat(eventPublisher.getFraudAlertStream()).isNotNull();
        assertThat(eventPublisher.getTransactionEventStream()).isNotNull();
        assertThat(eventPublisher.getRiskScoreUpdateStream()).isNotNull();
        assertThat(eventPublisher.getMetricsStream()).isNotNull();

        // Verify streams complete normally when empty
        StepVerifier.create(eventPublisher.getFraudAlertStream().take(0))
            .verifyComplete();
    }
}
