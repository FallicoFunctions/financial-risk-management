package com.nickfallico.financialriskmanagement.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.r2dbc.ConnectionFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.config.AbstractR2dbcConfiguration;
import org.springframework.data.r2dbc.convert.R2dbcCustomConversions;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

import io.r2dbc.spi.ConnectionFactory;

@Configuration
@EnableR2dbcRepositories(basePackages = {
    "com.nickfallico.financialriskmanagement.repository",
    "com.nickfallico.financialriskmanagement.eventstore.repository"
})
public class R2dbcConfig extends AbstractR2dbcConfiguration {
    @Value("${spring.r2dbc.url}")
    private String url;

    @Value("${spring.r2dbc.username}")
    private String username;

    @Value("${spring.r2dbc.password}")
    private String password;

    @Override
    public ConnectionFactory connectionFactory() {
        return ConnectionFactoryBuilder.withUrl(url)
            .username(username)
            .password(password)
            .build();
    }

    @Bean
    @Override
    public R2dbcCustomConversions r2dbcCustomConversions() {
        return R2dbcJsonbConverter.customConversions();
    }
}