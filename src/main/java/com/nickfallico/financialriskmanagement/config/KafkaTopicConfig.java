package com.nickfallico.financialriskmanagement.config;

import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka topic configuration.
 * Creates all required topics at application startup.
 * This ensures topics exist before any messages are published.
 */
@Configuration
public class KafkaTopicConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${kafka.topic.transaction-created}")
    private String transactionCreatedTopic;

    @Value("${kafka.topic.fraud-detected}")
    private String fraudDetectedTopic;

    @Value("${kafka.topic.fraud-cleared}")
    private String fraudClearedTopic;

    @Value("${kafka.topic.transaction-blocked}")
    private String transactionBlockedTopic;

    @Value("${kafka.topic.user-profile-updated}")
    private String userProfileUpdatedTopic;

    @Value("${kafka.topic.high-risk-user}")
    private String highRiskUserTopic;

    /**
     * KafkaAdmin bean for managing topics
     */
    @Bean
    public KafkaAdmin kafkaAdmin() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        return new KafkaAdmin(configs);
    }

    /**
     * Topic: transaction.created
     * Published when a new transaction is created
     */
    @Bean
    public NewTopic transactionCreatedTopic() {
        return TopicBuilder.name(transactionCreatedTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    /**
     * Topic: fraud.detected
     * Published when fraud is detected in a transaction
     */
    @Bean
    public NewTopic fraudDetectedTopic() {
        return TopicBuilder.name(fraudDetectedTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    /**
     * Topic: fraud.cleared
     * Published when a transaction passes fraud checks
     */
    @Bean
    public NewTopic fraudClearedTopic() {
        return TopicBuilder.name(fraudClearedTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    /**
     * Topic: transaction.blocked
     * Published when a transaction is blocked due to fraud
     */
    @Bean
    public NewTopic transactionBlockedTopic() {
        return TopicBuilder.name(transactionBlockedTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    /**
     * Topic: user.profile.updated
     * Published when a user's risk profile is updated
     */
    @Bean
    public NewTopic userProfileUpdatedTopic() {
        return TopicBuilder.name(userProfileUpdatedTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    /**
     * Topic: user.high-risk
     * Published when a user is flagged as high-risk
     */
    @Bean
    public NewTopic highRiskUserTopic() {
        return TopicBuilder.name(highRiskUserTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }
}