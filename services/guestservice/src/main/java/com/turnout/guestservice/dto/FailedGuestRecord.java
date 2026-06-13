package com.turnout.guestservice.dto;

public record FailedGuestRecord(
        int rowNumber,
        String email,
        String fullName,
        String reason
) {}
