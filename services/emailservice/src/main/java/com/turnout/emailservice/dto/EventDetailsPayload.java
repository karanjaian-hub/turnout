package com.turnout.emailservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// Mirrors the fields returned by GET {{gateway_url}}/api/events/{eventId} on eventservice.
// @JsonIgnoreProperties(ignoreUnknown = true) so this stays resilient if eventservice's
// response has extra fields (id, maxCapacity, status, etc.) we don't need here.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class EventDetailsPayload {
    private String title;
    private String description;
    private LocalDateTime eventDate;
    private String location;
}