package com.turnout.eventservice.controller;

import com.turnout.common.dto.ApiResponse;
import com.turnout.common.enums.EventStatus;
import com.turnout.eventservice.dto.*;
import com.turnout.eventservice.service.EventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    @PostMapping
    public ResponseEntity<ApiResponse<EventResponse>> createEvent(
            @Valid @RequestBody CreateEventRequest request,
            @RequestHeader("X-User-Id")   String userId,
            @RequestHeader("X-User-Role") String role) {

        EventResponse data = eventService.createEvent(request, UUID.fromString(userId), role);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Event created.", data));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PagedEventResponse>> listEvents(
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

        PagedEventResponse data = eventService.listEvents(
                UUID.fromString(userId), role,
                organizerId, status, dateFrom, dateTo,
                page, size);
        return ResponseEntity.ok(ApiResponse.success("Events retrieved.", data));
    }

    // No auth headers — public RSVP landing page for guests.
    // Declared before /{id} so Spring doesn't try to parse "public" as a UUID.
    @GetMapping("/{id}/public")
    public ResponseEntity<ApiResponse<PublicEventResponse>> getPublicEvent(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("Event retrieved.", eventService.getPublicEvent(id)));
    }

    // No auth headers — same anonymous guest flow as /public.
    @GetMapping("/{id}/capacity")
    public ResponseEntity<ApiResponse<CapacityResponse>> getCapacity(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("Capacity retrieved.", eventService.getCapacity(id)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EventResponse>> getEvent(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id")   String userId,
            @RequestHeader("X-User-Role") String role) {

        return ResponseEntity.ok(ApiResponse.success("Event retrieved.",
                eventService.getEvent(id, UUID.fromString(userId), role)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<EventResponse>> updateEvent(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateEventRequest request,
            @RequestHeader("X-User-Id")   String userId,
            @RequestHeader("X-User-Role") String role) {

        return ResponseEntity.ok(ApiResponse.success("Event updated.",
                eventService.updateEvent(id, request, UUID.fromString(userId), role)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteEvent(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id")   String userId,
            @RequestHeader("X-User-Role") String role) {

        eventService.deleteEvent(id, UUID.fromString(userId), role);
        return ResponseEntity.status(HttpStatus.NO_CONTENT)
                .body(ApiResponse.success("Event deleted."));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<EventResponse>> changeStatus(
            @PathVariable UUID id,
            @Valid @RequestBody ChangeStatusRequest request,
            @RequestHeader("X-User-Id")   String userId,
            @RequestHeader("X-User-Role") String role) {

        return ResponseEntity.ok(ApiResponse.success("Event status updated.",
                eventService.changeStatus(id, request, UUID.fromString(userId), role)));
    }

    @GetMapping("/{id}/stats")
    public ResponseEntity<ApiResponse<EventStatsResponse>> getEventStats(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id")   String userId,
            @RequestHeader("X-User-Role") String role) {

        return ResponseEntity.ok(ApiResponse.success("Event stats retrieved.",
                eventService.getEventStats(id, UUID.fromString(userId), role)));
    }
}
