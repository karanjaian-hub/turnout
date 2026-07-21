package com.turnout.emailservice.dto;

import lombok.Data;

@Data
public class GuestEmailPayload {
    private String id;
    private String email;
    private String fullName;

 // Extracts the first word of fullName for use in greetings ("Hi Abdi,")
    public String getFirstName() {
        if (fullName == null || fullName.isBlank()) return "Guest";
        return fullName.split(" ")[0];
    }
}
