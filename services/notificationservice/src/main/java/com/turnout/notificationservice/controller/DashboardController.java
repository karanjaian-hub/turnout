package com.turnout.notificationservice.controller;

import com.turnout.notificationservice.model.DashboardStats;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST endpoints for the admin dashboard.
 * All reads are from Redis — no database calls, sub-millisecond response times.
 * The gateway injects X-User-Id and X-User-Role headers after JWT validation.
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final RedisTemplate<String, Object> redisTemplate;

    @GetMapping("/stats/{eventId}")
    public ResponseEntity<Map<Object, Object>> getStats(
            @PathVariable UUID eventId,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Role") String userRole) {

        log.debug("Stats requested for event {} by user {}", eventId, userId);

        Map<Object, Object> stats = redisTemplate.opsForHash()
                .entries("dashboard:stats:%s".formatted(eventId));

        // Return empty map rather than 404 — event may just have no RSVPs yet
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

        // LRANGE 0 19 = the last 20 items we LPUSH'd
        List<Object> recent = redisTemplate.opsForList()
                .range("recent-rsvps:%s".formatted(eventId), 0, 19);

        return ResponseEntity.ok(recent != null ? recent : List.of());
    }
}
