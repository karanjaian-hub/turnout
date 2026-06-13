package com.turnout.aiservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.turnout.aiservice.client.GroqClient;
import com.turnout.aiservice.dto.AiRequests.*;
import com.turnout.aiservice.dto.AiResponses.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;

@Service
public class AiService {

    private static final Logger log = LoggerFactory.getLogger(AiService.class);
    private static final String UNAVAILABLE = "AI service temporarily unavailable.";

    private final GroqClient groqClient;
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final long cacheTtlMinutes;

    @Value("classpath:prompts/event-description.txt")
    private Resource eventDescriptionPrompt;

    @Value("classpath:prompts/invitation-copy.txt")
    private Resource invitationCopyPrompt;

    @Value("classpath:prompts/followup-suggestion.txt")
    private Resource followupPrompt;

    @Value("classpath:prompts/rsvp-insights.txt")
    private Resource rsvpInsightsPrompt;

    @Value("classpath:prompts/send-time.txt")
    private Resource sendTimePrompt;

    @Value("classpath:prompts/capacity-forecast.txt")
    private Resource capacityForecastPrompt;

    public AiService(GroqClient groqClient,
                     StringRedisTemplate redis,
                     ObjectMapper objectMapper,
                     @Value("${turnout.ai.cache-ttl-minutes}") long cacheTtlMinutes) {
        this.groqClient      = groqClient;
        this.redis           = redis;
        this.objectMapper    = objectMapper;
        this.cacheTtlMinutes = cacheTtlMinutes;
    }

    public EventDescriptionResponse generateEventDescription(EventDescriptionRequest req) {
        String userMessage = "Event notes: " + req.notes();
        String raw = cachedGroqCall("event-description", userMessage, eventDescriptionPrompt);
        return parseOrNull(raw, EventDescriptionResponse.class);
    }

    public InvitationCopyResponse generateInvitationCopy(InvitationCopyRequest req) {
        String userMessage = String.format(
                "Event title: %s | Date: %s | Location: %s | Description: %s",
                req.title(), req.date(), req.location(), req.description());
        String raw = cachedGroqCall("invitation-copy", userMessage, invitationCopyPrompt);
        return parseOrNull(raw, InvitationCopyResponse.class);
    }

    public FollowupResponse generateFollowup(FollowupRequest req) {
        String userMessage = String.format(
                "Event: %s | Days since sent: %d | Non-responders: %d",
                req.eventTitle(), req.daysSinceSent(), req.nonResponderCount());
        String raw = cachedGroqCall("followup", userMessage, followupPrompt);
        return parseOrNull(raw, FollowupResponse.class);
    }

    public RsvpInsightsResponse getRsvpInsights(RsvpInsightsRequest req) {
        String userMessage = String.format(
                "Event ID: %s | Total invited: %d | Confirmed: %d | Declined: %d | Pending: %d",
                req.eventId(), req.total(), req.confirmed(), req.declined(), req.pending());
        String raw = cachedGroqCall("rsvp-insights", userMessage, rsvpInsightsPrompt);
        return parseOrNull(raw, RsvpInsightsResponse.class);
    }

    public SendTimeResponse predictSendTime(SendTimeRequest req) {
        String userMessage = String.format(
                "Event type: %s | Audience size: %d | Event date: %s",
                req.eventType(), req.audienceSize(), req.eventDate());
        String raw = cachedGroqCall("send-time", userMessage, sendTimePrompt);
        return parseOrNull(raw, SendTimeResponse.class);
    }

    public CapacityForecastResponse predictCapacity(CapacityForecastRequest req) {
        String userMessage = String.format(
                "Event type: %s | Total invited: %d | Days until event: %d",
                req.eventType(), req.totalInvited(), req.daysUntilEvent());
        String raw = cachedGroqCall("capacity-forecast", userMessage, capacityForecastPrompt);
        return parseOrNull(raw, CapacityForecastResponse.class);
    }

    // ---------------------------------------------------------------------------
    // Private helpers
    // ---------------------------------------------------------------------------

    /**
     * Check Redis → on hit return cached value → on miss call Groq → cache result.
     * Cache key: ai:{featureName}:{SHA-256 of userMessage}
     * SHA-256 keeps keys short and fixed-length regardless of input size.
     */
    private String cachedGroqCall(String featureName, String userMessage, Resource promptResource) {
        String cacheKey = buildCacheKey(featureName, userMessage);

        String cached = redis.opsForValue().get(cacheKey);
        if (cached != null) {
            log.debug("Cache hit for feature={}", featureName);
            return cached;
        }

        String systemPrompt = loadPrompt(promptResource);
        String response     = groqClient.chat(systemPrompt, userMessage);

        // Don't cache the unavailable fallback — next request should retry Groq
        if (!response.equals(UNAVAILABLE)) {
            redis.opsForValue().set(cacheKey, response, Duration.ofMinutes(cacheTtlMinutes));
        }

        return response;
    }

    private String buildCacheKey(String featureName, String userMessage) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(userMessage.getBytes(StandardCharsets.UTF_8));
            return "ai:" + featureName + ":" + HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            log.warn("SHA-256 unavailable, falling back to hashCode");
            return "ai:" + featureName + ":" + userMessage.hashCode();
        }
    }

    private String loadPrompt(Resource resource) {
        try {
            return resource.getContentAsString(StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Failed to load prompt file: {}", resource.getFilename());
            throw new IllegalStateException("Prompt file missing: " + resource.getFilename());
        }
    }

    /**
     * Parses Groq's JSON response into a typed record.
     * Returns null on failure — controller maps null to 503.
     * Groq occasionally wraps output in markdown fences despite the prompt — strip them first.
     */
    private <T> T parseOrNull(String raw, Class<T> type) {
        if (raw == null || raw.equals(UNAVAILABLE)) return null;
        try {
            String cleaned = raw.strip()
                    .replaceAll("^```json\\s*", "")
                    .replaceAll("^```\\s*",     "")
                    .replaceAll("```$",          "");
            return objectMapper.readValue(cleaned, type);
        } catch (Exception e) {
            log.error("Failed to parse Groq response for type={}: raw={}", type.getSimpleName(), raw);
            return null;
        }
    }
}
