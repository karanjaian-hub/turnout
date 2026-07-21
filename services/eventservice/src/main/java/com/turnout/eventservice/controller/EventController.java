package com.turnout.eventservice.controller;

import com.turnout.eventservice.dto.*;
import com.turnout.common.enums.EventStatus;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.RequestParam;
import java.time.LocalDateTime;
import com.turnout.eventservice.service.EventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    @PostMapping
    public ResponseEntity<EventResponse> createEvent(
            @Valid @RequestBody CreateEventRequest request,
            @RequestHeader("X-User-Id")   String userId,
            @RequestHeader("X-User-Role") String role) {

        EventResponse response = eventService.createEvent(request, UUID.fromString(userId), role);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<PagedEventResponse> listEvents(
            @RequestHeader("X-User-Id")   String userId,
            @RequestHeader("X-User-Role") String role,
            @RequestParam(required = false) UUID organizerId,
            @RequestParam(required = false) EventStatus status,
            @RequestParam(required = false)
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFrom,
            @RequestParam(required = false)
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateTo,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        return ResponseEntity.ok(eventService.listEvents(
                UUID.fromString(userId), role,
                organizerId, status, dateFrom, dateTo,
                page, size));
    }

// No auth headers required,,coz,, public RSVP landing page for guests.
// Must be declared before /{id} so Spring doesn't try to parse "public" as a UUID.
    @GetMapping("/{id}/public")
    public ResponseEntity<PublicEventResponse> getPublicEvent(@PathVariable UUID id) {
        return ResponseEntity.ok(eventService.getPublicEvent(id));
    }

// No auth headers required — same anonymous guest flow as /public.
    @GetMapping("/{id}/capacity")
    public ResponseEntity<CapacityResponse> getCapacity(@PathVariable UUID id) {
        return ResponseEntity.ok(eventService.getCapacity(id));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventResponse> getEvent(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id")   String userId,
            @RequestHeader("X-User-Role") String role) {

        return ResponseEntity.ok(eventService.getEvent(id, UUID.fromString(userId), role));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EventResponse> updateEvent(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateEventRequest request,
            @RequestHeader("X-User-Id")   String userId,
            @RequestHeader("X-User-Role") String role) {

        return ResponseEntity.ok(eventService.updateEvent(id, request, UUID.fromString(userId), role));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEvent(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id")   String userId,
            @RequestHeader("X-User-Role") String role) {

        eventService.deleteEvent(id, UUID.fromString(userId), role);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<EventResponse> changeStatus(
            @PathVariable UUID id,
            @Valid @RequestBody ChangeStatusRequest request,
            @RequestHeader("X-User-Id")   String userId,
            @RequestHeader("X-User-Role") String role) {

        return ResponseEntity.ok(eventService.changeStatus(id, request, UUID.fromString(userId), role));
    }

    @GetMapping("/{id}/stats")
    public ResponseEntity<EventStatsResponse> getEventStats(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id")   String userId,
            @RequestHeader("X-User-Role") String role) {

        return ResponseEntity.ok(eventService.getEventStats(id, UUID.fromString(userId), role));
    }
}
