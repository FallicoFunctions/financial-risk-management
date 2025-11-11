package com.nickfallico.financialriskmanagement.config;


import io.r2dbc.spi.ConnectionFactory;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.r2dbc.config.AbstractR2dbcConfiguration;
import org.springframework.r2dbc.connection.init.ConnectionFactoryInitializer;
import org.springframework.r2dbc.connection.init.ResourceDatabasePopulator;
 
/**
 * Test configuration for R2DBC.
 *
 * This configuration is used by integration tests to set up the test database.
 * It uses the main R2DBC connection factory but can be customized for tests.
 */
@TestConfiguration
public class TestR2dbcConfig {
 
    /**
     * Optional: Initialize test database with schema/data.
     * Uncomment and customize if needed.
     */
    /*
    @Bean
    public ConnectionFactoryInitializer initializer(ConnectionFactory connectionFactory) {
        ConnectionFactoryInitializer initializer = new ConnectionFactoryInitializer();
        initializer.setConnectionFactory(connectionFactory);
 
        // Optional: Load test data
        // ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        // populator.addScript(new ClassPathResource("test-schema.sql"));
        // initializer.setDatabasePopulator(populator);
 
        return initializer;
    }
    */
}