package com.turnout.notificationservice.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Broadcast over /topic/email-progress/{eventId} as the email-service
 * sends out invitation batches. Drives the progress bar on the admin UI.
 */
public record EmailProgressMessage(
        UUID eventId,
        long totalEmails,
        long sentCount,
        long failedCount,
        int progressPercentage,
        LocalDateTime timestamp
) {}
