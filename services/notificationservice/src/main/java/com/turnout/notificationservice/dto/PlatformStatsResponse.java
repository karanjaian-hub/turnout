package com.turnout.notificationservice.dto;

/**
 * Platform-wide aggregate returned by GET /api/admin/dashboard/stats.
 * Cached in Redis for 30 seconds to avoid hammering other services.
 */
public record PlatformStatsResponse(
        long totalEvents,
        long activeEventsCount,
        long totalOrganizers,
        long totalGuestsInvited,
        long totalConfirmedRsvps,
        long totalRevenueKes
) {}
