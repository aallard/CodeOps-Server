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
import com.codeops.service.MfaService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for administrative operations including user management,
 * system settings, usage statistics, and audit log retrieval.
 *
 * <p>All endpoints require the caller to have either the {@code ADMIN} or
 * {@code OWNER} role, enforced at the class level via {@code @PreAuthorize}.</p>
 *
 * @see AdminService
 * @see AuditLogService
 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN') or hasRole('OWNER')")
@Tag(name = "Admin")
public class AdminController {

    private static final Logger log = LoggerFactory.getLogger(AdminController.class);

    private final AdminService adminService;
    private final AuditLogService auditLogService;
    private final MfaService mfaService;

    /**
     * Retrieves a paginated list of all users in the system.
     *
     * <p>GET {@code /api/v1/admin/users}</p>
     *
     * @param page zero-based page index (defaults to 0)
     * @param size number of items per page (defaults to 20, capped at {@link AppConstants#MAX_PAGE_SIZE})
     * @return paginated list of user responses
     */
    @GetMapping("/users")
    public ResponseEntity<Page<UserResponse>> getAllUsers(@RequestParam(defaultValue = "0") int page,
                                                          @RequestParam(defaultValue = "20") int size) {
        log.debug("getAllUsers called with page={}, size={}", page, size);
        return ResponseEntity.ok(adminService.getAllUsers(PageRequest.of(page, Math.min(size, AppConstants.MAX_PAGE_SIZE))));
    }

    /**
     * Retrieves a single user by their unique identifier.
     *
     * <p>GET {@code /api/v1/admin/users/{userId}}</p>
     *
     * @param userId the UUID of the user to retrieve
     * @return the user details
     */
    @GetMapping("/users/{userId}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable UUID userId) {
        log.debug("getUserById called with userId={}", userId);
        return ResponseEntity.ok(adminService.getUserById(userId));
    }

    /**
     * Updates a user's status or administrative properties (e.g., role, active flag).
     *
     * <p>PUT {@code /api/v1/admin/users/{userId}}</p>
     *
     * <p>Side effect: logs an {@code ADMIN_USER_UPDATED} audit entry for the target user.</p>
     *
     * @param userId  the UUID of the user to update
     * @param request the update payload containing the new user properties
     * @return the updated user details
     */
    @PutMapping("/users/{userId}")
    public ResponseEntity<UserResponse> updateUserStatus(@PathVariable UUID userId,
                                                          @Valid @RequestBody AdminUpdateUserRequest request) {
        log.debug("updateUserStatus called with userId={}", userId);
        UserResponse response = adminService.updateUserStatus(userId, request);
        auditLogService.log(SecurityUtils.getCurrentUserId(), null, "ADMIN_USER_UPDATED", "USER", userId, null);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves all system-level settings.
     *
     * <p>GET {@code /api/v1/admin/settings}</p>
     *
     * @return list of all system settings
     */
    @GetMapping("/settings")
    public ResponseEntity<List<SystemSettingResponse>> getAllSettings() {
        log.debug("getAllSettings called");
        return ResponseEntity.ok(adminService.getAllSettings());
    }

    /**
     * Retrieves a single system setting by its key.
     *
     * <p>GET {@code /api/v1/admin/settings/{key}}</p>
     *
     * @param key the unique key identifying the system setting
     * @return the system setting details
     */
    @GetMapping("/settings/{key}")
    public ResponseEntity<SystemSettingResponse> getSystemSetting(@PathVariable String key) {
        log.debug("getSystemSetting called with key={}", key);
        return ResponseEntity.ok(adminService.getSystemSetting(key));
    }

    /**
     * Updates a system-level setting.
     *
     * <p>PUT {@code /api/v1/admin/settings}</p>
     *
     * <p>Side effect: logs a {@code SYSTEM_SETTING_UPDATED} audit entry with the setting key.</p>
     *
     * @param request the update payload containing the setting key and new value
     * @return the updated system setting
     */
    @PutMapping("/settings")
    public ResponseEntity<SystemSettingResponse> updateSystemSetting(@Valid @RequestBody UpdateSystemSettingRequest request) {
        log.debug("updateSystemSetting called");
        SystemSettingResponse response = adminService.updateSystemSetting(request);
        auditLogService.log(SecurityUtils.getCurrentUserId(), null, "SYSTEM_SETTING_UPDATED", "SYSTEM_SETTING", null, request.key());
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves aggregate usage statistics for the platform.
     *
     * <p>GET {@code /api/v1/admin/usage}</p>
     *
     * @return a map of usage metric names to their values
     */
    @GetMapping("/usage")
    public ResponseEntity<Map<String, Object>> getUsageStats() {
        log.debug("getUsageStats called");
        return ResponseEntity.ok(adminService.getUsageStats());
    }

    /**
     * Retrieves a paginated audit log for a specific team.
     *
     * <p>GET {@code /api/v1/admin/audit-log/team/{teamId}}</p>
     *
     * @param teamId the UUID of the team whose audit log to retrieve
     * @param page   zero-based page index (defaults to 0)
     * @param size   number of items per page (defaults to 20, capped at {@link AppConstants#MAX_PAGE_SIZE})
     * @return paginated list of audit log entries for the team
     */
    @GetMapping("/audit-log/team/{teamId}")
    public ResponseEntity<Page<AuditLogResponse>> getTeamAuditLog(@PathVariable UUID teamId,
                                                                    @RequestParam(defaultValue = "0") int page,
                                                                    @RequestParam(defaultValue = "20") int size) {
        log.debug("getTeamAuditLog called with teamId={}, page={}, size={}", teamId, page, size);
        Pageable pageable = PageRequest.of(page, Math.min(size, AppConstants.MAX_PAGE_SIZE));
        return ResponseEntity.ok(auditLogService.getTeamAuditLog(teamId, pageable));
    }

    /**
     * Retrieves a paginated audit log for a specific user.
     *
     * <p>GET {@code /api/v1/admin/audit-log/user/{userId}}</p>
     *
     * @param userId the UUID of the user whose audit log to retrieve
     * @param page   zero-based page index (defaults to 0)
     * @param size   number of items per page (defaults to 20, capped at {@link AppConstants#MAX_PAGE_SIZE})
     * @return paginated list of audit log entries for the user
     */
    @GetMapping("/audit-log/user/{userId}")
    public ResponseEntity<Page<AuditLogResponse>> getUserAuditLog(@PathVariable UUID userId,
                                                                    @RequestParam(defaultValue = "0") int page,
                                                                    @RequestParam(defaultValue = "20") int size) {
        log.debug("getUserAuditLog called with userId={}, page={}, size={}", userId, page, size);
        Pageable pageable = PageRequest.of(page, Math.min(size, AppConstants.MAX_PAGE_SIZE));
        return ResponseEntity.ok(auditLogService.getUserAuditLog(userId, pageable));
    }

    /**
     * Force-resets MFA for a target user without requiring the user's password.
     *
     * <p>POST {@code /api/v1/admin/users/{userId}/reset-mfa}</p>
     *
     * <p>This is an emergency operation for when a user is locked out of their account
     * due to lost MFA devices or corrupted MFA state. Side effect: logs an
     * {@code ADMIN_MFA_RESET} audit entry.</p>
     *
     * @param userId the UUID of the user whose MFA should be reset
     * @return HTTP 200 OK with confirmation message
     */
    @PostMapping("/users/{userId}/reset-mfa")
    public ResponseEntity<Map<String, Object>> resetUserMfa(@PathVariable UUID userId) {
        log.debug("resetUserMfa called with userId={}", userId);
        mfaService.adminResetMfa(userId);
        auditLogService.log(SecurityUtils.getCurrentUserId(), null, "ADMIN_MFA_RESET", "USER", userId, null);
        return ResponseEntity.ok(Map.of("message", "MFA reset successfully", "userId", userId));
    }
}
