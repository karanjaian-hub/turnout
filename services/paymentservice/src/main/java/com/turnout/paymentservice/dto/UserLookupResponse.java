package com.turnout.paymentservice.dto;

import java.util.UUID;

/**
 * Minimal user info returned by auth-service GET /api/auth/users/{id}.
 * Never contains password or sensitive fields.
 */
public record UserLookupResponse(
        UUID   id,
        String username,
        String email,
        String fullName
) {}
