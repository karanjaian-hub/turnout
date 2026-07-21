package com.turnout.emailservice.dto;

import lombok.Data;

@Data
public class UserRegisteredEvent {
    private String email;
    private String firstName;
    private String verificationToken;
}
