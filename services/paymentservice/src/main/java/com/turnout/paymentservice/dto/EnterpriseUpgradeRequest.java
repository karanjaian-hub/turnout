package com.turnout.paymentservice.dto;

import jakarta.validation.constraints.NotBlank;

public record EnterpriseUpgradeRequest(

        @NotBlank(message = "Requested plan is required")
        String requestedPlan,

        // Optional — user can explain their use case
        String notes
) {}
