package com.turnout.guestservice.exception;

import java.util.UUID;

public class GuestNotFoundException extends RuntimeException {

    public GuestNotFoundException(UUID guestId) {
        super("Guest not found: " + guestId);
    }
}
