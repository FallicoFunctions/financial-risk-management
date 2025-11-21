package com.nickfallico.financialriskmanagement.config;
 
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
 
/**
 * Test-specific security configuration.
 * Ensures all endpoints including actuator are accessible during testing.
 */
@TestConfiguration
@EnableWebFluxSecurity
public class TestSecurityConfig {
 
    @Bean
    public SecurityWebFilterChain testSecurityFilterChain(ServerHttpSecurity http) {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeExchange(auth -> auth
                .pathMatchers("/actuator/**").permitAll()
                .pathMatchers("/api/**").permitAll()
                .anyExchange().permitAll()
            );
        return http.build();
    }
}