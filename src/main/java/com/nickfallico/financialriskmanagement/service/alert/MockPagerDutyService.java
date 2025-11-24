package com.nickfallico.financialriskmanagement.service.alert;

import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * Mock PagerDuty service for development and demonstration.
 * Logs incident creation instead of actually triggering PagerDuty.
 *
 * In production, replace with real PagerDuty Events API integration.
 */
@Service
@Slf4j
@ConditionalOnProperty(name = "alert.pagerduty.provider", havingValue = "mock", matchIfMissing = true)
public class MockPagerDutyService implements PagerDutyService {

    private final Counter incidentsCreatedCounter;
    private final Counter incidentsTriggeredCounter;

    public MockPagerDutyService(MeterRegistry meterRegistry) {
        this.incidentsCreatedCounter = Counter.builder("alerts.pagerduty.incidents.created")
            .description("Total number of PagerDuty incidents created (mock)")
            .tag("provider", "mock")
            .register(meterRegistry);

        this.incidentsTriggeredCounter = Counter.builder("alerts.pagerduty.incidents.triggered")
            .description("Total number of PagerDuty incidents triggered (mock)")
            .tag("provider", "mock")
            .register(meterRegistry);
    }

    @Override
    public Mono<String> createIncident(String title, String description, String severity, String details) {
        return Mono.fromCallable(() -> {
            incidentsCreatedCounter.increment();
            String incidentId = "INC-" + UUID.randomUUID().toString().substring(0, 8);

            log.error("""

                ╔════════════════════════════════════════════════════════════════╗
                ║              🚨 MOCK PAGERDUTY INCIDENT CREATED 🚨              ║
                ╠════════════════════════════════════════════════════════════════╣
                ║ Incident ID: {}
                ║ Title: {}
                ║ Severity: {}
                ║ Description: {}
                ║ Details: {}
                ║
                ║ [MOCK MODE - No actual PagerDuty incident created]
                ║ Production: Would trigger on-call engineer alert
                ╚════════════════════════════════════════════════════════════════╝
                """,
                incidentId,
                title,
                severity.toUpperCase(),
                description,
                details.length() > 200 ? details.substring(0, 200) + "..." : details
            );

            return incidentId;
        });
    }

    @Override
    public Mono<Void> triggerIncident(String title, String description) {
        return Mono.fromRunnable(() -> {
            incidentsTriggeredCounter.increment();

            log.error("""

                ╔════════════════════════════════════════════════════════════════╗
                ║            📟 MOCK PAGERDUTY INCIDENT TRIGGERED 📟              ║
                ╠════════════════════════════════════════════════════════════════╣
                ║ Title: {}
                ║ Description: {}
                ║
                ║ [MOCK MODE - On-call engineer would be paged]
                ║ Production: Would wake up on-call at 3am
                ╚════════════════════════════════════════════════════════════════╝
                """,
                title,
                description
            );
        });
    }
}
