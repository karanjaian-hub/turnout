package com.turnout.paymentservice.dto;

/**
 * Consumed by event-service via GET /api/payments/tier-check/{userId}
 * to decide whether to allow event creation or guest additions.
 */
public record TierLimitsResponse(
        String planName,
        int maxEvents,           // -1 = unlimited
        int maxGuestsPerEvent,   // -1 = unlimited
        int currentEventCount
) {}
