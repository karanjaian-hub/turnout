package com.turnout.paymentservice.dto;

/**
 * Returned to the controller after a successful STK Push initiation.
 * checkoutRequestId is Safaricom's reference — we store it so we can
 * match it when the callback arrives.
 */
public record StkPushResponse(String checkoutRequestId, String message) {}
