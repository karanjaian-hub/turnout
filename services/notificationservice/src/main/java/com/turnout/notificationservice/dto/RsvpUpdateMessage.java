package com.turnout.notificationservice.dto;

import java.time.LocalDateTime;
import java.util.UUID;

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
