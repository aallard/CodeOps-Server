package com.codeops.controller;

import com.codeops.config.AppConstants;
import com.codeops.dto.request.AdminUpdateUserRequest;
import com.codeops.dto.request.UpdateSystemSettingRequest;
import com.codeops.dto.response.AuditLogResponse;
import com.codeops.dto.response.SystemSettingResponse;
import com.codeops.dto.response.UserResponse;
import com.codeops.security.SecurityUtils;
import com.codeops.service.AdminService;
import com.codeops.service.AuditLogService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN') or hasRole('OWNER')")
@Tag(name = "Admin")
public class AdminController {

    private final AdminService adminService;
    private final AuditLogService auditLogService;

    @GetMapping("/users")
    public ResponseEntity<Page<UserResponse>> getAllUsers(@RequestParam(defaultValue = "0") int page,
                                                          @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(adminService.getAllUsers(PageRequest.of(page, Math.min(size, AppConstants.MAX_PAGE_SIZE))));
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable UUID userId) {
        return ResponseEntity.ok(adminService.getUserById(userId));
    }

    @PutMapping("/users/{userId}")
    public ResponseEntity<UserResponse> updateUserStatus(@PathVariable UUID userId,
                                                          @Valid @RequestBody AdminUpdateUserRequest request) {
        UserResponse response = adminService.updateUserStatus(userId, request);
        auditLogService.log(SecurityUtils.getCurrentUserId(), null, "ADMIN_USER_UPDATED", "USER", userId, null);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/settings")
    public ResponseEntity<List<SystemSettingResponse>> getAllSettings() {
        return ResponseEntity.ok(adminService.getAllSettings());
    }

    @GetMapping("/settings/{key}")
    public ResponseEntity<SystemSettingResponse> getSystemSetting(@PathVariable String key) {
        return ResponseEntity.ok(adminService.getSystemSetting(key));
    }

    @PutMapping("/settings")
    public ResponseEntity<SystemSettingResponse> updateSystemSetting(@Valid @RequestBody UpdateSystemSettingRequest request) {
        SystemSettingResponse response = adminService.updateSystemSetting(request);
        auditLogService.log(SecurityUtils.getCurrentUserId(), null, "SYSTEM_SETTING_UPDATED", "SYSTEM_SETTING", null, request.key());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/usage")
    public ResponseEntity<Map<String, Object>> getUsageStats() {
        return ResponseEntity.ok(adminService.getUsageStats());
    }

    @GetMapping("/audit-log/team/{teamId}")
    public ResponseEntity<Page<AuditLogResponse>> getTeamAuditLog(@PathVariable UUID teamId,
                                                                    @RequestParam(defaultValue = "0") int page,
                                                                    @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, AppConstants.MAX_PAGE_SIZE));
        return ResponseEntity.ok(auditLogService.getTeamAuditLog(teamId, pageable));
    }

    @GetMapping("/audit-log/user/{userId}")
    public ResponseEntity<Page<AuditLogResponse>> getUserAuditLog(@PathVariable UUID userId,
                                                                    @RequestParam(defaultValue = "0") int page,
                                                                    @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, AppConstants.MAX_PAGE_SIZE));
        return ResponseEntity.ok(auditLogService.getUserAuditLog(userId, pageable));
    }
}
