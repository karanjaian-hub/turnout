package com.turnout.common.exception;

public class ResourceNotFoundException extends TurnoutException {

    public ResourceNotFoundException(String resource, Object id) {
        super(resource + " not found with id: " + id, "RESOURCE_NOT_FOUND");
    }
}
