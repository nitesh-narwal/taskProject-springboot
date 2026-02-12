package com.example.shopapi.controller;

import com.example.shopapi.dto.admin.CreateWorkerRequest;
import com.example.shopapi.dto.admin.SystemStatsResponse;
import com.example.shopapi.dto.common.ApiResponse;
import com.example.shopapi.dto.common.PagedResponse;
import com.example.shopapi.dto.user.UserProfileResponse;
import com.example.shopapi.entity.AuditLog;
import com.example.shopapi.entity.Role;
import com.example.shopapi.service.AdminService;
import com.example.shopapi.service.AuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Admin management endpoints")
public class AdminController {

    private final AdminService adminService;
    private final AuditService auditService;

    @PostMapping("/workers")
    @Operation(summary = "Create a new worker account")
    public ResponseEntity<ApiResponse<UserProfileResponse>> createWorker(@Valid @RequestBody CreateWorkerRequest request) {
        UserProfileResponse worker = adminService.createWorker(request);
        return ResponseEntity.ok(ApiResponse.success("Worker created successfully", worker));
    }

    @GetMapping("/workers")
    @Operation(summary = "Get all workers")
    public ResponseEntity<ApiResponse<List<UserProfileResponse>>> getWorkers() {
        List<UserProfileResponse> workers = adminService.getWorkers();
        return ResponseEntity.ok(ApiResponse.success(workers));
    }

    @GetMapping("/users")
    @Operation(summary = "Get all users with pagination")
    public ResponseEntity<PagedResponse<UserProfileResponse>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<UserProfileResponse> users = adminService.getAllUsers(
                PageRequest.of(page, size, Sort.by("createdAt").descending()));
        return ResponseEntity.ok(PagedResponse.of(users.getContent(), page, size, users.getTotalElements()));
    }

    @PatchMapping("/users/{userId}/enable")
    @Operation(summary = "Enable a user account")
    public ResponseEntity<ApiResponse<UserProfileResponse>> enableUser(@PathVariable Long userId) {
        UserProfileResponse user = adminService.toggleUserStatus(userId, true);
        return ResponseEntity.ok(ApiResponse.success("User enabled", user));
    }

    @PatchMapping("/users/{userId}/disable")
    @Operation(summary = "Disable a user account")
    public ResponseEntity<ApiResponse<UserProfileResponse>> disableUser(@PathVariable Long userId) {
        UserProfileResponse user = adminService.toggleUserStatus(userId, false);
        return ResponseEntity.ok(ApiResponse.success("User disabled", user));
    }

    @GetMapping("/stats")
    @Operation(summary = "Get system statistics")
    public ResponseEntity<ApiResponse<SystemStatsResponse>> getStats() {
        SystemStatsResponse stats = adminService.getSystemStats();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @GetMapping("/audit-logs")
    @Operation(summary = "Get audit logs")
    public ResponseEntity<PagedResponse<AuditLog>> getAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        Page<AuditLog> logs = auditService.getAuditLogs(
                PageRequest.of(page, size, Sort.by("timestamp").descending()));
        return ResponseEntity.ok(PagedResponse.of(logs.getContent(), page, size, logs.getTotalElements()));
    }

    @GetMapping("/audit-logs/user/{username}")
    @Operation(summary = "Get audit logs by user")
    public ResponseEntity<PagedResponse<AuditLog>> getAuditLogsByUser(
            @PathVariable String username,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        Page<AuditLog> logs = auditService.getAuditLogsByUser(username,
                PageRequest.of(page, size, Sort.by("timestamp").descending()));
        return ResponseEntity.ok(PagedResponse.of(logs.getContent(), page, size, logs.getTotalElements()));
    }

    @GetMapping("/audit-logs/failed")
    @Operation(summary = "Get failed action logs")
    public ResponseEntity<PagedResponse<AuditLog>> getFailedActions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        Page<AuditLog> logs = auditService.getFailedActions(
                PageRequest.of(page, size, Sort.by("timestamp").descending()));
        return ResponseEntity.ok(PagedResponse.of(logs.getContent(), page, size, logs.getTotalElements()));
    }
}

