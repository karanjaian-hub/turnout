package com.turnout.paymentservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.UUID;

public record MpesaUpgradeRequest(

        @NotNull(message = "Plan ID is required")
        UUID planId,

        // Safaricom accepts 254XXXXXXXXX format — enforce at the boundary
        @NotBlank(message = "Phone number is required")
        @Pattern(regexp = "^254[17]\\d{8}$",
                 message = "Phone number must be in format 254XXXXXXXXX")
        String phoneNumber,

        @NotBlank(message = "Account reference is required")
        String accountRef
) {}
