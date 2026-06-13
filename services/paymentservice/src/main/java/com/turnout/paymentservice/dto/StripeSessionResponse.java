package com.turnout.paymentservice.dto;

/**
 * Returned to the frontend after a Stripe checkout session is created.
 * The frontend redirects the user to checkoutUrl.
 */
public record StripeSessionResponse(String sessionId, String checkoutUrl) {}
