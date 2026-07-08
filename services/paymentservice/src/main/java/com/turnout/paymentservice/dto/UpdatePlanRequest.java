package com.turnout.paymentservice.dto;

import java.math.BigDecimal;

/**
 * All fields optional — only non-null fields are applied to the plan.
 * This avoids overwriting fields the caller didn't intend to change.
 */
public record UpdatePlanRequest(
        Integer    maxEvents,
        Integer    maxGuestsPerEvent,
        BigDecimal monthlyPriceKes,
        BigDecimal monthlyPriceUsd,
        Boolean    active
) {}
