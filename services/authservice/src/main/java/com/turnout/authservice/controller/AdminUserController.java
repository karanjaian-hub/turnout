package com.turnout.authservice.controller;

import com.turnout.authservice.dto.*;
import com.turnout.authservice.entity.User;
import com.turnout.authservice.repository.UserRepository;
import com.turnout.authservice.service.AdminUserService;
import com.turnout.common.enums.AccountStatus;
import com.turnout.common.exception.ResourceNotFoundException;
import com.turnout.common.exception.UnauthorizedAccessException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    private void requireAdminOrSuperAdmin(String role) {
        if (!"ADMIN".equals(role) && !"SUPER_ADMIN".equals(role)) {
            throw new UnauthorizedAccessException("Admin access required");
        }
    }

    private void requireSuperAdmin(String role) {
        if (!"SUPER_ADMIN".equals(role)) {
            throw new UnauthorizedAccessException("Super admin access required");
        }
    }

    @GetMapping
    public ResponseEntity<PagedResponse<OrganizerSummaryResponse>> listOrganizers(
            @RequestHeader("X-User-Role") String role,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) AccountStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        requireAdminOrSuperAdmin(role);
        return ResponseEntity.ok(
                adminUserService.listOrganizers(search, status, PageRequest.of(page, size)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrganizerDetailResponse> getOrganizer(
            @RequestHeader("X-User-Role") String role,
            @PathVariable UUID id) {

        requireAdminOrSuperAdmin(role);
        return ResponseEntity.ok(adminUserService.getOrganizer(id));
    }

    @PatchMapping("/{id}/suspend")
    public ResponseEntity<OrganizerSummaryResponse> suspendOrganizer(
            @RequestHeader("X-User-Role") String role,
            @PathVariable UUID id,
            @RequestBody SuspendRequest request) {

        requireAdminOrSuperAdmin(role);
        return ResponseEntity.ok(adminUserService.suspendOrganizer(id, request.suspend()));
    }
}
