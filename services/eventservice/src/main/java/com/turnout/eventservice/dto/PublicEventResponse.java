package com.turnout.eventservice.dto;

import java.time.LocalDateTime;

// Guest-facing subset of EventResponse — no organizer, audit, or internal stats fields.
// Field names must match exactly what rsvpservice.fetchEventDetails reads:
// response.get("title"), response.get("eventDate"), response.get("location")
public record PublicEventResponse(
        String title,
        LocalDateTime eventDate,
        String location
) {}
