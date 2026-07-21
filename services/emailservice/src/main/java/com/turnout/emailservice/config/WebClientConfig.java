package com.turnout.emailservice.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@RequiredArgsConstructor
public class WebClientConfig {

    private final BrevoProperties brevoProperties;

// Base URL is set once here. Every call in EmailDispatchService only needs
// to specify the path (e.g. "/smtp/email"), not the full URL.
    @Bean
    public WebClient webClient() {
        return WebClient.builder()
                .baseUrl(brevoProperties.getApi().getBaseUrl())
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader("Accept", "application/json")
                .build();
    }
}
