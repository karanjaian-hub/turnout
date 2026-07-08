package com.turnout.authservice;

import com.turnout.authservice.dto.*;
import com.turnout.authservice.entity.User;
import com.turnout.authservice.repository.UserRepository;
import com.turnout.authservice.service.AuthService;
import com.turnout.authservice.service.BrevoEmailService;
import com.turnout.common.enums.AccountStatus;
import com.turnout.common.enums.UserRole;
import com.turnout.common.exception.DuplicateResourceException;
import com.turnout.common.exception.InvalidTokenException;
import com.turnout.common.exception.TurnoutException;
import com.turnout.common.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

// LENIENT strictness because redisTemplate.opsForValue() is stubbed in @BeforeEach
// but not every test touches Redis. Strict mode would flag those as unnecessary stubs.
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtUtil jwtUtil;
    @Mock private RedisTemplate<String, Object> redisTemplate;
    @Mock private BrevoEmailService brevoEmailService;
    @Mock private ValueOperations<String, Object> valueOperations;

    @InjectMocks private AuthService authService;

    // ── Shared test data ─────────────────────────────────────────────────────

    private static final UUID   USER_ID       = UUID.randomUUID();
    private static final String EMAIL         = "ian@turnout.app";
    private static final String USERNAME      = "ian_karanja";
    private static final String FULL_NAME     = "Ian Karanja";
    private static final String RAW_PASSWORD  = "SecurePass123!";
    private static final String HASHED_PW     = "$2a$10$hashedpassword";
    private static final String OTP           = "482910";
    private static final String ACCESS_TOKEN  = "eyJ.access.token";
    private static final String REFRESH_TOKEN = "eyJ.refresh.token";

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        ReflectionTestUtils.setField(authService, "accessTokenExpiryMs",     900_000L);
        ReflectionTestUtils.setField(authService, "refreshTokenExpiryMs",    604_800_000L);
        ReflectionTestUtils.setField(authService, "otpExpiryMinutes",        10);
        ReflectionTestUtils.setField(authService, "otpMaxAttempts",          3);
        ReflectionTestUtils.setField(authService, "resetTokenExpiryMinutes", 15);
    }

    // ════════════════════════════════════════════════════════════════════════
    // REGISTRATION
    // ════════════════════════════════════════════════════════════════════════

    @Test
    void register_success_savesUser_storesOtp_sendsEmail() {
        when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
        when(userRepository.existsByUsername(USERNAME)).thenReturn(false);
        when(passwordEncoder.encode(RAW_PASSWORD)).thenReturn(HASHED_PW);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            ReflectionTestUtils.setField(u, "id", USER_ID);
            return u;
        });

        var response = authService.register(buildRegisterRequest());

        verify(userRepository).save(argThat(u ->
            u.getEmail().equals(EMAIL) &&
            u.getUsername().equals(USERNAME) &&
            u.getPassword().equals(HASHED_PW) &&
            u.getStatus() == AccountStatus.PENDING_VERIFICATION &&
            !u.isEmailVerified()
        ));
        verify(valueOperations).set(eq("otp:" + USER_ID), anyString(), eq(10L), eq(TimeUnit.MINUTES));
        verify(brevoEmailService).sendOtpEmail(eq(EMAIL), eq(FULL_NAME), anyString());
        assertThat(response.email()).isEqualTo(EMAIL);
    }

    @Test
    void register_duplicateEmail_throws() {
        when(userRepository.existsByEmail(EMAIL)).thenReturn(true);

        assertThatThrownBy(() -> authService.register(buildRegisterRequest()))
            .isInstanceOf(DuplicateResourceException.class)
            .hasMessageContaining("email");

        verify(userRepository, never()).save(any());
    }

    @Test
    void register_duplicateUsername_throws() {
        when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
        when(userRepository.existsByUsername(USERNAME)).thenReturn(true);

        assertThatThrownBy(() -> authService.register(buildRegisterRequest()))
            .isInstanceOf(DuplicateResourceException.class)
            .hasMessageContaining("username");

        verify(userRepository, never()).save(any());
    }

    // ════════════════════════════════════════════════════════════════════════
    // OTP VERIFICATION
    // ════════════════════════════════════════════════════════════════════════

    @Test
    void verifyOtp_success_activatesUser_cleansUpRedis() {
        when(valueOperations.get("otp:attempts:" + USER_ID)).thenReturn(null);
        when(valueOperations.get("otp:" + USER_ID)).thenReturn(OTP);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(pendingUser()));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        authService.verifyOtp(new VerifyOtpRequest(USER_ID, OTP));

        verify(userRepository).save(argThat(u ->
            u.isEmailVerified() && u.getStatus() == AccountStatus.ACTIVE
        ));
        verify(redisTemplate).delete("otp:" + USER_ID);
        verify(redisTemplate).delete("otp:attempts:" + USER_ID);
        verify(brevoEmailService).sendWelcomeEmail(EMAIL, FULL_NAME);
    }

    @Test
    void verifyOtp_tooManyAttempts_throws() {
        when(valueOperations.get("otp:attempts:" + USER_ID)).thenReturn(3);

        assertThatThrownBy(() -> authService.verifyOtp(new VerifyOtpRequest(USER_ID, OTP)))
            .isInstanceOf(InvalidTokenException.class)
            .hasMessageContaining("attempts");
    }

    @Test
    void verifyOtp_expiredOtp_throws() {
        when(valueOperations.get("otp:attempts:" + USER_ID)).thenReturn(null);
        when(valueOperations.get("otp:" + USER_ID)).thenReturn(null);

        assertThatThrownBy(() -> authService.verifyOtp(new VerifyOtpRequest(USER_ID, OTP)))
            .isInstanceOf(InvalidTokenException.class)
            .hasMessageContaining("expired");
    }

    @Test
    void verifyOtp_wrongCode_incrementsAttempts_throws() {
        when(valueOperations.get("otp:attempts:" + USER_ID)).thenReturn(1);
        when(valueOperations.get("otp:" + USER_ID)).thenReturn(OTP);

        assertThatThrownBy(() -> authService.verifyOtp(new VerifyOtpRequest(USER_ID, "000000")))
            .isInstanceOf(InvalidTokenException.class)
            .hasMessageContaining("Incorrect");

        verify(valueOperations).increment("otp:attempts:" + USER_ID);
    }

    // ════════════════════════════════════════════════════════════════════════
    // LOGIN
    // ════════════════════════════════════════════════════════════════════════

    @Test
    void login_success_returnsBothTokens_storesRefreshInRedis() {
        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(activeUser()));
        when(passwordEncoder.matches(RAW_PASSWORD, HASHED_PW)).thenReturn(true);
        when(jwtUtil.generateAccessToken(USER_ID, UserRole.EVENT_ORGANIZER, 900_000L)).thenReturn(ACCESS_TOKEN);
        when(jwtUtil.generateRefreshToken(USER_ID, 604_800_000L)).thenReturn(REFRESH_TOKEN);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = authService.login(new LoginRequest(USERNAME, RAW_PASSWORD));

        assertThat(response.accessToken()).isEqualTo(ACCESS_TOKEN);
        assertThat(response.refreshToken()).isEqualTo(REFRESH_TOKEN);
        verify(valueOperations).set(eq("refresh:" + USER_ID), eq(REFRESH_TOKEN), eq(604_800_000L), eq(TimeUnit.MILLISECONDS));
    }

    @Test
    void login_wrongPassword_throws() {
        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(activeUser()));
        when(passwordEncoder.matches(anyString(), eq(HASHED_PW))).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest(USERNAME, "WrongPass!")))
            .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void login_pendingVerification_throws() {
        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(pendingUser()));
        when(passwordEncoder.matches(RAW_PASSWORD, HASHED_PW)).thenReturn(true);

        assertThatThrownBy(() -> authService.login(new LoginRequest(USERNAME, RAW_PASSWORD)))
            .isInstanceOf(TurnoutException.class)
            .hasMessageContaining("verified");
    }

    @Test
    void login_suspendedAccount_throws() {
        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(suspendedUser()));
        when(passwordEncoder.matches(RAW_PASSWORD, HASHED_PW)).thenReturn(true);

        assertThatThrownBy(() -> authService.login(new LoginRequest(USERNAME, RAW_PASSWORD)))
            .isInstanceOf(TurnoutException.class)
            .hasMessageContaining("suspended");
    }

    // ════════════════════════════════════════════════════════════════════════
    // TOKEN REFRESH
    // ════════════════════════════════════════════════════════════════════════

    @Test
    void refresh_validToken_returnsNewTokenPair() {
        when(jwtUtil.validateToken(REFRESH_TOKEN)).thenReturn(true);
        when(jwtUtil.extractUserId(REFRESH_TOKEN)).thenReturn(USER_ID);
        when(valueOperations.get("refresh:" + USER_ID)).thenReturn(REFRESH_TOKEN);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(activeUser()));
        when(jwtUtil.generateAccessToken(USER_ID, UserRole.EVENT_ORGANIZER, 900_000L)).thenReturn(ACCESS_TOKEN);
        when(jwtUtil.generateRefreshToken(USER_ID, 604_800_000L)).thenReturn("eyJ.new.refresh");

        var response = authService.refresh(new RefreshTokenRequest(REFRESH_TOKEN));

        assertThat(response.accessToken()).isEqualTo(ACCESS_TOKEN);
        verify(valueOperations).set(eq("refresh:" + USER_ID), eq("eyJ.new.refresh"), anyLong(), any());
    }

    @Test
    void refresh_revokedToken_throws() {
        when(jwtUtil.validateToken(REFRESH_TOKEN)).thenReturn(true);
        when(jwtUtil.extractUserId(REFRESH_TOKEN)).thenReturn(USER_ID);
        when(valueOperations.get("refresh:" + USER_ID)).thenReturn(null);

        assertThatThrownBy(() -> authService.refresh(new RefreshTokenRequest(REFRESH_TOKEN)))
            .isInstanceOf(InvalidTokenException.class)
            .hasMessageContaining("rotated");
    }

    @Test
    void refresh_expiredToken_throws() {
        when(jwtUtil.validateToken(REFRESH_TOKEN)).thenReturn(false);

        assertThatThrownBy(() -> authService.refresh(new RefreshTokenRequest(REFRESH_TOKEN)))
            .isInstanceOf(InvalidTokenException.class)
            .hasMessageContaining("invalid or expired");
    }

    // ════════════════════════════════════════════════════════════════════════
    // LOGOUT
    // ════════════════════════════════════════════════════════════════════════

    @Test
    void logout_blacklistsJti_deletesRefreshKey() {
        authService.logout(USER_ID.toString());
        verify(redisTemplate).delete("refresh:" + USER_ID);
    }

    // ════════════════════════════════════════════════════════════════════════
    // RESET PASSWORD
    // ════════════════════════════════════════════════════════════════════════

    @Test
    void resetPassword_success_updatesHash_invalidatesToken() {
        String resetToken = "valid-reset-token";
        String newPassword = "NewSecurePass456!";

        when(jwtUtil.validateToken(resetToken)).thenReturn(true);
        when(valueOperations.get("reset:" + resetToken)).thenReturn(USER_ID.toString());
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(activeUser()));
        when(passwordEncoder.encode(newPassword)).thenReturn("$2a$10$newhash");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        authService.resetPassword(new ResetPasswordRequest(resetToken, newPassword));

        verify(userRepository).save(argThat(u -> u.getPassword().equals("$2a$10$newhash")));
        verify(redisTemplate).delete("reset:" + resetToken);
        verify(redisTemplate).delete("refresh:" + USER_ID);
    }

    @Test
    void resetPassword_expiredToken_throws() {
        when(jwtUtil.validateToken("expired-token")).thenReturn(false);

        assertThatThrownBy(() -> authService.resetPassword(new ResetPasswordRequest("expired-token", "NewPass123!")))
            .isInstanceOf(InvalidTokenException.class)
            .hasMessageContaining("invalid or expired");
    }

    @Test
    void resetPassword_alreadyUsedToken_throws() {
        when(jwtUtil.validateToken("used-token")).thenReturn(true);
        when(valueOperations.get("reset:used-token")).thenReturn(null);

        assertThatThrownBy(() -> authService.resetPassword(new ResetPasswordRequest("used-token", "NewPass123!")))
            .isInstanceOf(InvalidTokenException.class)
            .hasMessageContaining("already been used");
    }

    // ════════════════════════════════════════════════════════════════════════
    // Test data builders
    // ════════════════════════════════════════════════════════════════════════

    private RegisterRequest buildRegisterRequest() {
        // Match the exact field order of your RegisterRequest record
        return new RegisterRequest(USERNAME, EMAIL, FULL_NAME, RAW_PASSWORD);
    }

    private User pendingUser() {
        User u = new User();
        ReflectionTestUtils.setField(u, "id", USER_ID);
        u.setEmail(EMAIL);
        u.setUsername(USERNAME);
        u.setFullName(FULL_NAME);
        u.setPassword(HASHED_PW);
        u.setRole(UserRole.EVENT_ORGANIZER);
        u.setStatus(AccountStatus.PENDING_VERIFICATION);
        u.setEmailVerified(false);
        return u;
    }

    private User activeUser() {
        User u = pendingUser();
        u.setStatus(AccountStatus.ACTIVE);
        u.setEmailVerified(true);
        return u;
    }

    private User suspendedUser() {
        User u = activeUser();
        u.setStatus(AccountStatus.SUSPENDED);
        return u;
    }
}
