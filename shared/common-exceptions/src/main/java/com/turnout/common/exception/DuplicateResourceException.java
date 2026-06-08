package com.turnout.common.exception;

public class DuplicateResourceException extends TurnoutException {

    public DuplicateResourceException(String resource, String field, Object value) {
        super(resource + " already exists with " + field + ": " + value, "DUPLICATE_RESOURCE");
    }
}
