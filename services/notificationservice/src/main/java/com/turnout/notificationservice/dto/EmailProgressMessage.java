package com.turnout.notificationservice.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record EmailProgressMessage(
        UUID eventId,
        long totalEmails,
        long sentCount,
        long failedCount,
        int progressPercentage,
        LocalDateTime timestamp
) {}
