package com.turnout.authservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Username is required")
        String username,

        @Email(message = "Invalid email format")
        @NotBlank(message = "Email is required")
        String email,

        @NotBlank(message = "Full name is required")
        String fullName,

        @Size(min = 8, message = "Password must be at least 8 characters")
        @NotBlank(message = "Password is required")
        String password
) {}
