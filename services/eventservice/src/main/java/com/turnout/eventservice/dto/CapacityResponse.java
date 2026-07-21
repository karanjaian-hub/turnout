package com.turnout.eventservice.dto;

public record CapacityResponse(
        int currentConfirmed,
        int maxCapacity
) {}
