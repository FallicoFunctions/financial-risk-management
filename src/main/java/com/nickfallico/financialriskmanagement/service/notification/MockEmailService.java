package com.nickfallico.financialriskmanagement.service.notification;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * Mock email service for development and demonstration.
 * Logs emails instead of actually sending them.
 *
 * In production, replace with SendGridEmailService or AwsSesEmailService.
 */
@Service
@Slf4j
@ConditionalOnProperty(name = "notification.email.provider", havingValue = "mock", matchIfMissing = true)
public class MockEmailService implements EmailService {

    private final Counter emailSentCounter;

    public MockEmailService(MeterRegistry meterRegistry) {
        this.emailSentCounter = Counter.builder("notifications.email.sent")
            .description("Total number of emails sent (mock)")
            .tag("provider", "mock")
            .register(meterRegistry);
    }

    @Override
    public Mono<Void> sendEmail(String userId, String emailAddress, String subject, String body) {
        return Mono.fromRunnable(() -> {
            emailSentCounter.increment();

            log.info("""

                ╔════════════════════════════════════════════════════════════════╗
                ║                    📧 MOCK EMAIL SENT                           ║
                ╠════════════════════════════════════════════════════════════════╣
                ║ To: {}
                ║ User: {}
                ║ Subject: {}
                ║ Body: {}
                ║
                ║ [MOCK MODE - No actual email sent]
                ║ Production: Would use SendGrid/AWS SES
                ╚════════════════════════════════════════════════════════════════╝
                """,
                emailAddress != null ? emailAddress : "Unknown",
                userId,
                subject,
                body.length() > 100 ? body.substring(0, 100) + "..." : body
            );
        });
    }
}
