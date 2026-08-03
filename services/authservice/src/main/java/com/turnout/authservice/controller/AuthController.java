package com.turnout.authservice.controller;

import com.turnout.authservice.dto.*;
import com.turnout.authservice.entity.User;
import com.turnout.authservice.repository.UserRepository;
import com.turnout.authservice.service.AdminUserService;
import com.turnout.authservice.service.AuthService;
import com.turnout.common.dto.ApiResponse;
import com.turnout.common.exception.ResourceNotFoundException;
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

    private static final long FCM_TTL_SECONDS = 2_592_000L;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegisterResponse>> register(
            @Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        "Registration successful. Check your email for OTP.",
                        authService.register(request)));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<MessageResponse>> verifyOtp(
            @Valid @RequestBody VerifyOtpRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success("Email verified successfully.", authService.verifyOtp(request)));
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<ApiResponse<Void>> resendOtp(
            @Valid @RequestBody ResendOtpRequest request) {
        authService.resendOtp(request);
        return ResponseEntity.ok(ApiResponse.success("OTP resent to your email."));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success("Login successful.", authService.login(request)));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            @Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success("Token refreshed.", authService.refresh(request)));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestHeader("X-User-Id") String userId) {
        authService.logout(userId);
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully."));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok(
                ApiResponse.success("If this email exists, a reset link has been sent."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success("Password reset successfully."));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> me(
            @RequestHeader("X-User-Id") String userId) {
        User user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        return ResponseEntity.ok(ApiResponse.success("User profile retrieved.", new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getRole().name(),
                user.getStatus().name(),
                user.isEmailVerified()
        )));
    }

    @PostMapping("/fcm-token")
    public ResponseEntity<ApiResponse<Void>> storeFcmToken(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody FcmTokenRequest request) {
        redisTemplate.opsForValue().set(
                "fcm:" + userId,
                request.fcmToken(),
                FCM_TTL_SECONDS,
                TimeUnit.SECONDS
        );
        return ResponseEntity.ok(ApiResponse.success("FCM token saved."));
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<ApiResponse<UserLookupResponse>> lookupUser(@PathVariable UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
        return ResponseEntity.ok(ApiResponse.success("User found.", new UserLookupResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName()
        )));
    }
}
