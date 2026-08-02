package com.turnout.eventservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.turnout.common.enums.EventStatus;
import com.turnout.common.exception.ResourceNotFoundException;
import com.turnout.common.exception.TierLimitExceededException;
import com.turnout.common.exception.UnauthorizedAccessException;
import com.turnout.eventservice.dto.*;
import com.turnout.eventservice.entity.AuditLog;
import com.turnout.eventservice.entity.Event;
import com.turnout.eventservice.repository.AuditLogRepository;
import com.turnout.eventservice.repository.EventRepository;
import com.turnout.eventservice.repository.EventSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventService {

    private static final String ROLE_ADMIN       = "ADMIN";
    private static final String ROLE_SUPER_ADMIN = "SUPER_ADMIN";
    private static final String STATS_CACHE_KEY  = "events:stats:";

    private final EventRepository      eventRepository;
    private final AuditLogRepository   auditLogRepository;
    private final StringRedisTemplate  redisTemplate;
    private final ObjectMapper         objectMapper;
    private final WebClient.Builder    webClientBuilder;

// Create
    @Transactional
    public EventResponse createEvent(CreateEventRequest request, UUID userId, String role) {
        enforceTierLimit(userId);

        Event event = new Event();
        event.setTitle(request.title());
        event.setDescription(request.description());
        event.setEventDate(request.eventDate());
        event.setLocation(request.location());
        event.setMaxCapacity(request.maxCapacity());
        event.setCreatedBy(userId);
        event.setStatus(EventStatus.DRAFT);

        Event saved = eventRepository.save(event);
        writeAuditLog(userId, "CREATED_EVENT", "Event", saved.getId(), saved.getId(), saved);

        return toResponse(saved);
    }

// Read
    @Transactional(readOnly = true)
    public EventResponse getEvent(UUID eventId, UUID userId, String role) {
        Event event = findOrThrow(eventId);
        assertCanAccess(event, userId, role);
        return toResponse(event);
    }

// No auth required — guest clicking an RSVP link in an email isn't logged in.
    @Transactional(readOnly = true)
    public PublicEventResponse getPublicEvent(UUID eventId) {
        Event event = findOrThrow(eventId);
        return new PublicEventResponse(event.getTitle(), event.getEventDate(), event.getLocation());
    }

    @Transactional(readOnly = true)
    public CapacityResponse getCapacity(UUID eventId) {
        Event event = findOrThrow(eventId);
        return new CapacityResponse(event.getCurrentRsvpCount(), event.getMaxCapacity());
    }

    @Transactional(readOnly = true)
    public PagedEventResponse listEvents(
            UUID userId, String role,
            UUID organizerId, EventStatus status,
            LocalDateTime dateFrom, LocalDateTime dateTo,
            int page, int size) {

        // EVENT_ORGANIZER always sees only their own events — organizerId param ignored
        UUID effectiveOrganizerId = isAdmin(role) ? organizerId : userId;

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "eventDate"));
        Specification<Event> spec = EventSpecification.withFilters(
                effectiveOrganizerId, status, dateFrom, dateTo);
        Page<Event> result = eventRepository.findAll(spec, pageable);

        return new PagedEventResponse(
                result.getContent().stream().map(this::toResponse).toList(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.getNumber(),
                result.getSize()
        );
    }

// Update
    @Transactional
    public EventResponse updateEvent(UUID eventId, UpdateEventRequest request, UUID userId, String role) {
        Event event = findOrThrow(eventId);
        assertCanModify(event, userId, role);

        if (request.title()       != null) event.setTitle(request.title());
        if (request.description() != null) event.setDescription(request.description());
        if (request.eventDate()   != null) event.setEventDate(request.eventDate());
        if (request.location()    != null) event.setLocation(request.location());
        if (request.maxCapacity() != null) event.setMaxCapacity(request.maxCapacity());

        Event saved = eventRepository.save(event);
        writeAuditLog(userId, "UPDATED_EVENT", "Event", saved.getId(), saved.getId(), saved);

        return toResponse(saved);
    }

// Delete
    @Transactional
    public void deleteEvent(UUID eventId, UUID userId, String role) {
        Event event = findOrThrow(eventId);
        assertCanModify(event, userId, role);

        writeAuditLog(userId, "DELETED_EVENT", "Event", event.getId(), event.getId(), event);
        eventRepository.delete(event);
        evictStatsCache(eventId);
    }

 //Status change
    @Transactional
    public EventResponse changeStatus(UUID eventId, ChangeStatusRequest request, UUID userId, String role) {
        Event event = eventRepository.findByIdForUpdate(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", eventId));

        assertCanModify(event, userId, role);
        assertValidTransition(event.getStatus(), request.newStatus());

        event.setStatus(request.newStatus());
        Event saved = eventRepository.save(event);

        writeAuditLog(userId, "CHANGED_STATUS", "Event", saved.getId(), saved.getId(),
                Map.of("from", event.getStatus(), "to", request.newStatus()));

        return toResponse(saved);
    }

    // ── Stats (Redis-cached) ─────────────────────────────────────────────────

    public EventStatsResponse getEventStats(UUID eventId, UUID userId, String role) {
        Event event = findOrThrow(eventId);
        assertCanAccess(event, userId, role);

        String cacheKey = STATS_CACHE_KEY + eventId;
        String cached   = redisTemplate.opsForValue().get(cacheKey);

        if (cached != null) {
            return deserializeStats(cached);
        }

        EventStatsResponse stats = webClientBuilder.build()
                .get()
                .uri("http://guestservice:8083/api/guests/event/{eventId}/stats", eventId)
                .retrieve()
                .bodyToMono(EventStatsResponse.class)
                .block();

        if (stats != null) {
            redisTemplate.opsForValue().set(cacheKey, serializeStats(stats), Duration.ofSeconds(10));
        }

        return stats;
    }

    // ── Package-visible: called by Kafka consumer ────────────────────────────

    public void incrementRsvpCount(UUID eventId) {
        eventRepository.findById(eventId).ifPresent(event -> {
            event.setCurrentRsvpCount(event.getCurrentRsvpCount() + 1);
            eventRepository.save(event);
            evictStatsCache(eventId);
        });
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private Event findOrThrow(UUID eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", eventId));
    }

    private void assertCanAccess(Event event, UUID userId, String role) {
        if (isAdmin(role)) return;
        if (!event.getCreatedBy().equals(userId)) {
            throw new UnauthorizedAccessException("Access denied to event: " + event.getId());
        }
    }

    private void assertCanModify(Event event, UUID userId, String role) {
        assertCanAccess(event, userId, role);
        EventStatus status = event.getStatus();
        if (status == EventStatus.COMPLETED || status == EventStatus.CANCELLED) {
            throw new IllegalStateException("Cannot modify a " + status + " event");
        }
    }

    private void assertValidTransition(EventStatus current, EventStatus next) {
        boolean valid = switch (current) {
            case DRAFT  -> next == EventStatus.ACTIVE;
            case ACTIVE -> next == EventStatus.COMPLETED || next == EventStatus.CANCELLED;
            default     -> false;
        };
        if (!valid) {
            throw new IllegalStateException("Invalid status transition: " + current + " → " + next);
        }
    }

    private void enforceTierLimit(UUID userId) {
        try {
            Map<?, ?> limits = webClientBuilder.build()
                    .get()
                    .uri("http://paymentservice:8087/api/payments/tier-check/{userId}", userId)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (limits == null) return;
            Object maxEvents = limits.get("maxEvents");
            if (maxEvents == null) return;
            int max = Integer.parseInt(maxEvents.toString());
            if (max == -1) return;

            long current = eventRepository.countByCreatedBy(userId);
            if (current >= max) {
                throw new TierLimitExceededException("event_creation", "FREE");
            }
        } catch (TierLimitExceededException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Could not reach payment-service for tier check: {}", e.getMessage());
        }
    }

    private boolean isAdmin(String role) {
        return ROLE_ADMIN.equals(role) || ROLE_SUPER_ADMIN.equals(role);
    }

    private void evictStatsCache(UUID eventId) {
        redisTemplate.delete(STATS_CACHE_KEY + eventId);
    }

    private void writeAuditLog(UUID userId, String action, String entityType,
                                UUID entityId, UUID eventId, Object details) {
        AuditLog auditLog = new AuditLog();
        auditLog.setEventId(eventId);
        auditLog.setUserId(userId);
        auditLog.setAction(action);
        auditLog.setEntityType(entityType);
        auditLog.setEntityId(entityId);
        auditLog.setDetails(serializeDetails(details));
        auditLogRepository.save(auditLog);
    }

    private EventResponse toResponse(Event e) {
        return new EventResponse(
                e.getId(), e.getTitle(), e.getDescription(), e.getEventDate(),
                e.getLocation(), e.getMaxCapacity(), e.getCurrentRsvpCount(),
                e.getCreatedBy(), e.getStatus(), e.getCreatedAt(), e.getUpdatedAt()
        );
    }

    private String serializeDetails(Object obj) {
        try { return objectMapper.writeValueAsString(obj); }
        catch (Exception e) { return obj.toString(); }
    }

    private String serializeStats(EventStatsResponse stats) {
        try { return objectMapper.writeValueAsString(stats); }
        catch (Exception e) { return "{}"; }
    }

    private EventStatsResponse deserializeStats(String json) {
        try { return objectMapper.readValue(json, EventStatsResponse.class); }
        catch (Exception e) { return null; }
    }
}
