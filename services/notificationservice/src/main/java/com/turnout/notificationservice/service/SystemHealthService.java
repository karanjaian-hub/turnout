package com.turnout.notificationservice.service;

import com.turnout.notificationservice.dto.SystemHealthResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class SystemHealthService {

    private final WebClient.Builder webClientBuilder;

// All 9 services
    private static final List<Map.Entry<String, String>> SERVICES = List.of(
            Map.entry("api-gateway",           "http://api-gateway:8080"),
            Map.entry("auth-service",          "http://authservice:8081"),
            Map.entry("event-service",         "http://eventservice:8082"),
            Map.entry("guest-service",         "http://guestservice:8083"),
            Map.entry("email-service",         "http://emailservice:8084"),
            Map.entry("rsvp-service",          "http://rsvpservice:8085"),
            Map.entry("payment-service",       "http://paymentservice:8087"),
            Map.entry("ai-service",            "http://aiservice:8088")
// notification-service (self) handled below without a network call
    );

    private static final Duration HEALTH_CHECK_TIMEOUT = Duration.ofSeconds(2);

    /**
     * Calls /actuator/health on all 8 external services in parallel.
     * Self (notification-service) is always UP — no network round-trip needed.
     * Any service that times out or returns non-2xx is marked DOWN.
     * Blocks until all calls complete or timeout — safe on virtual threads.
     */
    public SystemHealthResponse checkAll() {
        Map<String, String> statuses = new ConcurrentHashMap<>();

        // Self-report without a network call
        statuses.put("notification-service", "UP");

        // Build one Mono per external service
        List<Mono<Void>> checks = SERVICES.stream()
                .map(entry -> pingService(entry.getKey(), entry.getValue(), statuses))
                .toList();

        // Fan out all calls concurrently, wait for the slowest (bounded by timeout per call)
        Mono.when(checks).block();

        String overall = statuses.values().stream().allMatch("UP"::equals) ? "HEALTHY" : "DEGRADED";

        return new SystemHealthResponse(statuses, overall);
    }

    /**
     * Pings one service's actuator health endpoint.
     * On any error or timeout, writes DOWN into the shared map rather than propagating.
     */
    private Mono<Void> pingService(String name, String baseUrl, Map<String, String> statuses) {
        return webClientBuilder
                .baseUrl(baseUrl)
                .build()
                .get()
                .uri("/actuator/health")
                .retrieve()
                .toBodilessEntity()
                .timeout(HEALTH_CHECK_TIMEOUT)
                .doOnSuccess(response -> {
// Any 2xx means the service is reachable and healthy
                    statuses.put(name, "UP");
                    log.debug("{} health check: UP", name);
                })
                .onErrorResume(ex -> {
// Timeout, connection refused, non-2xx — all map to DOWN
                    statuses.put(name, "DOWN");
                    log.warn("{} health check failed: {}", name, ex.getMessage());
                    return Mono.empty();
                })
                .then();
    }
}
