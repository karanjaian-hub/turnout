package com.turnout.aiservice.dto;

public final class AiResponses {

    public record EventDescriptionResponse(
            String description,
            String tagline,
            String invitationCopy
    ) {}

    public record InvitationCopyResponse(
            String subject,
            String body,
            String callToAction
    ) {}

    public record EmailVariant(String subject, String body) {}

    public record FollowupResponse(
            EmailVariant gentle,
            EmailVariant urgent,
            EmailVariant personal
    ) {}

    public record RsvpInsightsResponse(
            String summary,
            String responseRateAssessment,
            String topInsight,
            String recommendation
    ) {}

    public record SendTimeResponse(
            String recommendedDay,
            String recommendedTime,
            String reasoning,
            String alternativeSlot
    ) {}

    public record CapacityForecastResponse(
            double expectedConfirmationRate,
            int    expectedConfirmed,
            int    lowEstimate,
            int    highEstimate,
            String confidence,
            String reasoning
    ) {}

    public record ErrorResponse(String error) {}

    private AiResponses() {}
}
