package com.nickfallico.financialriskmanagement.service.notification;

import reactor.core.publisher.Mono;

/**
 * Interface for SMS notification service.
 * Implementations can be production (Twilio, AWS SNS) or mock.
 */
public interface SmsService {

    /**
     * Send SMS message to user.
     *
     * @param userId User identifier
     * @param phoneNumber User's phone number
     * @param message SMS message content
     * @return Mono completing when SMS is sent
     */
    Mono<Void> sendSms(String userId, String phoneNumber, String message);
}
