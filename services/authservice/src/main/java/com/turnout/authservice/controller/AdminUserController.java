package com.turnout.authservice.controller;

import com.turnout.authservice.dto.*;
import com.turnout.authservice.service.AdminUserService;
import com.turnout.common.dto.ApiResponse;
import com.turnout.common.enums.AccountStatus;
import com.turnout.common.exception.UnauthorizedAccessException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
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

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<OrganizerSummaryResponse>>> listOrganizers(
            @RequestHeader("X-User-Role") String role,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) AccountStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        requireAdminOrSuperAdmin(role);
        return ResponseEntity.ok(ApiResponse.success(
                "Organizers retrieved.",
                adminUserService.listOrganizers(search, status, PageRequest.of(page, size))));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrganizerDetailResponse>> getOrganizer(
            @RequestHeader("X-User-Role") String role,
            @PathVariable UUID id) {

        requireAdminOrSuperAdmin(role);
        return ResponseEntity.ok(ApiResponse.success(
                "Organizer retrieved.", adminUserService.getOrganizer(id)));
    }

    @PatchMapping("/{id}/suspend")
    public ResponseEntity<ApiResponse<OrganizerSummaryResponse>> suspendOrganizer(
            @RequestHeader("X-User-Role") String role,
            @PathVariable UUID id,
            @RequestBody SuspendRequest request) {

        requireAdminOrSuperAdmin(role);
        return ResponseEntity.ok(ApiResponse.success(
                "Account status updated.", adminUserService.suspendOrganizer(id, request.suspend())));
    }
}
