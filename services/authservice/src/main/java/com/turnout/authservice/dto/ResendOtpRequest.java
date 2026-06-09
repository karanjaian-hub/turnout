package com.turnout.authservice.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ResendOtpRequest(
        @NotNull(message = "User ID is required")
        UUID userId
) {}
