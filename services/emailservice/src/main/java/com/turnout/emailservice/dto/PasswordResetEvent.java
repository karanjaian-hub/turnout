package com.turnout.emailservice.dto;

import lombok.Data;

// Payload published to "user.password-reset" topic by authservice when a reset is requested.
@Data
public class PasswordResetEvent {
    private String email;
    private String firstName;
    private String resetToken;
}
