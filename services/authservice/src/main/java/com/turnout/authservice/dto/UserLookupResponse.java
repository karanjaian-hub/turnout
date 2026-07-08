package com.turnout.authservice.dto;

import java.util.UUID;

public record UserLookupResponse(
        UUID id,
        String username,
        String email,
        String fullName
) {}
