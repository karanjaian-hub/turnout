package com.turnout.guestservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
public class EventServiceClient {

    private final WebClient webClient;

    public EventServiceClient(@Value("${turnout.event-service.url:http://eventservice:8082}") String baseUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    // Fetches just the event date — used to calculate guest token expiry
    public LocalDateTime getEventDate(UUID eventId) {
        return webClient.get()
                .uri("/api/events/{id}/date", eventId)
                .retrieve()
                .bodyToMono(LocalDateTime.class)
                // If event-service is down, fall back to 30 days from now
                // so import doesn't fail entirely over a date fetch
                .onErrorReturn(LocalDateTime.now().plusDays(30))
                .block();
    }
}
