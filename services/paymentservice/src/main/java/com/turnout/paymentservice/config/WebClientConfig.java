package com.turnout.paymentservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Pre-configured WebClient instances per external API.
 * WHY separate beans: each API has a different base URL and auth scheme.
 * Injecting a named bean is cleaner than constructing WebClient inline in services.
 */
@Configuration
public class WebClientConfig {

    @Bean("mpesaWebClient")
    public WebClient mpesaWebClient(MpesaProperties props) {
        return WebClient.builder()
                .baseUrl(props.getApiBaseUrl())
                .build();
    }

    @Bean("stripeWebClient")
    public WebClient stripeWebClient() {
        return WebClient.builder()
                .baseUrl("https://api.stripe.com")
                .build();
    }

    @Bean("eventServiceWebClient")
    public WebClient eventServiceWebClient() {
        return WebClient.builder()
                .baseUrl("http://eventservice:8082")
                .build();
    }
}
