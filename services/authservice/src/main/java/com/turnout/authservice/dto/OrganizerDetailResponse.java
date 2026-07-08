package com.turnout.authservice.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record OrganizerDetailResponse(
        UUID id,
        String username,
        String email,
        String fullName,
        String role,
        String status,
        boolean emailVerified,
        LocalDateTime createdAt,
        LocalDateTime lastLoginAt
) {}
