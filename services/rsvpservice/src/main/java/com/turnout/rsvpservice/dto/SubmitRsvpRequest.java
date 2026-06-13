package com.turnout.rsvpservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SubmitRsvpRequest(
        @NotBlank(message = "Token is required")
        String token,

        @NotNull(message = "RSVP status is required")
        String rsvpStatus   // CONFIRMED / DECLINED / MAYBE
) {}
