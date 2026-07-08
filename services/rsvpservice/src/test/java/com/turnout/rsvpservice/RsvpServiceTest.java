package com.turnout.rsvpservice;

import com.turnout.rsvpservice.dto.RsvpResponse;
import com.turnout.rsvpservice.dto.SubmitRsvpRequest;
import com.turnout.rsvpservice.dto.ValidateTokenResponse;
import com.turnout.rsvpservice.service.RsvpService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RsvpServiceTest {

    @Mock private StringRedisTemplate redisTemplate;
    @Mock private KafkaTemplate<String, Object> kafkaTemplate;
    @Mock private RestTemplate restTemplate;
    @Mock private ValueOperations<String, String> valueOperations;

    @InjectMocks private RsvpService rsvpService;

    // ── Shared test data ─────────────────────────────────────────────────────

    // Must be at least 32 chars for HMAC-SHA256
    private static final String JWT_SECRET  = "turnout-test-secret-key-32chars!!";
    private static final UUID   GUEST_ID    = UUID.randomUUID();
    private static final UUID   EVENT_ID    = UUID.randomUUID();
    private static final String GUEST_NAME  = "Ian Karanja";
    private static final String EVENT_TITLE = "TechFest Nairobi 2026";
    private static final String LOCATION    = "KICC, Nairobi";
    private static final LocalDateTime EVENT_DATE = LocalDateTime.of(2026, 9, 15, 10, 0);

    // The event service URL injected via @Value
    private static final String EVENT_SERVICE_URL = "http://eventservice:8082";

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        ReflectionTestUtils.setField(rsvpService, "jwtSecret",        JWT_SECRET);
        ReflectionTestUtils.setField(rsvpService, "lockTtlSeconds",   5L);
        ReflectionTestUtils.setField(rsvpService, "eventServiceUrl",  EVENT_SERVICE_URL);
    }

    // ════════════════════════════════════════════════════════════════════════
    // TOKEN VALIDATION
    // ════════════════════════════════════════════════════════════════════════

    @Test
    void validateToken_validFreshToken_returnsResponseWithAlreadyRespondedFalse() {
        String token = buildToken(GUEST_ID, EVENT_ID, false);
        stubEventDetailsCall();

        ValidateTokenResponse response = rsvpService.validateToken(token);

        assertThat(response.valid()).isTrue();
        assertThat(response.guestId()).isEqualTo(GUEST_ID);
        assertThat(response.eventId()).isEqualTo(EVENT_ID);
        assertThat(response.guestName()).isEqualTo(GUEST_NAME);
        assertThat(response.alreadyResponded()).isFalse();
        assertThat(response.currentRsvpStatus()).isNull();
    }

    @Test
    void validateToken_alreadyUsedToken_returnsCurrentStatus() {
        // tokenUsed=true means the guest already submitted an RSVP
        String token = buildToken(GUEST_ID, EVENT_ID, true);
        stubEventDetailsCall();

        ValidateTokenResponse response = rsvpService.validateToken(token);

        assertThat(response.alreadyResponded()).isTrue();
        // fetchCurrentRsvpStatus currently returns "UNKNOWN" (TODO stub in service)
        assertThat(response.currentRsvpStatus()).isEqualTo("UNKNOWN");
    }

    @Test
    void validateToken_expiredToken_throwsInvalidTokenException() {
        // Build a token that expired 1 hour ago
        String expiredToken = buildExpiredToken(GUEST_ID, EVENT_ID);

        assertThatThrownBy(() -> rsvpService.validateToken(expiredToken))
            .isInstanceOf(RsvpService.InvalidTokenException.class)
            .hasMessageContaining("invalid or has expired");
    }

    @Test
    void validateToken_tamperedToken_throwsInvalidTokenException() {
        String token = buildToken(GUEST_ID, EVENT_ID, false) + "tampered";

        assertThatThrownBy(() -> rsvpService.validateToken(token))
            .isInstanceOf(RsvpService.InvalidTokenException.class);
    }

    // ════════════════════════════════════════════════════════════════════════
    // RSVP SUBMISSION — HAPPY PATHS
    // ════════════════════════════════════════════════════════════════════════

    @Test
    void submitRsvp_confirmed_eventHasCapacity_returnsSuccess() {
        String token = buildToken(GUEST_ID, EVENT_ID, false);
        stubEventDetailsCall();
        stubCapacityCall(50, 100); // 50 confirmed out of 100 max — plenty of room
        stubLockAcquired();

        RsvpResponse response = rsvpService.submitRsvp(new SubmitRsvpRequest(token, "CONFIRMED"));

        assertThat(response.success()).isTrue();
        assertThat(response.wasWaitlisted()).isFalse();
        assertThat(response.message()).contains("Thank you");

        // Lock must be released even on success
        verify(redisTemplate).delete("rsvp:lock:" + EVENT_ID);
        // Kafka event must be published
        verify(kafkaTemplate).send(eq("rsvp-submitted"), eq(EVENT_ID.toString()), any());
    }

    @Test
    void submitRsvp_confirmed_eventAtCapacity_waitlistsGuest() {
        String token = buildToken(GUEST_ID, EVENT_ID, false);
        stubEventDetailsCall();
        stubCapacityCall(100, 100); // full house
        stubLockAcquired();

        RsvpResponse response = rsvpService.submitRsvp(new SubmitRsvpRequest(token, "CONFIRMED"));

        assertThat(response.success()).isTrue();
        assertThat(response.wasWaitlisted()).isTrue();
        assertThat(response.message()).contains("waitlist");

        verify(kafkaTemplate).send(eq("rsvp-submitted"), eq(EVENT_ID.toString()), any());
        verify(redisTemplate).delete("rsvp:lock:" + EVENT_ID);
    }

    @Test
    void submitRsvp_declined_skipsCapacityCheck_recordsDeclined() {
        String token = buildToken(GUEST_ID, EVENT_ID, false);
        stubEventDetailsCall();
        stubLockAcquired();

        RsvpResponse response = rsvpService.submitRsvp(new SubmitRsvpRequest(token, "DECLINED"));

        assertThat(response.success()).isTrue();
        assertThat(response.wasWaitlisted()).isFalse();

        // Capacity endpoint must NOT be called for non-CONFIRMED statuses
        verify(restTemplate, never()).getForObject(contains("capacity"), any(), any(Object[].class));
        verify(redisTemplate).delete("rsvp:lock:" + EVENT_ID);
    }

    @Test
    void submitRsvp_maybe_skipsCapacityCheck_recordsMaybe() {
        String token = buildToken(GUEST_ID, EVENT_ID, false);
        stubEventDetailsCall();
        stubLockAcquired();

        RsvpResponse response = rsvpService.submitRsvp(new SubmitRsvpRequest(token, "MAYBE"));

        assertThat(response.success()).isTrue();
        verify(restTemplate, never()).getForObject(contains("capacity"), any(), any(Object[].class));
    }

    // ════════════════════════════════════════════════════════════════════════
    // RSVP SUBMISSION — DUPLICATE PREVENTION
    // ════════════════════════════════════════════════════════════════════════

    @Test
    void submitRsvp_alreadyResponded_returnsFailureWithoutPublishingEvent() {
        // tokenUsed=true → guest already submitted
        String token = buildToken(GUEST_ID, EVENT_ID, true);
        stubEventDetailsCall();

        RsvpResponse response = rsvpService.submitRsvp(new SubmitRsvpRequest(token, "CONFIRMED"));

        assertThat(response.success()).isFalse();
        assertThat(response.message()).contains("already responded");

        // No lock acquired, no Kafka event fired
        verify(valueOperations, never()).setIfAbsent(anyString(), anyString(), anyLong(), any());
        verify(kafkaTemplate, never()).send(anyString(), anyString(), any());
    }

    // ════════════════════════════════════════════════════════════════════════
    // RSVP SUBMISSION — CONCURRENCY (the most important section)
    // ════════════════════════════════════════════════════════════════════════

    @Test
    void submitRsvp_lockAlreadyHeld_throwsLockNotAcquiredException() {
        // Another request already holds the lock for this event
        String token = buildToken(GUEST_ID, EVENT_ID, false);
        stubEventDetailsCall();
        when(valueOperations.setIfAbsent(
            eq("rsvp:lock:" + EVENT_ID), eq("1"), eq(5L), eq(TimeUnit.SECONDS)
        )).thenReturn(false); // false = lock already taken

        assertThatThrownBy(() -> rsvpService.submitRsvp(new SubmitRsvpRequest(token, "CONFIRMED")))
            .isInstanceOf(RsvpService.LockNotAcquiredException.class)
            .hasMessageContaining("busy");

        // Lock was never held by us, so we must NOT delete it
        verify(redisTemplate, never()).delete(anyString());
        // No Kafka event must fire
        verify(kafkaTemplate, never()).send(anyString(), anyString(), any());
    }

    @Test
    void submitRsvp_lockReturnedNull_throwsLockNotAcquiredException() {
        // Redis can return null if the connection is interrupted
        String token = buildToken(GUEST_ID, EVENT_ID, false);
        stubEventDetailsCall();
        when(valueOperations.setIfAbsent(
            eq("rsvp:lock:" + EVENT_ID), eq("1"), eq(5L), eq(TimeUnit.SECONDS)
        )).thenReturn(null);

        assertThatThrownBy(() -> rsvpService.submitRsvp(new SubmitRsvpRequest(token, "CONFIRMED")))
            .isInstanceOf(RsvpService.LockNotAcquiredException.class);
    }

    @Test
    void submitRsvp_lockReleasedEvenWhenCapacityCallFails() {
        // If the event service is down mid-request, the lock must still be released.
        // Otherwise every subsequent RSVP for this event would be permanently blocked.
        String token = buildToken(GUEST_ID, EVENT_ID, false);
        stubEventDetailsCall();
        stubLockAcquired();

        // Capacity call throws — simulates event service being unavailable
        when(restTemplate.getForObject(contains("capacity"), eq(Map.class), eq(EVENT_ID)))
            .thenThrow(new org.springframework.web.client.RestClientException("timeout"));

        assertThatThrownBy(() -> rsvpService.submitRsvp(new SubmitRsvpRequest(token, "CONFIRMED")))
            .isInstanceOf(RsvpService.ResourceNotFoundException.class);

        // THE CRITICAL ASSERTION: lock must be deleted despite the exception
        verify(redisTemplate).delete("rsvp:lock:" + EVENT_ID);
    }

    // ════════════════════════════════════════════════════════════════════════
    // Private helpers — build real JWTs using the same secret as the service
    // ════════════════════════════════════════════════════════════════════════

    private String buildToken(UUID guestId, UUID eventId, boolean tokenUsed) {
        return Jwts.builder()
            .subject(guestId.toString())
            .claim("guestId",   guestId.toString())
            .claim("eventId",   eventId.toString())
            .claim("guestName", GUEST_NAME)
            .claim("tokenUsed", tokenUsed)
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + 86_400_000L)) // 24h
            .signWith(signingKey())
            .compact();
    }

    private String buildExpiredToken(UUID guestId, UUID eventId) {
        long oneHourAgo = System.currentTimeMillis() - 3_600_000L;
        return Jwts.builder()
            .subject(guestId.toString())
            .claim("guestId",   guestId.toString())
            .claim("eventId",   eventId.toString())
            .claim("guestName", GUEST_NAME)
            .claim("tokenUsed", false)
            .issuedAt(new Date(oneHourAgo - 1000))
            .expiration(new Date(oneHourAgo)) // already expired
            .signWith(signingKey())
            .compact();
    }

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8));
    }

    // ── Stub helpers — keep test bodies focused on the behaviour being tested ──

    @SuppressWarnings("unchecked")
    private void stubEventDetailsCall() {
        Map<String, Object> eventResponse = Map.of(
            "title",     EVENT_TITLE,
            "eventDate", EVENT_DATE.toString(),
            "location",  LOCATION
        );
        when(restTemplate.getForObject(
            contains("/public"), eq(Map.class), eq(EVENT_ID)
        )).thenReturn(eventResponse);
    }

    @SuppressWarnings("unchecked")
    private void stubCapacityCall(int current, int max) {
        Map<String, Object> capacityResponse = Map.of(
            "currentConfirmed", current,
            "maxCapacity",      max
        );
        when(restTemplate.getForObject(
            contains("capacity"), eq(Map.class), eq(EVENT_ID)
        )).thenReturn(capacityResponse);
    }

    private void stubLockAcquired() {
        when(valueOperations.setIfAbsent(
            eq("rsvp:lock:" + EVENT_ID), eq("1"), eq(5L), eq(TimeUnit.SECONDS)
        )).thenReturn(true);
    }
}
