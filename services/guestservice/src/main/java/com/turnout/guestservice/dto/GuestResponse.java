package com.turnout.guestservice.dto;

import com.turnout.common.enums.RsvpStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record GuestResponse(
        UUID id,
        UUID eventId,
        String fullName,
        String email,
        RsvpStatus rsvpStatus,
        boolean tokenUsed,
        LocalDateTime rsvpDate,
        LocalDateTime createdAt
) {}
