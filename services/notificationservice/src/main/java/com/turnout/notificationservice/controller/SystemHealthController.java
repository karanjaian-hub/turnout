package com.turnout.notificationservice.controller;

import com.turnout.notificationservice.dto.SystemHealthResponse;
import com.turnout.notificationservice.service.SystemHealthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class SystemHealthController {

    private final SystemHealthService systemHealthService;

    private static final String ROLE_ADMIN       = "ADMIN";
    private static final String ROLE_SUPER_ADMIN = "SUPER_ADMIN";

    @GetMapping("/system-health")
    public ResponseEntity<SystemHealthResponse> getSystemHealth(
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Role") String userRole) {

        requireAdminRole(userRole);
        return ResponseEntity.ok(systemHealthService.checkAll());
    }

    private void requireAdminRole(String userRole) {
        if (!ROLE_ADMIN.equals(userRole) && !ROLE_SUPER_ADMIN.equals(userRole)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN,
                    "Admin access required");
        }
    }
}
