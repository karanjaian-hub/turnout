package com.turnout.eventservice.dto;

// Public capacity check for anonymous guests before RSVP submission.
// Field names match exactly what rsvpservice expects to read.
public record CapacityResponse(
        int currentConfirmed,
        int maxCapacity
) {}
