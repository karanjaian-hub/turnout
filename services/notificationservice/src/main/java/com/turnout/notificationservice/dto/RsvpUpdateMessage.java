package com.turnout.notificationservice.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Broadcast over /topic/rsvp-updates/{eventId} whenever an RSVP is processed.
 * Contains full dashboard counts so the client doesn't need a follow-up HTTP call.
 */
public record RsvpUpdateMessage(
        UUID eventId,
        long confirmedCount,
        long declinedCount,
        long maybeCount,
        long pendingCount,
        long waitlistedCount,
        long totalInvited,
        double confirmationRate,
        LocalDateTime timestamp
) {}
