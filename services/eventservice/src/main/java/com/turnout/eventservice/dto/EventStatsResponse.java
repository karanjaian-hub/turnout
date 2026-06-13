package com.turnout.eventservice.dto;

import java.util.UUID;

public record EventStatsResponse(
        UUID eventId,
        long totalInvited,
        long confirmed,
        long declined,
        long maybe,
        long pending,
        long waitlisted,
        double confirmationRate,
        int capacity,
        int currentRsvpCount
) {}
