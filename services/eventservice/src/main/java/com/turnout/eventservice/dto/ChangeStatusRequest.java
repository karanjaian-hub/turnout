package com.turnout.eventservice.dto;

import com.turnout.common.enums.EventStatus;
import jakarta.validation.constraints.NotNull;

public record ChangeStatusRequest(
        @NotNull EventStatus newStatus
) {}
