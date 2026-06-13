package com.turnout.emailservice.dto;

import lombok.Data;

// Payload published to "user.registered" topic by authservice after a new user signs up.
// Field names must exactly match what authservice serializes — do not rename without
// updating the producer side.
@Data
public class UserRegisteredEvent {
    private String email;
    private String firstName;
    private String verificationToken;
}
