package com.nickfallico.financialriskmanagement.config;

import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Ensures a PrometheusMeterRegistry is available in the test context.
 */
@TestConfiguration
public class TestPrometheusRegistryConfig {

    @Bean
    @Primary
    public PrometheusMeterRegistry prometheusMeterRegistry() {
        return new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
    }
}
