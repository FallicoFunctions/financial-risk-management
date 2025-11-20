package com.nickfallico.financialriskmanagement.kafka.config;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import com.nickfallico.financialriskmanagement.kafka.event.FraudClearedEvent;
import com.nickfallico.financialriskmanagement.kafka.event.FraudDetectedEvent;
import com.nickfallico.financialriskmanagement.kafka.event.TransactionBlockedEvent;
import com.nickfallico.financialriskmanagement.kafka.event.TransactionCreatedEvent;

@Configuration
@ConditionalOnProperty(name = "spring.kafka.enabled", havingValue = "true", matchIfMissing = true)
@EnableKafka
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    /**
     * Base consumer configuration shared by all factories.
     * Only includes Kafka connection and consumer settings - NO deserializer config.
     */
    private Map<String, Object> baseConsumerConfigs() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return props;
    }

    /**
     * Generic consumer factory for TransactionCreatedEvent.
     * Used by TransactionEventConsumer.
     */
    @Bean
    public ConsumerFactory<String, TransactionCreatedEvent> consumerFactory() {
        JsonDeserializer<TransactionCreatedEvent> deserializer = 
            new JsonDeserializer<>(TransactionCreatedEvent.class);
        deserializer.addTrustedPackages("*");
        deserializer.setUseTypeHeaders(false);
        
        return new DefaultKafkaConsumerFactory<>(
            baseConsumerConfigs(),
            new StringDeserializer(),
            deserializer
        );
    }

    /**
     * Generic listener container factory for TransactionCreatedEvent.
     * Used by TransactionEventConsumer with containerFactory = "kafkaListenerContainerFactory".
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, TransactionCreatedEvent> 
            kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, TransactionCreatedEvent> factory = 
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.setConcurrency(3); // 3 concurrent consumers for high throughput
        return factory;
    }

    /**
     * Event-specific factory for FraudDetectedEvent.
     * Ensures type-safe deserialization for fraud detection events.
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, FraudDetectedEvent> 
            fraudDetectedKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, FraudDetectedEvent> factory =
            new ConcurrentKafkaListenerContainerFactory<>();
        
        JsonDeserializer<FraudDetectedEvent> deserializer = 
            new JsonDeserializer<>(FraudDetectedEvent.class);
        deserializer.addTrustedPackages("*");
        deserializer.setUseTypeHeaders(false);
        
        ConsumerFactory<String, FraudDetectedEvent> consumerFactory =
            new DefaultKafkaConsumerFactory<>(
                baseConsumerConfigs(),
                new StringDeserializer(),
                deserializer
            );
        
        factory.setConsumerFactory(consumerFactory);
        factory.setConcurrency(2); // Lower concurrency for critical fraud events
        return factory;
    }

    /**
     * Event-specific factory for FraudClearedEvent.
     * Ensures type-safe deserialization for cleared fraud events.
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, FraudClearedEvent> 
            fraudClearedKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, FraudClearedEvent> factory =
            new ConcurrentKafkaListenerContainerFactory<>();
        
        JsonDeserializer<FraudClearedEvent> deserializer = 
            new JsonDeserializer<>(FraudClearedEvent.class);
        deserializer.addTrustedPackages("*");
        deserializer.setUseTypeHeaders(false);
        
        ConsumerFactory<String, FraudClearedEvent> consumerFactory =
            new DefaultKafkaConsumerFactory<>(
                baseConsumerConfigs(),
                new StringDeserializer(),
                deserializer
            );
        
        factory.setConsumerFactory(consumerFactory);
        factory.setConcurrency(2);
        return factory;
    }

    /**
     * Event-specific factory for TransactionBlockedEvent.
     * Ensures type-safe deserialization for blocked transaction events.
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, TransactionBlockedEvent> 
            transactionBlockedKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, TransactionBlockedEvent> factory =
            new ConcurrentKafkaListenerContainerFactory<>();
        
        JsonDeserializer<TransactionBlockedEvent> deserializer = 
            new JsonDeserializer<>(TransactionBlockedEvent.class);
        deserializer.addTrustedPackages("*");
        deserializer.setUseTypeHeaders(false);
        
        ConsumerFactory<String, TransactionBlockedEvent> consumerFactory =
            new DefaultKafkaConsumerFactory<>(
                baseConsumerConfigs(),
                new StringDeserializer(),
                deserializer
            );
        
        factory.setConsumerFactory(consumerFactory);
        factory.setConcurrency(2);
        return factory;
    }
}