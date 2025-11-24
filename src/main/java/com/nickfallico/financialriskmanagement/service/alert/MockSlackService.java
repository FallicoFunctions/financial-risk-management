package com.nickfallico.financialriskmanagement.service.alert;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * Mock Slack service for development and demonstration.
 * Logs Slack messages instead of actually posting them.
 *
 * In production, replace with real Slack Webhook integration.
 */
@Service
@Slf4j
@ConditionalOnProperty(name = "alert.slack.provider", havingValue = "mock", matchIfMissing = true)
public class MockSlackService implements SlackService {

    private final Counter slackMessageCounter;

    public MockSlackService(MeterRegistry meterRegistry) {
        this.slackMessageCounter = Counter.builder("alerts.slack.sent")
            .description("Total number of Slack messages sent (mock)")
            .tag("provider", "mock")
            .register(meterRegistry);
    }

    @Override
    public Mono<Void> postToChannel(String channel, String message) {
        return Mono.fromRunnable(() -> {
            slackMessageCounter.increment();

            log.info("""

                ╔════════════════════════════════════════════════════════════════╗
                ║                   💬 MOCK SLACK MESSAGE SENT                    ║
                ╠════════════════════════════════════════════════════════════════╣
                ║ Channel: {}
                ║ Message:
                ║ {}
                ║
                ║ [MOCK MODE - No actual Slack webhook called]
                ║ Production: Would POST to Slack Webhook URL
                ╚════════════════════════════════════════════════════════════════╝
                """,
                channel,
                message.length() > 500 ? message.substring(0, 500) + "..." : message
            );
        });
    }
}
