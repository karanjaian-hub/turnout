package com.turnout.authservice.controller;

import com.turnout.authservice.dto.CreateAdminRequest;
import com.turnout.authservice.dto.RegisterResponse;
import com.turnout.authservice.service.AdminUserService;
import com.turnout.common.dto.ApiResponse;
import com.turnout.common.exception.UnauthorizedAccessException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminUserService adminUserService;

    @PostMapping("/create-admin")
    public ResponseEntity<ApiResponse<RegisterResponse>> createAdmin(
            @RequestHeader("X-User-Role") String role,
            @Valid @RequestBody CreateAdminRequest request) {
        if (!"SUPER_ADMIN".equals(role)) {
            throw new UnauthorizedAccessException("Super admin access required");
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Admin account created.", adminUserService.createAdmin(request)));
    }
}
