package com.nickfallico.financialriskmanagement.websocket.handler;

import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nickfallico.financialriskmanagement.websocket.message.FraudAlertMessage;
import com.nickfallico.financialriskmanagement.websocket.service.DashboardEventPublisher;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * WebSocket handler for real-time fraud alert streaming.
 * Endpoint: /ws/fraud-alerts
 *
 * Streams:
 * - FraudDetected events
 * - FraudCleared events
 * - TransactionBlocked events
 */
@Component
@Slf4j
public class FraudAlertWebSocketHandler implements WebSocketHandler {

    private final DashboardEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;
    private final AtomicInteger activeConnections;
    private final Counter messagesCounter;

    public FraudAlertWebSocketHandler(
            DashboardEventPublisher eventPublisher,
            ObjectMapper objectMapper,
            MeterRegistry meterRegistry) {
        this.eventPublisher = eventPublisher;
        this.objectMapper = objectMapper;
        this.activeConnections = new AtomicInteger(0);

        // Register metrics
        Gauge.builder("websocket.connections.active", activeConnections, AtomicInteger::get)
            .tag("endpoint", "fraud-alerts")
            .tag("component", "websocket")
            .description("Number of active WebSocket connections for fraud alerts")
            .register(meterRegistry);

        this.messagesCounter = Counter.builder("websocket.messages.sent")
            .tag("endpoint", "fraud-alerts")
            .tag("component", "websocket")
            .description("Number of WebSocket messages sent to fraud alert clients")
            .register(meterRegistry);

        log.info("FraudAlertWebSocketHandler initialized");
    }

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        String sessionId = session.getId();
        log.info("New WebSocket connection for fraud alerts: sessionId={}", sessionId);
        activeConnections.incrementAndGet();

        return session.send(
            eventPublisher.getFraudAlertStream()
                .map(message -> createTextMessage(session, message))
                .doOnNext(msg -> messagesCounter.increment())
        )
        .doFinally(signalType -> {
            activeConnections.decrementAndGet();
            log.info("WebSocket connection closed for fraud alerts: sessionId={}, signal={}",
                sessionId, signalType);
        });
    }

    private WebSocketMessage createTextMessage(WebSocketSession session, FraudAlertMessage message) {
        try {
            String json = objectMapper.writeValueAsString(message);
            return session.textMessage(json);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize FraudAlertMessage to JSON", e);
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
