package com.nickfallico.financialriskmanagement.service.notification;

import reactor.core.publisher.Mono;

/**
 * Interface for push notification service.
 * Implementations can be production (Firebase, OneSignal) or mock.
 */
public interface PushNotificationService {

    /**
     * Send push notification to user's mobile device.
     *
     * @param userId User identifier
     * @param title Notification title
     * @param message Notification message
     * @param data Additional data payload
     * @return Mono completing when notification is sent
     */
    Mono<Void> sendPushNotification(String userId, String title, String message, String data);
}
