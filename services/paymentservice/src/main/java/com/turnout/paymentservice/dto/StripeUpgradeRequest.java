package com.turnout.paymentservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record StripeUpgradeRequest(

        @NotNull(message = "Plan ID is required")
        UUID planId,

        @NotBlank(message = "Success URL is required")
        String successUrl,

        @NotBlank(message = "Cancel URL is required")
        String cancelUrl
) {}
