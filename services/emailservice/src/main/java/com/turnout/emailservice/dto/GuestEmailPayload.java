package com.turnout.emailservice.dto;

import lombok.Data;

// Shape of guest data returned by guestservice when we fetch by eventId.
// Field names must match guestservice's GuestResponse JSON exactly.
@Data
public class GuestEmailPayload {
    private String id;
    private String email;
    private String firstName;
    private String lastName;
}
