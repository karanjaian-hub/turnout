package com.turnout.rsvpservice.service;

import com.turnout.rsvpservice.dto.RsvpResponse;
import com.turnout.rsvpservice.dto.SubmitRsvpRequest;
import com.turnout.rsvpservice.dto.ValidateTokenResponse;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class RsvpService {

    private final StringRedisTemplate redisTemplate;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    // RestTemplate is in spring-web — no extra dependency needed
    private final RestTemplate restTemplate;

    @Value("${turnout.jwt.secret}")
    private String jwtSecret;

    @Value("${turnout.rsvp.lock-ttl-seconds}")
    private long lockTtlSeconds;

    @Value("${turnout.services.event-service-url}")
    private String eventServiceUrl;

    private static final String TOPIC_RSVP_SUBMITTED = "rsvp-submitted";

    // PUBLIC API

    public ValidateTokenResponse validateToken(String token) {
        Claims claims = decodeToken(token);

        UUID guestId  = UUID.fromString(claims.get("guestId", String.class));
        UUID eventId  = UUID.fromString(claims.get("eventId", String.class));
        boolean tokenUsed = Boolean.TRUE.equals(claims.get("tokenUsed", Boolean.class));

        EventDetails eventDetails = fetchEventDetails(eventId);
        String currentStatus = tokenUsed ? fetchCurrentRsvpStatus(guestId) : null;

        return new ValidateTokenResponse(
                true,
                guestId,
                eventId,
                claims.get("guestName", String.class),
                eventDetails.title(),
                eventDetails.eventDate(),
                eventDetails.location(),
                tokenUsed,
                currentStatus
        );
    }

    public RsvpResponse submitRsvp(SubmitRsvpRequest request) {
        ValidateTokenResponse tokenData = validateToken(request.token());

        if (tokenData.alreadyResponded()) {
            return new RsvpResponse(
                    false,
                    "You have already responded to this invitation.",
                    false,
                    tokenData.guestName(),
                    tokenData.eventTitle(),
                    tokenData.eventLocation(),
                    tokenData.eventDate()
            );
        }

        UUID eventId = tokenData.eventId();
        UUID guestId = tokenData.guestId();
        String lockKey = "rsvp:lock:" + eventId;

        // ─── CRITICAL CONCURRENCY SECTION ─────────────────────────────
        Boolean lockAcquired = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, "1", lockTtlSeconds, TimeUnit.SECONDS);

        if (lockAcquired == null || !lockAcquired) {
            throw new LockNotAcquiredException("Event is busy, please retry in a moment.");
        }

        try {
            String finalStatus = resolveRsvpStatus(request.rsvpStatus(), eventId);

            persistRsvp(guestId, finalStatus);
            publishRsvpSubmittedEvent(guestId, eventId, tokenData.guestName(), finalStatus);

            boolean wasWaitlisted = "WAITLISTED".equals(finalStatus)
                    && "CONFIRMED".equals(request.rsvpStatus());

            String message = wasWaitlisted
                    ? "The event is at capacity. You've been added to the waitlist."
                    : "Your RSVP has been recorded. Thank you!";

            log.info("RSVP submitted: guest={} event={} status={}", guestId, eventId, finalStatus);

            return new RsvpResponse(true, message, wasWaitlisted,
                    tokenData.guestName(), tokenData.eventTitle(),
                    tokenData.eventLocation(), tokenData.eventDate());

        } finally {
            // ALWAYS release — even on exception. That's the point of finally.
            redisTemplate.delete(lockKey);
        }

    }

    // PRIVATE HELPERS
     private Claims decodeToken(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException e) {
            log.warn("Invalid RSVP token: {}", e.getMessage());
            throw new InvalidTokenException("Token is invalid or has expired.");
        }
    }

    private String resolveRsvpStatus(String requestedStatus, UUID eventId) {
        if (!"CONFIRMED".equals(requestedStatus)) {
            return requestedStatus;
        }
        EventCapacity capacity = fetchEventCapacity(eventId);
        if (capacity.currentConfirmed() >= capacity.maxCapacity()) {
            log.info("Event {} full ({}/{}), waitlisting guest",
                    eventId, capacity.currentConfirmed(), capacity.maxCapacity());
            return "WAITLISTED";
        }
        return "CONFIRMED";
    }

    private void persistRsvp(UUID guestId, String status) {
        // TODO: Inject GuestRepository and update:
        //   guest.setRsvpStatus(RsvpStatus.valueOf(status));
        //   guest.setTokenUsed(true);
        //   guest.setRsvpDate(LocalDateTime.now());
        //   guestRepository.save(guest);
        log.debug("Persisting RSVP for guest={} status={}", guestId, status);
    }

    private void publishRsvpSubmittedEvent(UUID guestId, UUID eventId,
                                            String guestName, String finalStatus) {
        // TODO: Replace Map with RsvpSubmittedEvent from common-dto
        Map<String, Object> event = Map.of(
                "guestId",    guestId.toString(),
                "eventId",    eventId.toString(),
                "guestName",  guestName,
                "rsvpStatus", finalStatus,
                "timestamp",  LocalDateTime.now().toString()
        );
        kafkaTemplate.send(TOPIC_RSVP_SUBMITTED, eventId.toString(), event);
    }

    @SuppressWarnings("unchecked")
    private EventDetails fetchEventDetails(UUID eventId) {
        try {
            Map<String, Object> response = restTemplate.getForObject(
                    eventServiceUrl + "/api/events/{id}/public",
                    Map.class,
                    eventId
            );
            if (response == null) throw new ResourceNotFoundException("Event not found: " + eventId);

            return new EventDetails(
                    (String) response.get("title"),
                    LocalDateTime.parse((String) response.get("eventDate")),
                    (String) response.get("location")
            );
        } catch (RestClientException e) {
            log.error("Failed to fetch event details for {}: {}", eventId, e.getMessage());
            throw new ResourceNotFoundException("Could not load event details.");
        }
    }

    @SuppressWarnings("unchecked")
    private EventCapacity fetchEventCapacity(UUID eventId) {
        try {
            Map<String, Object> response = restTemplate.getForObject(
                    eventServiceUrl + "/api/events/{id}/capacity",
                    Map.class,
                    eventId
            );
            if (response == null) throw new ResourceNotFoundException("Event not found: " + eventId);

            return new EventCapacity(
                    ((Number) response.get("currentConfirmed")).intValue(),
                    ((Number) response.get("maxCapacity")).intValue()
            );
        } catch (RestClientException e) {
            log.error("Failed to fetch capacity for event {}: {}", eventId, e.getMessage());
            throw new ResourceNotFoundException("Could not check event capacity.");
        }
    }

    private String fetchCurrentRsvpStatus(UUID guestId) {
        // TODO: guestRepository.findById(guestId).map(g -> g.getRsvpStatus().name()).orElse(null)
        return "UNKNOWN";
    }

    // INNER RECORDS
    private record EventDetails(String title, LocalDateTime eventDate, String location) {}
    private record EventCapacity(int currentConfirmed, int maxCapacity) {}


    // INLINE EXCEPTIONS
       public static class InvalidTokenException extends RuntimeException {
        public InvalidTokenException(String message) { super(message); }
    }

    public static class LockNotAcquiredException extends RuntimeException {
        public LockNotAcquiredException(String message) { super(message); }
    }

    public static class ResourceNotFoundException extends RuntimeException {
        public ResourceNotFoundException(String message) { super(message); }
    }
}
