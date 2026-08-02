package com.turnout.emailservice.controller;

import com.turnout.emailservice.dto.EmailProgressResponse;
import com.turnout.emailservice.dto.EventDetailsPayload;
import com.turnout.emailservice.dto.GuestEmailPayload;
import com.turnout.emailservice.dto.PagedGuestResponse;
import com.turnout.emailservice.entity.EmailLog;
import com.turnout.emailservice.repository.EmailLogRepository;
import com.turnout.emailservice.service.EmailDispatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/emails")
@RequiredArgsConstructor
@Slf4j
public class EmailController {

    private final EmailLogRepository emailLogRepository;
    private final EmailDispatchService emailDispatchService;
    private final WebClient webClient;
    private final JdbcTemplate jdbcTemplate;

    @PostMapping("/send-invitations")
    public ResponseEntity<Map<String, Object>> sendInvitations(@RequestBody Map<String, String> body) {
        String eventIdStr = body.get("eventId");
        if (eventIdStr == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "eventId is required"));
        }

        UUID eventId;
        try {
            eventId = UUID.fromString(eventIdStr);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "eventId must be a valid UUID"));
        }

        EventDetailsPayload eventDetails = fetchEventDetails(eventId);
        List<GuestEmailPayload> guests = fetchGuestsForEvent(eventId);

        if (guests.isEmpty()) {
            return ResponseEntity.ok(Map.of("message", "No guests found for event", "queued", 0));
        }

        int queued = 0;
        for (GuestEmailPayload guest : guests) {
            try {
                EmailLog queuedLog = EmailLog.builder()
                        .eventId(eventId)
                        .guestId(guest.getId())
                        .eventType("MANUAL_INVITATION")
                        .recipientEmail(guest.getEmail())
                        .recipientName(guest.getFullName())
                        .subject("You're invited!")
                        .status("QUEUED")
                        .attemptedAt(LocalDateTime.now())
                        .build();
                emailLogRepository.save(queuedLog);

                String guestToken = fetchGuestToken(guest.getId());
                emailDispatchService.sendInvitationEmail(
                        guest.getEmail(),
                        guest.getFirstName(),
                        eventId,
                        guest.getId(),
                        guestToken,
                        eventDetails
                );
                queued++;
            } catch (Exception e) {
                log.error("Failed to queue invitation for guest {}: {}", guest.getId(), e.getMessage());
            }
        }

        return ResponseEntity.ok(Map.of(
                "message", "Invitations queued",
                "queued", queued,
                "total", guests.size()
        ));
    }

    @PostMapping("/resend/{guestId}")
    public ResponseEntity<Map<String, String>> resend(@PathVariable String guestId) {
        List<EmailLog> previousLogs = emailLogRepository.findByGuestIdOrderByAttemptedAtDesc(guestId);
        if (previousLogs.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        EmailLog latest = previousLogs.get(0);

        EmailLog resendLog = EmailLog.builder()
                .eventId(latest.getEventId())
                .guestId(guestId)
                .eventType("MANUAL_INVITATION")
                .recipientEmail(latest.getRecipientEmail())
                .recipientName(latest.getRecipientName())
                .subject(latest.getSubject())
                .status("QUEUED")
                .attemptedAt(LocalDateTime.now())
                .build();
        emailLogRepository.save(resendLog);

        EventDetailsPayload eventDetails = fetchEventDetails(latest.getEventId());

        String guestToken = fetchGuestToken(guestId);
        emailDispatchService.sendInvitationEmail(
                latest.getRecipientEmail(),
                latest.getRecipientName(),
                latest.getEventId(),
                guestId,
                guestToken,
                eventDetails
        );

        return ResponseEntity.ok(Map.of("message", "Re-send triggered for guest " + guestId));
    }

    @GetMapping("/logs/event/{eventId}")
    public ResponseEntity<Page<EmailLog>> logsByEvent(
            @PathVariable UUID eventId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(
                emailLogRepository.findByEventIdOrderByAttemptedAtDesc(eventId, pageable));
    }

    @GetMapping("/logs/guest/{guestId}")
    public ResponseEntity<List<EmailLog>> logsByGuest(@PathVariable String guestId) {
        return ResponseEntity.ok(
                emailLogRepository.findByGuestIdOrderByAttemptedAtDesc(guestId));
    }

    @GetMapping("/progress/{eventId}")
    public ResponseEntity<EmailProgressResponse> progress(@PathVariable UUID eventId) {
        List<Object[]> rows = emailLogRepository.countByStatusForEvent(eventId);

        Map<String, Long> counts = rows.stream()
                .collect(Collectors.toMap(
                        r -> (String) r[0],
                        r -> (Long) r[1]
                ));

        long queued = counts.getOrDefault("QUEUED", 0L);
        long sent   = counts.getOrDefault("SUCCESS", 0L);
        long failed = counts.getOrDefault("FAILED", 0L);

        return ResponseEntity.ok(EmailProgressResponse.builder()
                .eventId(eventId)
                .queued(queued)
                .sent(sent)
                .failed(failed)
                .total(queued + sent + failed)
                .build());
    }

    private EventDetailsPayload fetchEventDetails(UUID eventId) {
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT id, title, description, location, event_date FROM events.events WHERE id = ?::uuid",
                    (rs, rowNum) -> {
                        EventDetailsPayload e = new EventDetailsPayload();
                        e.setId(rs.getString("id"));
                        e.setTitle(rs.getString("title"));
                        e.setDescription(rs.getString("description"));
                        e.setLocation(rs.getString("location"));
                        e.setEventDate(rs.getTimestamp("event_date").toLocalDateTime());
                        return e;
                    },
                    eventId.toString()
            );
        } catch (Exception e) {
            log.warn("Could not fetch event details for {} from DB: {}", eventId, e.getMessage());
            return null;
        }
    }

    private String fetchGuestToken(String guestId) {
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT token FROM guests.guests WHERE id = ?::uuid",
                    String.class,
                    guestId
            );
        } catch (Exception e) {
            log.warn("Could not fetch token for guest {}: {}", guestId, e.getMessage());
            return null;
        }
    }

    private List<GuestEmailPayload> fetchGuestsForEvent(UUID eventId) {
        try {
            PagedGuestResponse guests = webClient.get()
                    .uri("http://guestservice:8083/api/guests/event/" + eventId + "?page=0&size=200")
                    .retrieve()
                    .bodyToMono(PagedGuestResponse.class)
                    .block();
            return guests != null && guests.getContent() != null ? guests.getContent() : List.of();
        } catch (Exception e) {
            log.error("Failed to fetch guests for event {}: {}", eventId, e.getMessage());
            return List.of();
        }
    }
}
