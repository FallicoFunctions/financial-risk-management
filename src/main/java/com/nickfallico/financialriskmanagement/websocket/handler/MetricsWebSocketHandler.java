package com.nickfallico.financialriskmanagement.websocket.handler;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nickfallico.financialriskmanagement.service.MetricsService;
import com.nickfallico.financialriskmanagement.websocket.message.DashboardMessage;
import com.nickfallico.financialriskmanagement.websocket.message.MetricsSnapshotMessage;
import com.nickfallico.financialriskmanagement.websocket.service.DashboardEventPublisher;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.search.Search;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * WebSocket handler for real-time metrics streaming.
 * Endpoint: /ws/metrics
 *
 * Streams periodic metrics snapshots every 5 seconds including:
 * - Transaction metrics
 * - Fraud detection metrics
 * - Risk assessment metrics
 * - Performance metrics
 * - WebSocket connection metrics
 */
@Component
@Slf4j
public class MetricsWebSocketHandler implements WebSocketHandler {

    private static final Duration METRICS_INTERVAL = Duration.ofSeconds(5);

    private final DashboardEventPublisher eventPublisher;
    private final MeterRegistry meterRegistry;
    private final MetricsService metricsService;
    private final ObjectMapper objectMapper;
    private final AtomicInteger activeConnections;
    private final Counter messagesCounter;

    public MetricsWebSocketHandler(
            DashboardEventPublisher eventPublisher,
            MeterRegistry meterRegistry,
            MetricsService metricsService,
            ObjectMapper objectMapper) {
        this.eventPublisher = eventPublisher;
        this.meterRegistry = meterRegistry;
        this.metricsService = metricsService;
        this.objectMapper = objectMapper;
        this.activeConnections = new AtomicInteger(0);

        // Register metrics
        Gauge.builder("websocket.connections.active", activeConnections, AtomicInteger::get)
            .tag("endpoint", "metrics")
            .tag("component", "websocket")
            .description("Number of active WebSocket connections for metrics stream")
            .register(meterRegistry);

        this.messagesCounter = Counter.builder("websocket.messages.sent")
            .tag("endpoint", "metrics")
            .tag("component", "websocket")
            .description("Number of WebSocket messages sent to metrics clients")
            .register(meterRegistry);

        log.info("MetricsWebSocketHandler initialized with {}s interval", METRICS_INTERVAL.getSeconds());
    }

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        String sessionId = session.getId();
        log.info("New WebSocket connection for metrics stream: sessionId={}", sessionId);
        activeConnections.incrementAndGet();

        // Merge periodic metrics with event-driven metrics from the publisher
        Flux<MetricsSnapshotMessage> metricsStream = Flux.merge(
            // Periodic metrics every 5 seconds
            Flux.interval(Duration.ZERO, METRICS_INTERVAL)
                .map(tick -> buildMetricsSnapshot()),
            // Event-driven metrics from the publisher
            eventPublisher.getMetricsStream()
        );

        return session.send(
            metricsStream
                .map(message -> createTextMessage(session, message))
                .doOnNext(msg -> messagesCounter.increment())
        )
        .doFinally(signalType -> {
            activeConnections.decrementAndGet();
            log.info("WebSocket connection closed for metrics stream: sessionId={}, signal={}",
                sessionId, signalType);
        });
    }

    /**
     * Build a metrics snapshot from current Micrometer metrics.
     */
    private MetricsSnapshotMessage buildMetricsSnapshot() {
        return MetricsSnapshotMessage.builder()
            .messageType(DashboardMessage.MessageType.METRICS_SNAPSHOT)
            .eventTimestamp(Instant.now())
            .publishedAt(Instant.now())
            // Transaction metrics
            .totalTransactionsProcessed(getCounterValue("transactions.processed.total"))
            .transactionsLastHour(getGaugeValue("transactions.processed.hour"))
            .transactionsLastMinute(0L) // Could add minute tracking if needed
            // Fraud metrics
            .totalFraudDetected(getCounterValue("fraud.detected.total"))
            .fraudDetectedLastHour(0L) // Could add hourly fraud tracking
            .transactionsBlocked(getCounterValue("transactions.blocked.total"))
            .transactionsBlockedLastHour(getGaugeValue("transactions.blocked.hour"))
            .fraudDetectionRate(metricsService.getFraudDetectionRate())
            // Risk metrics
            .averageRiskScore(getGaugeValue("risk.score.average") / 100.0) // Convert back to 0-1 range
            .highRiskTransactions(getCounterValue("transactions.high.risk"))
            // Performance metrics
            .avgFraudDetectionTimeMs(getTimerMean("fraud.detection.duration"))
            .avgTransactionProcessingTimeMs(getTimerMean("transaction.processing.duration"))
            // WebSocket metrics
            .activeWebSocketConnections(getTotalActiveWebSocketConnections())
            .totalMessagesPublished(getTotalWebSocketMessages())
            .build();
    }

    private Long getCounterValue(String name) {
        try {
            io.micrometer.core.instrument.Counter counter = meterRegistry.find(name).counter();
            return counter != null ? (long) counter.count() : 0L;
        } catch (Exception e) {
            log.trace("Could not find counter: {}", name);
            return 0L;
        }
    }

    private Long getGaugeValue(String name) {
        try {
            io.micrometer.core.instrument.Gauge gauge = meterRegistry.find(name).gauge();
            return gauge != null ? (long) gauge.value() : 0L;
        } catch (Exception e) {
            log.trace("Could not find gauge: {}", name);
            return 0L;
        }
    }

    private Double getTimerMean(String name) {
        try {
            io.micrometer.core.instrument.Timer timer = meterRegistry.find(name).timer();
            return timer != null ? timer.mean(java.util.concurrent.TimeUnit.MILLISECONDS) : 0.0;
        } catch (Exception e) {
            log.trace("Could not find timer: {}", name);
            return 0.0;
        }
    }

    private Integer getTotalActiveWebSocketConnections() {
        try {
            Search search = meterRegistry.find("websocket.connections.active");
            return (int) search.gauges().stream()
                .mapToDouble(io.micrometer.core.instrument.Gauge::value)
                .sum();
        } catch (Exception e) {
            log.trace("Could not sum WebSocket connections");
            return 0;
        }
    }

    private Long getTotalWebSocketMessages() {
        try {
            Search search = meterRegistry.find("websocket.messages.sent");
            return (long) search.counters().stream()
                .mapToDouble(io.micrometer.core.instrument.Counter::count)
                .sum();
        } catch (Exception e) {
            log.trace("Could not sum WebSocket messages");
            return 0L;
        }
    }

    private WebSocketMessage createTextMessage(WebSocketSession session, MetricsSnapshotMessage message) {
        try {
            String json = objectMapper.writeValueAsString(message);
            return session.textMessage(json);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize MetricsSnapshotMessage to JSON", e);
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
