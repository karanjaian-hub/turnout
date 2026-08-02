package com.turnout.guestservice.dto;

import com.turnout.common.enums.RsvpStatus;
import jakarta.validation.constraints.NotNull;

public record RsvpUpdateRequest(
        @NotNull RsvpStatus rsvpStatus,
        boolean tokenUsed
) {}
