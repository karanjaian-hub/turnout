package com.turnout.authservice.service;

import com.turnout.authservice.dto.*;
import com.turnout.authservice.entity.User;
import com.turnout.authservice.repository.UserRepository;
import com.turnout.common.enums.AccountStatus;
import com.turnout.common.enums.UserRole;
import com.turnout.common.exception.DuplicateResourceException;
import com.turnout.common.exception.ResourceNotFoundException;
import com.turnout.common.exception.TurnoutException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final BrevoEmailService brevoEmailService;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String KEY_BLACKLIST = "blacklist:access:user:";

    public PagedResponse<OrganizerSummaryResponse> listOrganizers(
            String search, AccountStatus status, Pageable pageable) {

        Page<User> page = userRepository.searchOrganizers(
                UserRole.EVENT_ORGANIZER.name(), status != null ? status.name() : null, search, pageable);

        return new PagedResponse<>(
                page.getContent().stream().map(this::toSummary).toList(),
                page.getNumber(),
                page.getTotalPages(),
                page.getTotalElements()
        );
    }

    public OrganizerDetailResponse getOrganizer(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Organizer", id));

        if (user.getRole() != UserRole.EVENT_ORGANIZER) {
            throw new ResourceNotFoundException("Organizer", id);
        }

        return toDetail(user);
    }

    @Transactional
    public OrganizerSummaryResponse suspendOrganizer(UUID id, boolean suspend) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Organizer", id));

        if (user.getRole() != UserRole.EVENT_ORGANIZER) {
            throw new TurnoutException(
                    "Cannot suspend admin accounts", "INVALID_TARGET");
        }

        user.setStatus(suspend ? AccountStatus.SUSPENDED : AccountStatus.ACTIVE);
        userRepository.save(user);

        if (suspend) {
// Blacklist all tokens for this user by marking their userId in Redis.
// The gateway checks this key on every request and rejects if present.
            redisTemplate.opsForValue().set(
                    KEY_BLACKLIST + id,
                    "suspended",
                    30,
                    TimeUnit.DAYS
            );
            log.info("User {} suspended — tokens blacklisted", id);
        } else {
            redisTemplate.delete(KEY_BLACKLIST + id);
        }

        return toSummary(user);
    }

    @Transactional
    public RegisterResponse createAdmin(CreateAdminRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("User", "email", request.email());
        }
        if (userRepository.existsByUsername(request.username())) {
            throw new DuplicateResourceException("User", "username", request.username());
        }

        User admin = new User();
        admin.setUsername(request.username());
        admin.setEmail(request.email());
        admin.setFullName(request.fullName());
        admin.setPassword(passwordEncoder.encode(request.password()));
        admin.setRole(UserRole.ADMIN);
        admin.setStatus(AccountStatus.ACTIVE);
        admin.setEmailVerified(true); // no OTP for admin creation

        User saved = userRepository.save(admin);
        brevoEmailService.sendWelcomeEmail(saved.getEmail(), saved.getFullName());

        return new RegisterResponse(
                saved.getId(),
                saved.getUsername(),
                saved.getEmail(),
                "Admin account created successfully."
        );
    }

// Mappers
    private OrganizerSummaryResponse toSummary(User user) {
        return new OrganizerSummaryResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getStatus().name(),
                user.getCreatedAt(),
                user.getLastLoginAt()
        );
    }

    private OrganizerDetailResponse toDetail(User user) {
        return new OrganizerDetailResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getRole().name(),
                user.getStatus().name(),
                user.isEmailVerified(),
                user.getCreatedAt(),
                user.getLastLoginAt()
        );
    }
}
