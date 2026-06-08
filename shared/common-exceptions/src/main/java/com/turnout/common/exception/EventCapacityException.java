package com.turnout.common.exception;

public class EventCapacityException extends TurnoutException {

    public EventCapacityException(String eventId) {
        super("Event " + eventId + " has reached maximum capacity", "EVENT_CAPACITY_EXCEEDED");
    }
}
