package com.turnout.eventservice.dto;

import com.turnout.common.enums.EventStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record EventResponse(
        UUID id,
        String title,
        String description,
        LocalDateTime eventDate,
        String location,
        int maxCapacity,
        int currentRsvpCount,
        UUID createdBy,
        EventStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
