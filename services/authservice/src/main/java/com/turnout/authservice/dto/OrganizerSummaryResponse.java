package com.turnout.authservice.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record OrganizerSummaryResponse(
        UUID id,
        String username,
        String email,
        String fullName,
        String status,
        LocalDateTime createdAt,
        LocalDateTime lastLoginAt
) {}
