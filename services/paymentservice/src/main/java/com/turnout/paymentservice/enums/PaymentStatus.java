package com.turnout.paymentservice.enums;

public enum PaymentStatus {
    PENDING,
    COMPLETED,
    FAILED,
    CANCELLED,
    REFUNDED,

    // Legacy alias — seed data uses SUCCESS; mapped to COMPLETED at the DB level.
    // Do not use in new code — always use COMPLETED.
    @Deprecated
    SUCCESS
}
