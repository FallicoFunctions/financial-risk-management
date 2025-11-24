package com.nickfallico.financialriskmanagement.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
 
/**
 * WebFlux Security configuration.
 * Configures security rules for REST API and WebSocket endpoints.
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {
    @Bean
    public SecurityWebFilterChain securityFilterChain(ServerHttpSecurity http) {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeExchange(auth -> auth
                .pathMatchers("/actuator/**").permitAll()  // Explicitly permit actuator endpoints
                .pathMatchers("/ws/**").permitAll()        // WebSocket endpoints for dashboard streaming
                .anyExchange().permitAll()
            );
        return http.build();
    }
}