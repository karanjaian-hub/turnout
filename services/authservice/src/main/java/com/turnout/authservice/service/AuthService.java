package com.turnout.authservice.service;

import com.turnout.authservice.dto.*;
import com.turnout.authservice.entity.User;
import com.turnout.authservice.repository.UserRepository;
import com.turnout.common.enums.AccountStatus;
import com.turnout.common.enums.UserRole;
import com.turnout.common.exception.DuplicateResourceException;
import com.turnout.common.exception.InvalidTokenException;
import com.turnout.common.exception.ResourceNotFoundException;
import com.turnout.common.exception.TurnoutException;
import com.turnout.common.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RedisTemplate<String, Object> redisTemplate;
    private final BrevoEmailService brevoEmailService;

    @Value("${turnout.jwt.expiration-ms}")
    private long accessTokenExpiryMs;

    @Value("${turnout.jwt.refresh-expiration-ms}")
    private long refreshTokenExpiryMs;

    @Value("${turnout.otp.expiry-minutes}")
    private int otpExpiryMinutes;

    @Value("${turnout.otp.max-attempts}")
    private int otpMaxAttempts;

    @Value("${turnout.reset.token-expiry-minutes}")
    private int resetTokenExpiryMinutes;

// Redis key prefixes
    private static final String KEY_OTP          = "otp:";
    private static final String KEY_OTP_ATTEMPTS = "otp:attempts:";
    private static final String KEY_REFRESH      = "refresh:";
    private static final String KEY_BLACKLIST    = "blacklist:access:";
    private static final String KEY_RESET        = "reset:";

// Registration
    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("User", "email", request.email());
        }
        if (userRepository.existsByUsername(request.username())) {
            throw new DuplicateResourceException("User", "username", request.username());
        }

        User user = new User();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setFullName(request.fullName());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(UserRole.EVENT_ORGANIZER);
        user.setStatus(AccountStatus.PENDING_VERIFICATION);
        user.setEmailVerified(false);

        User saved = userRepository.save(user);

        String otp = generateAndStoreOtp(saved.getId());
        brevoEmailService.sendOtpEmail(saved.getEmail(), saved.getFullName(), otp);

        return new RegisterResponse(
                saved.getId(),
                saved.getUsername(),
                saved.getEmail(),
                "Registration successful. Check your email for the verification code."
        );
    }

// OTP verification
    @Transactional
    public MessageResponse verifyOtp(VerifyOtpRequest request) {
        String attemptsKey = KEY_OTP_ATTEMPTS + request.userId();
        Integer attempts = (Integer) redisTemplate.opsForValue().get(attemptsKey);

        if (attempts != null && attempts >= otpMaxAttempts) {
            throw new InvalidTokenException("Too many incorrect attempts. Request a new code.");
        }

        String storedOtp = (String) redisTemplate.opsForValue().get(KEY_OTP + request.userId());
        if (storedOtp == null) {
            throw new InvalidTokenException("OTP expired. Request a new code.");
        }

        if (!storedOtp.equals(request.otp())) {
            // Increment attempt counter — keep same TTL as the OTP
            redisTemplate.opsForValue().increment(attemptsKey);
            redisTemplate.expire(attemptsKey, otpExpiryMinutes, TimeUnit.MINUTES);
            throw new InvalidTokenException("Incorrect verification code.");
        }

        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User", request.userId()));

        user.setEmailVerified(true);
        user.setStatus(AccountStatus.ACTIVE);
        userRepository.save(user);

// Clean up — OTP keys no longer needed
        redisTemplate.delete(KEY_OTP + request.userId());
        redisTemplate.delete(attemptsKey);

        brevoEmailService.sendWelcomeEmail(user.getEmail(), user.getFullName());

        return new MessageResponse("Email verified successfully. You can now log in.", true);
    }

// Resend OTP
    public MessageResponse resendOtp(ResendOtpRequest request) {
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User", request.userId()));

    // Reset attempts and issue a fresh code
        redisTemplate.delete(KEY_OTP + request.userId());
        redisTemplate.delete(KEY_OTP_ATTEMPTS + request.userId());

        String otp = generateAndStoreOtp(user.getId());
        brevoEmailService.sendOtpEmail(user.getEmail(), user.getFullName(), otp);

        return new MessageResponse("A new verification code has been sent to your email.", true);
    }

// Login
    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new BadCredentialsException("Invalid username or password"));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BadCredentialsException("Invalid username or password");
        }

        if (user.getStatus() == AccountStatus.PENDING_VERIFICATION) {
            throw new TurnoutException("Email not verified. Check your inbox for the OTP.", "UNVERIFIED_EMAIL");
        }

        if (user.getStatus() != AccountStatus.ACTIVE) {
            throw new TurnoutException("Account is " + user.getStatus().name().toLowerCase() + ".", "ACCOUNT_INACTIVE");
        }

        String accessToken  = jwtUtil.generateAccessToken(user.getId(), user.getRole(), accessTokenExpiryMs);
        String refreshToken = jwtUtil.generateRefreshToken(user.getId(), refreshTokenExpiryMs);

// Store refresh token in Redis — used to validate rotation on next refresh call
        redisTemplate.opsForValue().set(
                KEY_REFRESH + user.getId(),
                refreshToken,
                refreshTokenExpiryMs,
                TimeUnit.MILLISECONDS
        );

        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        return new AuthResponse(
                accessToken,
                refreshToken,
                "Bearer",
                accessTokenExpiryMs,
                user.getUsername(),
                user.getRole().name()
        );
    }

// Refresh token
    public AuthResponse refresh(RefreshTokenRequest request) {
        if (!jwtUtil.validateToken(request.refreshToken())) {
            throw new InvalidTokenException("Refresh token is invalid or expired.");
        }

        UUID userId = jwtUtil.extractUserId(request.refreshToken());
        String storedToken = (String) redisTemplate.opsForValue().get(KEY_REFRESH + userId);

        if (storedToken == null || !storedToken.equals(request.refreshToken())) {
            throw new InvalidTokenException("Refresh token has been rotated or revoked.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        String newAccessToken  = jwtUtil.generateAccessToken(user.getId(), user.getRole(), accessTokenExpiryMs);
        String newRefreshToken = jwtUtil.generateRefreshToken(user.getId(), refreshTokenExpiryMs);

// Rotate — old refresh token is now invalid
        redisTemplate.opsForValue().set(
                KEY_REFRESH + userId,
                newRefreshToken,
                refreshTokenExpiryMs,
                TimeUnit.MILLISECONDS
        );

        return new AuthResponse(
                newAccessToken,
                newRefreshToken,
                "Bearer",
                accessTokenExpiryMs,
                user.getUsername(),
                user.getRole().name()
        );
    }

    public MessageResponse logout(String userId) {
        // Delete refresh token — prevents token rotation attacks.
        // Access token expires naturally (15 min). Full JTI blacklisting
        // will be wired in the gateway phase.
        redisTemplate.delete(KEY_REFRESH + userId);
        return new MessageResponse("Logged out successfully.", true);
    }

    public MessageResponse forgotPassword(ForgotPasswordRequest request) {
// Always return success — never reveal whether an email exists
        userRepository.findByEmail(request.email()).ifPresent(user -> {
            String resetToken = jwtUtil.generateAccessToken(
                    user.getId(), user.getRole(),
                    (long) resetTokenExpiryMinutes * 60 * 1000
            );

            redisTemplate.opsForValue().set(
                    KEY_RESET + resetToken,
                    user.getId().toString(),
                    resetTokenExpiryMinutes,
                    TimeUnit.MINUTES
            );

            brevoEmailService.sendPasswordResetEmail(user.getEmail(), user.getFullName(), resetToken);
        });

        return new MessageResponse("If that email exists, a reset link has been sent.", true);
    }

// Reset password
    @Transactional
    public MessageResponse resetPassword(ResetPasswordRequest request) {
        if (!jwtUtil.validateToken(request.token())) {
            throw new InvalidTokenException("Reset token is invalid or expired.");
        }

        String storedUserId = (String) redisTemplate.opsForValue().get(KEY_RESET + request.token());
        if (storedUserId == null) {
            throw new InvalidTokenException("Reset token has already been used or expired.");
        }

        User user = userRepository.findById(UUID.fromString(storedUserId))
                .orElseThrow(() -> new ResourceNotFoundException("User", storedUserId));

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

// Invalidate reset token and all active sessions
        redisTemplate.delete(KEY_RESET + request.token());
        redisTemplate.delete(KEY_REFRESH + user.getId());

        return new MessageResponse("Password reset successfully. Please log in again.", true);
    }

// Private helpers
    private String generateAndStoreOtp(UUID userId) {
// 6-digit zero-padded numeric OTP
        String otp = String.format("%06d", (int) (Math.random() * 1_000_000));

        redisTemplate.opsForValue().set(
                KEY_OTP + userId,
                otp,
                otpExpiryMinutes,
                TimeUnit.MINUTES
        );

        return otp;
    }
}
