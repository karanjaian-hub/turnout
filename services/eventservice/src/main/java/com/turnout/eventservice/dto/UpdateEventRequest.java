package com.turnout.eventservice.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Positive;

import java.time.LocalDateTime;

// All fields optional — only non-null values are applied in the service
public record UpdateEventRequest(
        String title,
        String description,
        @Future LocalDateTime eventDate,
        String location,
        @Positive Integer maxCapacity
) {}
