package com.nickfallico.financialriskmanagement.service.notification;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * Mock SMS service for development and demonstration.
 * Logs SMS messages instead of actually sending them.
 *
 * In production, replace with TwilioSmsService or AwsSnsSmsService.
 */
@Service
@Slf4j
@ConditionalOnProperty(name = "notification.sms.provider", havingValue = "mock", matchIfMissing = true)
public class MockSmsService implements SmsService {

    private final Counter smsSentCounter;

    public MockSmsService(MeterRegistry meterRegistry) {
        this.smsSentCounter = Counter.builder("notifications.sms.sent")
            .description("Total number of SMS messages sent (mock)")
            .tag("provider", "mock")
            .register(meterRegistry);
    }

    @Override
    public Mono<Void> sendSms(String userId, String phoneNumber, String message) {
        return Mono.fromRunnable(() -> {
            smsSentCounter.increment();

            log.info("""

                ╔════════════════════════════════════════════════════════════════╗
                ║                    📱 MOCK SMS SENT                             ║
                ╠════════════════════════════════════════════════════════════════╣
                ║ To: {}
                ║ User: {}
                ║ Message: {}
                ║
                ║ [MOCK MODE - No actual SMS sent]
                ║ Production: Would use Twilio/AWS SNS
                ╚════════════════════════════════════════════════════════════════╝
                """,
                phoneNumber != null ? phoneNumber : "Unknown",
                userId,
                message
            );
        });
    }
}
