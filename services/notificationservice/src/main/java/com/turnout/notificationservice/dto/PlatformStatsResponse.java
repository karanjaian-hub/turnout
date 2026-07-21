package com.turnout.notificationservice.dto;


public record PlatformStatsResponse(
        long totalEvents,
        long activeEventsCount,
        long totalOrganizers,
        long totalGuestsInvited,
        long totalConfirmedRsvps,
        long totalRevenueKes
) {}
