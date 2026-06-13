package com.turnout.rsvpservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AppConfig {

    // RestTemplate is in spring-web (already a transitive dep of spring-boot-starter-web).
    // We register it as a bean so Spring can inject it into RsvpService.
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
