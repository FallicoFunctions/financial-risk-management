package com.nickfallico.financialriskmanagement.config;

import com.nickfallico.financialriskmanagement.kafka.event.*;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    private Map<String, Object> consumerConfigs() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return props;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, FraudDetectedEvent> fraudDetectedKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, FraudDetectedEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        
        JsonDeserializer<FraudDetectedEvent> deserializer = new JsonDeserializer<>(FraudDetectedEvent.class);
        deserializer.addTrustedPackages("*");
        deserializer.setUseTypeHeaders(false);
        
        ConsumerFactory<String, FraudDetectedEvent> consumerFactory =
                new DefaultKafkaConsumerFactory<>(
                        consumerConfigs(),
                        new StringDeserializer(),
                        deserializer
                );
        
        factory.setConsumerFactory(consumerFactory);
        return factory;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, FraudClearedEvent> fraudClearedKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, FraudClearedEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        
        JsonDeserializer<FraudClearedEvent> deserializer = new JsonDeserializer<>(FraudClearedEvent.class);
        deserializer.addTrustedPackages("*");
        deserializer.setUseTypeHeaders(false);
        
        ConsumerFactory<String, FraudClearedEvent> consumerFactory =
                new DefaultKafkaConsumerFactory<>(
                        consumerConfigs(),
                        new StringDeserializer(),
                        deserializer
                );
        
        factory.setConsumerFactory(consumerFactory);
        return factory;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, TransactionBlockedEvent> transactionBlockedKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, TransactionBlockedEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        
        JsonDeserializer<TransactionBlockedEvent> deserializer = new JsonDeserializer<>(TransactionBlockedEvent.class);
        deserializer.addTrustedPackages("*");
        deserializer.setUseTypeHeaders(false);
        
        ConsumerFactory<String, TransactionBlockedEvent> consumerFactory =
                new DefaultKafkaConsumerFactory<>(
                        consumerConfigs(),
                        new StringDeserializer(),
                        deserializer
                );
        
        factory.setConsumerFactory(consumerFactory);
        return factory;
    }
}