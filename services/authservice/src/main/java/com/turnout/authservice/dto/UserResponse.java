package com.turnout.authservice.dto;

import java.util.UUID;

public record UserResponse(
        UUID id,
        String username,
        String email,
        String fullName,
        String role,
        String status,
        boolean emailVerified
) {}
