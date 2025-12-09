package com.nickfallico.financialriskmanagement.config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.ApplicationRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;

import lombok.extern.slf4j.Slf4j;

/**
 * Kafka topic configuration.
 * Creates all required topics at application startup.
 * This ensures topics exist before any messages are published.
 */
@Configuration
@ConditionalOnProperty(name = "spring.kafka.enabled", havingValue = "true", matchIfMissing = true)
@Slf4j
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

    private NewTopic buildTopic(String topicName) {
        return TopicBuilder.name(topicName)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic transactionCreatedTopic() {
        return buildTopic(transactionCreatedTopic);
    }

    @Bean
    public NewTopic fraudDetectedTopic() {
        return buildTopic(fraudDetectedTopic);
    }

    @Bean
    public NewTopic fraudClearedTopic() {
        return buildTopic(fraudClearedTopic);
    }

    @Bean
    public NewTopic transactionBlockedTopic() {
        return buildTopic(transactionBlockedTopic);
    }

    @Bean
    public NewTopic userProfileUpdatedTopic() {
        return buildTopic(userProfileUpdatedTopic);
    }

    @Bean
    public NewTopic highRiskUserTopic() {
        return buildTopic(highRiskUserTopic);
    }

    /**
     * Startup hook that double-checks all required topics exist, creating any that are missing.
     * Prevents producer send failures (like transaction.blocked) when brokers disable auto creation.
     */
    @Bean
    public ApplicationRunner kafkaTopicInitializer(KafkaAdmin kafkaAdmin, List<NewTopic> kafkaTopics) {
        return args -> {
            try (AdminClient adminClient = AdminClient.create(kafkaAdmin.getConfigurationProperties())) {
                Set<String> existingTopics = adminClient.listTopics().names().get(5, TimeUnit.SECONDS);
                List<NewTopic> missingTopics = kafkaTopics.stream()
                        .filter(topic -> !existingTopics.contains(topic.name()))
                        .collect(Collectors.toList());

                if (missingTopics.isEmpty()) {
                    log.info("All Kafka topics already exist. Skipping creation.");
                    return;
                }

                adminClient.createTopics(missingTopics).all().get(10, TimeUnit.SECONDS);
                log.info("Created Kafka topics: {}", missingTopics.stream()
                        .map(NewTopic::name)
                        .collect(Collectors.joining(", ")));
            } catch (Exception ex) {
                log.warn("Unable to verify/create Kafka topics at startup. Kafka may not be available yet.", ex);
            }
        };
    }
}
