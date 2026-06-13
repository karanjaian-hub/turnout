package com.turnout.aiservice.controller;

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
    public ResponseEntity<?> generateDescription(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody EventDescriptionRequest request) {
        return toResponse(aiService.generateEventDescription(request));
    }

    @PostMapping("/generate/invitation")
    public ResponseEntity<?> generateInvitation(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody InvitationCopyRequest request) {
        return toResponse(aiService.generateInvitationCopy(request));
    }

    @PostMapping("/generate/followup")
    public ResponseEntity<?> generateFollowup(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody FollowupRequest request) {
        return toResponse(aiService.generateFollowup(request));
    }

    @PostMapping("/insights/event")
    public ResponseEntity<?> getRsvpInsights(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody RsvpInsightsRequest request) {
        return toResponse(aiService.getRsvpInsights(request));
    }

    @PostMapping("/predict/sendtime")
    public ResponseEntity<?> predictSendTime(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody SendTimeRequest request) {
        return toResponse(aiService.predictSendTime(request));
    }

    @PostMapping("/predict/capacity")
    public ResponseEntity<?> predictCapacity(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody CapacityForecastRequest request) {
        return toResponse(aiService.predictCapacity(request));
    }

    /**
     * Null means Groq was down or JSON parsing failed.
     * 503 = server is up, but a dependency it needs is not responding.
     */
    private ResponseEntity<?> toResponse(Object result) {
        if (result == null) {
            return ResponseEntity.status(503)
                    .body(new ErrorResponse("AI service temporarily unavailable. Please try again."));
        }
        return ResponseEntity.ok(result);
    }
}
