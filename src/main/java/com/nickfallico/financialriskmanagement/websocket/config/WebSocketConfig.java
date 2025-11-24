package com.nickfallico.financialriskmanagement.websocket.config;

import java.util.HashMap;
import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;

import com.nickfallico.financialriskmanagement.websocket.handler.FraudAlertWebSocketHandler;
import com.nickfallico.financialriskmanagement.websocket.handler.MetricsWebSocketHandler;
import com.nickfallico.financialriskmanagement.websocket.handler.RiskScoreWebSocketHandler;
import com.nickfallico.financialriskmanagement.websocket.handler.TransactionEventWebSocketHandler;

import lombok.RequiredArgsConstructor;

/**
 * WebSocket configuration for real-time dashboard streaming.
 * Configures reactive WebSocket handlers for different event streams.
 *
 * Endpoints:
 * - /ws/fraud-alerts     - Real-time fraud detection alerts
 * - /ws/transactions     - Live transaction event feed
 * - /ws/risk-scores      - Risk score update stream
 * - /ws/metrics          - Real-time platform metrics
 */
@Configuration
@RequiredArgsConstructor
public class WebSocketConfig {

    private final FraudAlertWebSocketHandler fraudAlertWebSocketHandler;
    private final TransactionEventWebSocketHandler transactionEventWebSocketHandler;
    private final RiskScoreWebSocketHandler riskScoreWebSocketHandler;
    private final MetricsWebSocketHandler metricsWebSocketHandler;

    /**
     * Configure WebSocket handler mapping with URL patterns.
     * Order -1 ensures WebSocket routes are checked before other handlers.
     */
    @Bean
    public HandlerMapping webSocketHandlerMapping() {
        Map<String, WebSocketHandler> map = new HashMap<>();
        map.put("/ws/fraud-alerts", fraudAlertWebSocketHandler);
        map.put("/ws/transactions", transactionEventWebSocketHandler);
        map.put("/ws/risk-scores", riskScoreWebSocketHandler);
        map.put("/ws/metrics", metricsWebSocketHandler);

        SimpleUrlHandlerMapping mapping = new SimpleUrlHandlerMapping();
        mapping.setUrlMap(map);
        mapping.setOrder(-1); // High priority
        return mapping;
    }

    /**
     * WebSocket handler adapter for reactive WebSocket support.
     */
    @Bean
    public WebSocketHandlerAdapter webSocketHandlerAdapter() {
        return new WebSocketHandlerAdapter();
    }
}
