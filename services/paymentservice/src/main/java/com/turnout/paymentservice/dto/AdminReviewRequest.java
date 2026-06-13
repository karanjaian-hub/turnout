package com.turnout.paymentservice.dto;

public record AdminReviewRequest(
        // Optional admin notes when approving or rejecting
        String adminNotes
) {}
