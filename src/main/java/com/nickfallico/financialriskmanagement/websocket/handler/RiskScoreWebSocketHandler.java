package com.nickfallico.financialriskmanagement.websocket.handler;

import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nickfallico.financialriskmanagement.websocket.message.RiskScoreUpdateMessage;
import com.nickfallico.financialriskmanagement.websocket.service.DashboardEventPublisher;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * WebSocket handler for real-time risk score update streaming.
 * Endpoint: /ws/risk-scores
 *
 * Streams:
 * - HighRiskUserIdentified events
 * - UserProfileUpdated events
 */
@Component
@Slf4j
public class RiskScoreWebSocketHandler implements WebSocketHandler {

    private final DashboardEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;
    private final AtomicInteger activeConnections;
    private final Counter messagesCounter;

    public RiskScoreWebSocketHandler(
            DashboardEventPublisher eventPublisher,
            ObjectMapper objectMapper,
            MeterRegistry meterRegistry) {
        this.eventPublisher = eventPublisher;
        this.objectMapper = objectMapper;
        this.activeConnections = new AtomicInteger(0);

        // Register metrics
        Gauge.builder("websocket.connections.active", activeConnections, AtomicInteger::get)
            .tag("endpoint", "risk-scores")
            .tag("component", "websocket")
            .description("Number of active WebSocket connections for risk score updates")
            .register(meterRegistry);

        this.messagesCounter = Counter.builder("websocket.messages.sent")
            .tag("endpoint", "risk-scores")
            .tag("component", "websocket")
            .description("Number of WebSocket messages sent to risk score clients")
            .register(meterRegistry);

        log.info("RiskScoreWebSocketHandler initialized");
    }

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        String sessionId = session.getId();
        log.info("New WebSocket connection for risk score updates: sessionId={}", sessionId);
        activeConnections.incrementAndGet();

        return session.send(
            eventPublisher.getRiskScoreUpdateStream()
                .map(message -> createTextMessage(session, message))
                .doOnNext(msg -> messagesCounter.increment())
        )
        .doFinally(signalType -> {
            activeConnections.decrementAndGet();
            log.info("WebSocket connection closed for risk score updates: sessionId={}, signal={}",
                sessionId, signalType);
        });
    }

    private WebSocketMessage createTextMessage(WebSocketSession session, RiskScoreUpdateMessage message) {
        try {
            String json = objectMapper.writeValueAsString(message);
            return session.textMessage(json);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize RiskScoreUpdateMessage to JSON", e);
            return session.textMessage("{\"error\": \"Serialization failed\"}");
        }
    }

    /**
     * Get the current number of active connections.
     */
    public int getActiveConnectionCount() {
        return activeConnections.get();
    }
}
