package com.turnout.emailservice.dto;

import lombok.Data;

import java.util.List;

// Matches the paginated wrapper Spring Data returns from guestservice.
// We only care about "content" — the rest (pageable, totalPages, etc.) we discard.
@Data
public class PagedGuestResponse {
    private List<GuestEmailPayload> content;
}
