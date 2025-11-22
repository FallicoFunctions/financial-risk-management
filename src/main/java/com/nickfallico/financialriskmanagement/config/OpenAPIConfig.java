package com.nickfallico.financialriskmanagement.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI (Swagger) documentation configuration for Financial Risk Management API.
 * Access the interactive API documentation at: /swagger-ui.html
 * Access the OpenAPI specification at: /v3/api-docs
 */
@Configuration
public class OpenAPIConfig {

    @Bean
    public OpenAPI financialRiskManagementAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Financial Risk Management Platform API")
                .description("""
                    Advanced Transaction Risk Management Microservice

                    This API provides comprehensive fraud detection and risk assessment capabilities for financial transactions.

                    ## Key Features:
                    - Real-time transaction risk assessment using ML models
                    - User risk profiling and behavioral analysis
                    - Fraud detection and alerting
                    - Event-driven architecture with Kafka
                    - Transaction monitoring and analytics
                    - Administrative controls for fraud investigation

                    ## Authentication:
                    All endpoints require HTTP Basic authentication.

                    ## Rate Limiting:
                    API endpoints are subject to rate limiting. Contact your administrator for limits.

                    ## Event-Driven Architecture:
                    This service publishes events to Kafka topics for asynchronous processing.
                    See the Kafka Events section for event schemas.
                    """)
                .version("0.0.1-SNAPSHOT")
                .contact(new Contact()
                    .name("Nick Fallico")
                    .email("nick@example.com")
                    .url("https://github.com/nickfallico"))
                .license(new License()
                    .name("MIT License")
                    .url("https://opensource.org/licenses/MIT")))
            .servers(List.of(
                new Server()
                    .url("http://localhost:8080")
                    .description("Development Server"),
                new Server()
                    .url("https://api.financial-risk.example.com")
                    .description("Production Server")))
            .tags(List.of(
                new Tag()
                    .name("Transactions")
                    .description("Transaction creation and querying endpoints"),
                new Tag()
                    .name("Risk Assessment")
                    .description("User risk profiles and assessment endpoints"),
                new Tag()
                    .name("Fraud Detection")
                    .description("Fraud event history and monitoring"),
                new Tag()
                    .name("Administration")
                    .description("Administrative endpoints for fraud investigation and rule management"),
                new Tag()
                    .name("Health & Monitoring")
                    .description("Health checks and system status"),
                new Tag()
                    .name("Event Replay")
                    .description("Event sourcing and replay capabilities")));
    }
}
