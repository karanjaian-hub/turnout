package com.turnout.paymentservice.dto;

import com.turnout.paymentservice.enums.UpgradeRequestStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record UpgradeRequestResponse(
        UUID                  id,
        UUID                  userId,
        String                username,
        String                email,
        String                requestedPlan,
        UpgradeRequestStatus  status,
        String                adminNotes,
        LocalDateTime         createdAt
) {}
