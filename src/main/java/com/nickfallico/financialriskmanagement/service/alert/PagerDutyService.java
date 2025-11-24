package com.nickfallico.financialriskmanagement.service.alert;

import reactor.core.publisher.Mono;

/**
 * Interface for PagerDuty incident management service.
 * Implementations can be production (PagerDuty API) or mock.
 */
public interface PagerDutyService {

    /**
     * Create incident in PagerDuty.
     *
     * @param title Incident title
     * @param description Incident description
     * @param severity Severity level (critical, error, warning, info)
     * @param details Additional details as JSON
     * @return Mono completing when incident is created
     */
    Mono<String> createIncident(String title, String description, String severity, String details);

    /**
     * Trigger PagerDuty incident for immediate on-call response.
     *
     * @param title Incident title
     * @param description Incident description
     * @return Mono completing when incident is triggered
     */
    Mono<Void> triggerIncident(String title, String description);
}
