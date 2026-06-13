package com.turnout.eventservice.controller;

import com.turnout.eventservice.dto.*;
import com.turnout.eventservice.service.EventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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
    public ResponseEntity<List<EventResponse>> listEvents(
            @RequestHeader("X-User-Id")   String userId,
            @RequestHeader("X-User-Role") String role) {

        return ResponseEntity.ok(eventService.listEvents(UUID.fromString(userId), role));
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
