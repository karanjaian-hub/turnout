package com.turnout.eventservice.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDateTime;

public record CreateEventRequest(
        @NotBlank String title,
        String description,
        @NotNull @Future LocalDateTime eventDate,
        @NotBlank String location,
        @Positive int maxCapacity
) {}
