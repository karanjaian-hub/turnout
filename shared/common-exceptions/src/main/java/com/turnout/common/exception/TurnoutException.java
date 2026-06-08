package com.turnout.common.exception;

import lombok.Getter;

/**
 * Base exception for all Turnout business errors.
 * Every custom exception extends this so controllers can catch one type
 * and map it to the correct HTTP status via a single @ExceptionHandler.
 */
@Getter
public class TurnoutException extends RuntimeException {

    private final String errorCode;

    public TurnoutException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public TurnoutException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
}
