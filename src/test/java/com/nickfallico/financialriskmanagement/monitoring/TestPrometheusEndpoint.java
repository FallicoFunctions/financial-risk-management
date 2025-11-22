package com.nickfallico.financialriskmanagement.monitoring;

import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.micrometer.prometheus.PrometheusMeterRegistry;

/**
 * Test-only Prometheus endpoint to ensure /actuator/prometheus is available in @SpringBootTest.
 * This avoids the actuator auto-configuration quirks that sometimes skip registering
 * the PrometheusScrapeEndpoint during test slices.
 */
@RestController
@Profile("test")
@RequestMapping("/actuator")
@Import(com.nickfallico.financialriskmanagement.config.TestPrometheusRegistryConfig.class)
public class TestPrometheusEndpoint {

    private final PrometheusMeterRegistry registry;

    public TestPrometheusEndpoint(PrometheusMeterRegistry registry) {
        this.registry = registry;
    }

    @GetMapping(value = "/prometheus", produces = MediaType.TEXT_PLAIN_VALUE)
    public String prometheus() {
        return registry.scrape();
    }
}
