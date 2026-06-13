package com.turnout.aiservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDateTime;
import java.util.UUID;

public final class AiRequests {

    public record EventDescriptionRequest(
            @NotBlank(message = "Notes are required") String notes
    ) {}

    public record InvitationCopyRequest(
            @NotBlank String title,
            @NotBlank String date,
            @NotBlank String location,
            @NotBlank String description
    ) {}

    public record FollowupRequest(
            @NotBlank String eventTitle,
            @Positive int    daysSinceSent,
            @Positive int    nonResponderCount
    ) {}

    public record RsvpInsightsRequest(
            @NotNull UUID eventId,
            long total,
            long confirmed,
            long declined,
            long pending
    ) {}

    public record SendTimeRequest(
            @NotBlank    String        eventType,
            @Positive    int           audienceSize,
            @NotNull     LocalDateTime eventDate
    ) {}

    public record CapacityForecastRequest(
            @NotBlank String eventType,
            @Positive int    totalInvited,
            @Positive int    daysUntilEvent
    ) {}

    private AiRequests() {}
}
