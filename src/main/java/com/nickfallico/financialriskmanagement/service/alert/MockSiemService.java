package com.nickfallico.financialriskmanagement.service.alert;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * Mock SIEM service for development and demonstration.
 * Logs security events to application log instead of SIEM system.
 *
 * In production, replace with real SIEM integration (Splunk, DataDog, Elastic).
 */
@Service
@Slf4j
@ConditionalOnProperty(name = "alert.siem.provider", havingValue = "mock", matchIfMissing = true)
public class MockSiemService implements SiemService {

    private final Counter securityEventsCounter;

    public MockSiemService(MeterRegistry meterRegistry) {
        this.securityEventsCounter = Counter.builder("alerts.siem.events.logged")
            .description("Total number of security events logged to SIEM (mock)")
            .tag("provider", "mock")
            .register(meterRegistry);
    }

    @Override
    public Mono<Void> logSecurityEvent(String eventType, String severity, String userId, String details) {
        return Mono.fromRunnable(() -> {
            securityEventsCounter.increment();

            log.warn("""

                ╔════════════════════════════════════════════════════════════════╗
                ║                🔐 MOCK SIEM SECURITY EVENT LOGGED 🔐            ║
                ╠════════════════════════════════════════════════════════════════╣
                ║ Event Type: {}
                ║ Severity: {}
                ║ User ID: {}
                ║ Details: {}
                ║ Timestamp: {}
                ║
                ║ [MOCK MODE - Logged to application log only]
                ║ Production: Would send to Splunk/DataDog/Elastic SIEM
                ╚════════════════════════════════════════════════════════════════╝
                """,
                eventType,
                severity.toUpperCase(),
                userId,
                details.length() > 300 ? details.substring(0, 300) + "..." : details,
                java.time.Instant.now()
            );
        });
    }
}
