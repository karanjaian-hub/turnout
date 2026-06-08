package com.turnout.common.exception;

public class UnauthorizedAccessException extends TurnoutException {

    public UnauthorizedAccessException(String message) {
        super(message, "UNAUTHORIZED_ACCESS");
    }
}
