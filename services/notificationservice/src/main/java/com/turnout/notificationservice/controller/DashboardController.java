package com.turnout.notificationservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.turnout.notificationservice.dto.PlatformStatsResponse;
import com.turnout.notificationservice.dto.SystemHealthResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final RedisTemplate<String, Object> redisTemplate;
    private final WebClient.Builder webClientBuilder;

    private static final String PLATFORM_STATS_KEY = "dashboard:platform-stats";
    private static final long   PLATFORM_STATS_TTL = 30L;
    private static final String ROLE_ADMIN         = "ADMIN";
    private static final String ROLE_SUPER_ADMIN   = "SUPER_ADMIN";

    @GetMapping("/stats/{eventId}")
    public ResponseEntity<Map<Object, Object>> getStats(
            @PathVariable UUID eventId,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Role") String userRole) {

        Map<Object, Object> stats = redisTemplate.opsForHash()
                .entries("dashboard:stats:%s".formatted(eventId));
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/email-progress/{eventId}")
    public ResponseEntity<Map<Object, Object>> getEmailProgress(
            @PathVariable UUID eventId,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Role") String userRole) {

        Map<Object, Object> progress = redisTemplate.opsForHash()
                .entries("email:progress:%s".formatted(eventId));
        return ResponseEntity.ok(progress);
    }

    @GetMapping("/recent-rsvps/{eventId}")
    public ResponseEntity<List<Object>> getRecentRsvps(
            @PathVariable UUID eventId,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Role") String userRole) {

        List<Object> recent = redisTemplate.opsForList()
                .range("recent-rsvps:%s".formatted(eventId), 0, 19);
        return ResponseEntity.ok(recent != null ? recent : List.of());
    }

    @GetMapping("/stats")
    public ResponseEntity<PlatformStatsResponse> getPlatformStats(
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Role") String userRole) {

        requireAdminRole(userRole);

        Object cached = redisTemplate.opsForValue().get(PLATFORM_STATS_KEY);
        if (cached != null) {
            return ResponseEntity.ok(convertCached(cached, PlatformStatsResponse.class));
        }

        PlatformStatsResponse stats = computePlatformStats();
        redisTemplate.opsForValue().set(PLATFORM_STATS_KEY, stats, PLATFORM_STATS_TTL, TimeUnit.SECONDS);
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/recent-rsvps")
    public ResponseEntity<List<Object>> getPlatformRecentRsvps(
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Role") String userRole) {

        requireAdminRole(userRole);
        List<Object> recent = redisTemplate.opsForList()
                .range("recent-rsvps:platform", 0, 49);
        return ResponseEntity.ok(recent != null ? recent : List.of());
    }

    private void requireAdminRole(String userRole) {
        if (!ROLE_ADMIN.equals(userRole) && !ROLE_SUPER_ADMIN.equals(userRole)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN,
                    "Admin access required");
        }
    }

    @SuppressWarnings("unchecked")
    private PlatformStatsResponse computePlatformStats() {
        WebClient client = webClientBuilder.build();

        long totalEvents = 0L;
        long activeEventsCount = 0L;
        try {
            Map response = client.get()
                    .uri("http://eventservice:8082/api/events?size=1000")
                    .header("X-User-Id", "89810216-3eb4-48d4-98be-fb154960cf17")
                    .header("X-User-Role", "SUPER_ADMIN")
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(3))
                    .block();
            if (response != null) {
                if (response.containsKey("totalElements")) {
                    totalEvents = ((Number) response.get("totalElements")).longValue();
                }
                Object contentObj = response.get("content");
                if (contentObj instanceof List<?> contentList) {
                    activeEventsCount = contentList.stream()
                            .filter(item -> item instanceof Map<?,?> m
                                    && "ACTIVE".equals(m.get("status")))
                            .count();
                }
            }
        } catch (Exception ex) {
            log.warn("Could not reach event-service for platform stats: {}", ex.getMessage());
        }

        long totalOrganizers = 0L;
        try {
            Map response = client.get()
                    .uri("http://authservice:8081/api/internal/stats")
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(3))
                    .block();
            if (response != null && response.containsKey("totalUsers")) {
                totalOrganizers = ((Number) response.get("totalUsers")).longValue();
            }
        } catch (Exception ex) {
            log.warn("Could not reach auth-service for platform stats: {}", ex.getMessage());
        }

        long totalConfirmedRsvps = sumRedisStatField("confirmed");
        long totalGuestsInvited  = sumRedisStatField("totalInvited");
        long totalRevenueKes     = 0L;

        return new PlatformStatsResponse(
                totalEvents, activeEventsCount, totalOrganizers,
                totalGuestsInvited, totalConfirmedRsvps, totalRevenueKes
        );
    }

    private long sumRedisStatField(String field) {
        long total = 0L;
        try {
            var keys = redisTemplate.keys("dashboard:stats:*");
            if (keys == null) return 0L;
            for (String key : keys) {
                Object val = redisTemplate.opsForHash().get(key, field);
                if (val != null) {
                    total += Long.parseLong(val.toString());
                }
            }
        } catch (Exception ex) {
            log.warn("Redis scan for {} failed: {}", field, ex.getMessage());
        }
        return total;
    }

    private <T> T convertCached(Object cached, Class<T> type) {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.convertValue(cached, type);
    }
}
