package com.turnout.paymentservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

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

    @Bean("authServiceWebClient")
    public WebClient authServiceWebClient() {
        return WebClient.builder()
                .baseUrl("http://authservice:8081")
                .build();
    }
}
