package com.nickfallico.financialriskmanagement.service.alert;

import reactor.core.publisher.Mono;

/**
 * Interface for Slack notification service.
 * Implementations can be production (Slack Webhook API) or mock.
 */
public interface SlackService {

    /**
     * Post message to Slack channel.
     *
     * @param channel Slack channel name (e.g., "#fraud-alerts")
     * @param message Message content (can include Slack markdown)
     * @return Mono completing when message is posted
     */
    Mono<Void> postToChannel(String channel, String message);
}
