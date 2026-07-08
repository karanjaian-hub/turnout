package com.turnout.rsvpservice.controller;

import com.turnout.rsvpservice.dto.RsvpResponse;
import com.turnout.rsvpservice.dto.SubmitRsvpRequest;
import com.turnout.rsvpservice.dto.ValidateTokenResponse;
import com.turnout.rsvpservice.service.RsvpService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rsvp")
@RequiredArgsConstructor
public class RsvpController {

    private final RsvpService rsvpService;

    // Public — no JWT. The guest token in the query param IS the auth.
    // The frontend calls this first to show event details before the form.
    @GetMapping("/validate")
    public ResponseEntity<ValidateTokenResponse> validateToken(@RequestParam("token") String token) {
        return ResponseEntity.ok(rsvpService.validateToken(token));
    }

    // Public — guest submits their RSVP choice (CONFIRMED / DECLINED / MAYBE).
    @PostMapping("/submit")
    public ResponseEntity<RsvpResponse> submitRsvp(@Valid @RequestBody SubmitRsvpRequest request) {
        return ResponseEntity.ok(rsvpService.submitRsvp(request));
    }
}
