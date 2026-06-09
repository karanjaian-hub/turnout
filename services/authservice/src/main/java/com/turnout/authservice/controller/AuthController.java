package com.turnout.authservice.controller;

import com.turnout.authservice.dto.*;
import com.turnout.authservice.entity.User;
import com.turnout.authservice.repository.UserRepository;
import com.turnout.authservice.service.AuthService;
import com.turnout.common.exception.ResourceNotFoundException;
import com.turnout.common.security.JwtUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(
            @Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(authService.register(request));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<MessageResponse> verifyOtp(
            @Valid @RequestBody VerifyOtpRequest request) {
        return ResponseEntity.ok(authService.verifyOtp(request));
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<MessageResponse> resendOtp(
            @Valid @RequestBody ResendOtpRequest request) {
        return ResponseEntity.ok(authService.resendOtp(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<MessageResponse> logout(
            @RequestHeader("Authorization") String authHeader) {

        // Extract token from "Bearer <token>"
        String token = authHeader.substring(7);
        String userId = jwtUtil.extractUserId(token).toString();
        String jti    = jwtUtil.extractJti(token);

        // Calculate remaining lifetime so we set the correct Redis TTL
        long expiryMs = jwtUtil.extractExpiration(token).getTime() - System.currentTimeMillis();

        return ResponseEntity.ok(authService.logout(userId, jti, expiryMs));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<MessageResponse> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        return ResponseEntity.ok(authService.forgotPassword(request));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<MessageResponse> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        return ResponseEntity.ok(authService.resetPassword(request));
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User", userDetails.getUsername()));

        return ResponseEntity.ok(new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getRole().name(),
                user.getStatus().name(),
                user.isEmailVerified()
        ));
    }
}
