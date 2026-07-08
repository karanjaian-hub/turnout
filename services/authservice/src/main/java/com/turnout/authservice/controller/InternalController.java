package com.turnout.authservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.Map;

/**
 * Internal endpoints,, called service-to-service only — never by end users.
 * The gateway routes /api/internal/** without JWT validation.
 */
@Slf4j
@RestController
@RequestMapping("/api/internal")
@RequiredArgsConstructor
public class InternalController {

    private final DataSource dataSource;

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Long>> getStats() {
        long totalUsers = 0L;
        long activeUsers = 0L;

        try (Connection conn = dataSource.getConnection()) {
            try (ResultSet rs = conn.createStatement()
                    .executeQuery("SELECT COUNT(*) FROM auth.users")) {
                if (rs.next()) totalUsers = rs.getLong(1);
            }
            try (ResultSet rs = conn.createStatement()
                    .executeQuery("SELECT COUNT(*) FROM auth.users WHERE status = 'ACTIVE'")) {
                if (rs.next()) activeUsers = rs.getLong(1);
            }
        } catch (Exception ex) {
            log.error("Failed to query user stats: {}", ex.getMessage());
            return ResponseEntity.internalServerError().build();
        }

        return ResponseEntity.ok(Map.of(
                "totalUsers", totalUsers,
                "activeUsers", activeUsers
        ));
    }
}
