package com.turnout.guestservice.dto;

import java.util.UUID;

public record EventGuestStatsResponse(
        UUID eventId,
        long total,
        long confirmed,
        long declined,
        long pending,
        long maybe,
        long waitlisted
) {}
