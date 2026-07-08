package com.turnout.paymentservice.dto;

import com.turnout.paymentservice.enums.PaymentProvider;
import com.turnout.paymentservice.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Enriched transaction response — adds username/email resolved from auth-service.
 * The raw PaymentTransaction entity only has userId; this DTO adds the human-readable fields
 * the admin panel needs to display on the Payments page.
 */
public record PaymentTransactionResponse(
        UUID            id,
        UUID            userId,
        String          username,
        String          email,
        UUID            planId,
        PaymentProvider provider,
        BigDecimal      amount,
        String          currency,
        PaymentStatus   status,
        LocalDateTime   createdAt
) {}
