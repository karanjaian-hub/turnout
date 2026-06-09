package com.turnout.authservice.dto;

import java.util.UUID;

public record RegisterResponse(
        UUID userId,
        String username,
        String email,
        String message
) {}
