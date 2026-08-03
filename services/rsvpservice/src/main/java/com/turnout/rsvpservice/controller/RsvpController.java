package com.turnout.rsvpservice.controller;

import com.turnout.rsvpservice.dto.RsvpResponse;
import com.turnout.rsvpservice.dto.SubmitRsvpRequest;
import com.turnout.rsvpservice.dto.ValidateTokenResponse;
import com.turnout.rsvpservice.service.RsvpService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import com.turnout.common.dto.ApiResponse;
import org.springframework.web.bind.annotation.*;
import com.turnout.common.dto.ApiResponse;

@RestController
@RequestMapping("/api/rsvp")
@RequiredArgsConstructor
public class RsvpController {

    private final RsvpService rsvpService;

// Public ((no JWT. The guest token in the query param IS the auth.
    @GetMapping("/validate")
    public ResponseEntity<ApiResponse<ValidateTokenResponse>> validateToken(@RequestParam("token") String token) {
        return ResponseEntity.ok(ApiResponse.success("Token validated.", rsvpService.validateToken(token)));
    }

// Public ((guest submits their RSVP choice (CONFIRMED / DECLINED / MAYBE).
    @PostMapping("/submit")
    public ResponseEntity<ApiResponse<RsvpResponse>> submitRsvp(@Valid @RequestBody SubmitRsvpRequest request) {
        RsvpResponse rsvpResponse = rsvpService.submitRsvp(request);
        String message;
        if (!rsvpResponse.success()) {
            message = rsvpResponse.message();
        } else if (rsvpResponse.wasWaitlisted()) {
            message = "You have been added to the waitlist.";
        } else if ("DECLINED".equals(request.rsvpStatus())) {
            message = "Thank you for letting us know.";
        } else if ("MAYBE".equals(request.rsvpStatus())) {
            message = "Your response has been recorded.";
        } else {
            message = "RSVP confirmed! See you there.";
        }
        return ResponseEntity.ok(ApiResponse.success(message, rsvpResponse));
    }
}
