package com.codeops.controller;

import com.codeops.dto.request.AdminUpdateUserRequest;
import com.codeops.dto.request.UpdateSystemSettingRequest;
import com.codeops.dto.response.AuditLogResponse;
import com.codeops.dto.response.SystemSettingResponse;
import com.codeops.dto.response.UserResponse;
import com.codeops.service.AdminService;
import com.codeops.service.AuditLogService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    @Mock
    private AdminService adminService;

    @Mock
    private AuditLogService auditLogService;

    private AdminController controller;

    private final UUID currentUserId = UUID.randomUUID();
    private final UUID targetUserId = UUID.randomUUID();
    private final UUID teamId = UUID.randomUUID();
    private final Instant now = Instant.now();

    @BeforeEach
    void setUp() {
        controller = new AdminController(adminService, auditLogService);
        setSecurityContext(currentUserId);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void setSecurityContext(UUID userId) {
        var auth = new UsernamePasswordAuthenticationToken(userId, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private UserResponse userResponse(UUID id) {
        return new UserResponse(id, "user@example.com", "Test User", null, true, now, now, false);
    }

    @Test
    void getAllUsers_returns200WithPage() {
        Page<UserResponse> expected = new PageImpl<>(List.of(userResponse(targetUserId)),
                PageRequest.of(0, 20), 1);
        when(adminService.getAllUsers(any(Pageable.class))).thenReturn(expected);

        ResponseEntity<Page<UserResponse>> response = controller.getAllUsers(0, 20);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(expected, response.getBody());
    }

    @Test
    void getAllUsers_clampsSizeToMaxPageSize() {
        Page<UserResponse> expected = new PageImpl<>(List.of(), PageRequest.of(0, 100), 0);
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        when(adminService.getAllUsers(any(Pageable.class))).thenReturn(expected);

        controller.getAllUsers(0, 500);

        verify(adminService).getAllUsers(captor.capture());
        assertEquals(100, captor.getValue().getPageSize());
    }

    @Test
    void getUserById_returns200WithUser() {
        UserResponse expected = userResponse(targetUserId);
        when(adminService.getUserById(targetUserId)).thenReturn(expected);

        ResponseEntity<UserResponse> response = controller.getUserById(targetUserId);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(expected, response.getBody());
        verify(adminService).getUserById(targetUserId);
    }

    @Test
    void updateUserStatus_returns200WithUpdatedUserAndLogsAudit() {
        AdminUpdateUserRequest request = new AdminUpdateUserRequest(false);
        UserResponse expected = new UserResponse(targetUserId, "user@example.com", "Test User", null, false, now, now, false);
        when(adminService.updateUserStatus(targetUserId, request)).thenReturn(expected);

        ResponseEntity<UserResponse> response = controller.updateUserStatus(targetUserId, request);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(expected, response.getBody());
        verify(adminService).updateUserStatus(targetUserId, request);
        verify(auditLogService).log(currentUserId, null, "ADMIN_USER_UPDATED", "USER", targetUserId, null);
    }

    @Test
    void getAllSettings_returns200WithList() {
        List<SystemSettingResponse> expected = List.of(
                new SystemSettingResponse("key1", "value1", currentUserId, now));
        when(adminService.getAllSettings()).thenReturn(expected);

        ResponseEntity<List<SystemSettingResponse>> response = controller.getAllSettings();

        assertEquals(200, response.getStatusCode().value());
        assertEquals(expected, response.getBody());
        verify(adminService).getAllSettings();
    }

    @Test
    void getSystemSetting_returns200WithSetting() {
        SystemSettingResponse expected = new SystemSettingResponse("max.users", "100", currentUserId, now);
        when(adminService.getSystemSetting("max.users")).thenReturn(expected);

        ResponseEntity<SystemSettingResponse> response = controller.getSystemSetting("max.users");

        assertEquals(200, response.getStatusCode().value());
        assertEquals(expected, response.getBody());
        verify(adminService).getSystemSetting("max.users");
    }

    @Test
    void updateSystemSetting_returns200WithUpdatedSettingAndLogsAudit() {
        UpdateSystemSettingRequest request = new UpdateSystemSettingRequest("max.users", "200");
        SystemSettingResponse expected = new SystemSettingResponse("max.users", "200", currentUserId, now);
        when(adminService.updateSystemSetting(request)).thenReturn(expected);

        ResponseEntity<SystemSettingResponse> response = controller.updateSystemSetting(request);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(expected, response.getBody());
        verify(adminService).updateSystemSetting(request);
        verify(auditLogService).log(currentUserId, null, "SYSTEM_SETTING_UPDATED", "SYSTEM_SETTING", null, "max.users");
    }

    @Test
    void getUsageStats_returns200WithMap() {
        Map<String, Object> expected = Map.of(
                "totalUsers", 10L,
                "activeUsers", 8L,
                "totalTeams", 3L,
                "totalProjects", 5L,
                "totalJobs", 20L);
        when(adminService.getUsageStats()).thenReturn(expected);

        ResponseEntity<Map<String, Object>> response = controller.getUsageStats();

        assertEquals(200, response.getStatusCode().value());
        assertEquals(expected, response.getBody());
        verify(adminService).getUsageStats();
    }

    @Test
    void getTeamAuditLog_returns200WithPage() {
        Page<AuditLogResponse> expected = new PageImpl<>(List.of(
                new AuditLogResponse(1L, currentUserId, null, teamId, "TEAM_CREATED", "TEAM", teamId, null, null, now)),
                PageRequest.of(0, 20), 1);
        when(auditLogService.getTeamAuditLog(eq(teamId), any(Pageable.class))).thenReturn(expected);

        ResponseEntity<Page<AuditLogResponse>> response = controller.getTeamAuditLog(teamId, 0, 20);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(expected, response.getBody());
    }

    @Test
    void getTeamAuditLog_clampsSizeToMaxPageSize() {
        Page<AuditLogResponse> expected = new PageImpl<>(List.of(), PageRequest.of(0, 100), 0);
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        when(auditLogService.getTeamAuditLog(eq(teamId), any(Pageable.class))).thenReturn(expected);

        controller.getTeamAuditLog(teamId, 0, 500);

        verify(auditLogService).getTeamAuditLog(eq(teamId), captor.capture());
        assertEquals(100, captor.getValue().getPageSize());
    }

    @Test
    void getUserAuditLog_returns200WithPage() {
        Page<AuditLogResponse> expected = new PageImpl<>(List.of(
                new AuditLogResponse(2L, targetUserId, null, null, "USER_LOGIN", "USER", targetUserId, null, null, now)),
                PageRequest.of(0, 20), 1);
        when(auditLogService.getUserAuditLog(eq(targetUserId), any(Pageable.class))).thenReturn(expected);

        ResponseEntity<Page<AuditLogResponse>> response = controller.getUserAuditLog(targetUserId, 0, 20);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(expected, response.getBody());
    }
}
