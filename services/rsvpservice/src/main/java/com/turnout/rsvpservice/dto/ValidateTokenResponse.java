package com.turnout.rsvpservice.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record ValidateTokenResponse(
        boolean valid,
        UUID guestId,
        UUID eventId,
        String guestName,
        String eventTitle,
        LocalDateTime eventDate,
        String eventLocation,
        boolean alreadyResponded,
        String currentRsvpStatus
) {}
