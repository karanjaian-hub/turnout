package com.turnout.guestservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UpdateGuestRequest(
        @NotBlank String fullName,
        @NotBlank @Email String email
) {}
