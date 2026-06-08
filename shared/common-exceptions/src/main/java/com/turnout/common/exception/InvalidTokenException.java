package com.turnout.common.exception;

public class InvalidTokenException extends TurnoutException {

    public InvalidTokenException(String reason) {
        super("Invalid token: " + reason, "INVALID_TOKEN");
    }
}
