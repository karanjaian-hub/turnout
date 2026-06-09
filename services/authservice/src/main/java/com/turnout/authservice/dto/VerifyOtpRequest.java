package com.turnout.authservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record VerifyOtpRequest(
        @NotNull(message = "User ID is required")
        UUID userId,

        @NotBlank(message = "OTP is required")
        String otp
) {}
