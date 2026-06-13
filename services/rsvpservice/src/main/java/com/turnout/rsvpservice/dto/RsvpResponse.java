package com.turnout.rsvpservice.dto;

import java.time.LocalDateTime;

public record RsvpResponse(
        boolean success,
        String message,
        boolean wasWaitlisted,
        String guestName,
        String eventTitle,
        String eventLocation,
        LocalDateTime eventDate
) {}
