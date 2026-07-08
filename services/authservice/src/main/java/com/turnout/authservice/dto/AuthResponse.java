package com.turnout.authservice.dto;

public record AuthResponse(
        String accessToken,
        String refreshToken,
// tells the client how to send the token
        String tokenType,
// Milliseconds until the access token expires
        long expiresIn,
        String username,
        String role
) {}
