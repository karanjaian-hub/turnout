package com.turnout.authservice.controller;

import com.turnout.authservice.dto.*;
import com.turnout.authservice.entity.User;
import com.turnout.authservice.repository.UserRepository;
import com.turnout.authservice.service.AdminUserService;
import com.turnout.authservice.service.AuthService;
import com.turnout.common.exception.ResourceNotFoundException;
import com.turnout.common.exception.UnauthorizedAccessException;
import com.turnout.common.security.JwtUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final AdminUserService adminUserService;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final long FCM_TTL_SECONDS = 2_592_000L; // 30 days

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
            @RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(authService.logout(userId));
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
            @RequestHeader("X-User-Id") String userId) {
        User user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
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

// Stores the Android FCM push token in Redis so the notification service
// can look it up when sending push notifications to this user.
    @PostMapping("/fcm-token")
    public ResponseEntity<MessageResponse> storeFcmToken(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody FcmTokenRequest request) {
        redisTemplate.opsForValue().set(
                "fcm:" + userId,
                request.fcmToken(),
                FCM_TTL_SECONDS,
                TimeUnit.SECONDS
        );
        return ResponseEntity.ok(new MessageResponse("FCM token stored.", true));
    }

// Internal endpoint,,  called by other services (payment-service... ect)
// via WebClient to resolve a userId to basic profile info.
    @GetMapping("/users/{id}")
    public ResponseEntity<UserLookupResponse> lookupUser(@PathVariable UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
        return ResponseEntity.ok(new UserLookupResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName()
        ));
    }


}
