package com.nickfallico.financialriskmanagement.service.notification;

import reactor.core.publisher.Mono;

/**
 * Interface for email notification service.
 * Implementations can be production (SendGrid, AWS SES) or mock.
 */
public interface EmailService {

    /**
     * Send email to user.
     *
     * @param userId User identifier
     * @param emailAddress User's email address
     * @param subject Email subject
     * @param body Email body (HTML or plain text)
     * @return Mono completing when email is sent
     */
    Mono<Void> sendEmail(String userId, String emailAddress, String subject, String body);
}
