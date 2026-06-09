package com.turnout.authservice.dto;

public record MessageResponse(
        String message,
        boolean success
) {}
