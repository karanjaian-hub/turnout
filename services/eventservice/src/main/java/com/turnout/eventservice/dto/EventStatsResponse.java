package com.turnout.eventservice.dto;

import java.util.UUID;

public record EventStatsResponse(
        UUID eventId,
        long total,
        long confirmed,
        long declined,
        long maybe,
        long pending,
        long waitlisted
) {}
