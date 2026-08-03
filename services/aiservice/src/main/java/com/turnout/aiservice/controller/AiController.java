package com.turnout.aiservice.controller;

import com.turnout.common.dto.ApiResponse;
import com.turnout.aiservice.dto.AiRequests.*;
import com.turnout.aiservice.dto.AiResponses.*;
import com.turnout.aiservice.service.AiService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiService aiService;

    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    @PostMapping("/generate/description")
    public ResponseEntity<ApiResponse<EventDescriptionResponse>> generateDescription(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody EventDescriptionRequest request) {
        EventDescriptionResponse result = aiService.generateEventDescription(request);
        return toResponse(result, "Event description generated.");
    }

    @PostMapping("/generate/invitation")
    public ResponseEntity<ApiResponse<InvitationCopyResponse>> generateInvitation(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody InvitationCopyRequest request) {
        InvitationCopyResponse result = aiService.generateInvitationCopy(request);
        return toResponse(result, "Invitation copy generated.");
    }

    @PostMapping("/generate/followup")
    public ResponseEntity<ApiResponse<FollowupResponse>> generateFollowup(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody FollowupRequest request) {
        FollowupResponse result = aiService.generateFollowup(request);
        return toResponse(result, "Follow-up suggestions generated.");
    }

    @PostMapping("/insights/event")
    public ResponseEntity<ApiResponse<RsvpInsightsResponse>> getRsvpInsights(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody RsvpInsightsRequest request) {
        RsvpInsightsResponse result = aiService.getRsvpInsights(request);
        return toResponse(result, "RSVP insights generated.");
    }

    @PostMapping("/predict/sendtime")
    public ResponseEntity<ApiResponse<SendTimeResponse>> predictSendTime(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody SendTimeRequest request) {
        SendTimeResponse result = aiService.predictSendTime(request);
        return toResponse(result, "Send time recommendation generated.");
    }

    @PostMapping("/predict/capacity")
    public ResponseEntity<ApiResponse<CapacityForecastResponse>> predictCapacity(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody CapacityForecastRequest request) {
        CapacityForecastResponse result = aiService.predictCapacity(request);
        return toResponse(result, "Capacity forecast generated.");
    }

    private <T> ResponseEntity<ApiResponse<T>> toResponse(T result, String successMessage) {
        if (result == null) {
            return ResponseEntity.status(503)
                    .body(ApiResponse.error("AI service is temporarily unavailable. Please try again shortly."));
        }
        return ResponseEntity.ok(ApiResponse.success(successMessage, result));
    }
}
