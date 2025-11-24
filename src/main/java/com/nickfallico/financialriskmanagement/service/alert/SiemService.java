package com.nickfallico.financialriskmanagement.service.alert;

import reactor.core.publisher.Mono;

/**
 * Interface for SIEM (Security Information and Event Management) service.
 * Implementations can be production (Splunk, DataDog, Elastic SIEM) or mock.
 */
public interface SiemService {

    /**
     * Log security event to SIEM system.
     *
     * @param eventType Type of security event
     * @param severity Event severity (critical, high, medium, low)
     * @param userId User involved in the event
     * @param details Event details as JSON
     * @return Mono completing when event is logged
     */
    Mono<Void> logSecurityEvent(String eventType, String severity, String userId, String details);
}
