package com.nickfallico.financialriskmanagement.service.notification;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * Mock push notification service for development and demonstration.
 * Logs push notifications instead of actually sending them.
 *
 * In production, replace with FirebasePushNotificationService or OneSignalService.
 */
@Service
@Slf4j
@ConditionalOnProperty(name = "notification.push.provider", havingValue = "mock", matchIfMissing = true)
public class MockPushNotificationService implements PushNotificationService {

    private final Counter pushSentCounter;

    public MockPushNotificationService(MeterRegistry meterRegistry) {
        this.pushSentCounter = Counter.builder("notifications.push.sent")
            .description("Total number of push notifications sent (mock)")
            .tag("provider", "mock")
            .register(meterRegistry);
    }

    @Override
    public Mono<Void> sendPushNotification(String userId, String title, String message, String data) {
        return Mono.fromRunnable(() -> {
            pushSentCounter.increment();

            log.info("""

                ╔════════════════════════════════════════════════════════════════╗
                ║                 🔔 MOCK PUSH NOTIFICATION SENT                  ║
                ╠════════════════════════════════════════════════════════════════╣
                ║ User: {}
                ║ Title: {}
                ║ Message: {}
                ║ Data: {}
                ║
                ║ [MOCK MODE - No actual push notification sent]
                ║ Production: Would use Firebase/OneSignal
                ╚════════════════════════════════════════════════════════════════╝
                """,
                userId,
                title,
                message,
                data != null ? data : "None"
            );
        });
    }
}
