package com.codeops.service;

import com.codeops.dto.request.AdminUpdateUserRequest;
import com.codeops.dto.request.UpdateSystemSettingRequest;
import com.codeops.dto.response.SystemSettingResponse;
import com.codeops.dto.response.UserResponse;
import com.codeops.entity.SystemSetting;
import com.codeops.entity.User;
import com.codeops.repository.*;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private TeamRepository teamRepository;
    @Mock private ProjectRepository projectRepository;
    @Mock private QaJobRepository qaJobRepository;
    @Mock private SystemSettingRepository systemSettingRepository;

    @InjectMocks
    private AdminService adminService;

    private UUID adminUserId;
    private UUID regularUserId;
    private User adminUser;
    private User regularUser;

    @BeforeEach
    void setUp() {
        adminUserId = UUID.randomUUID();
        regularUserId = UUID.randomUUID();

        adminUser = User.builder()
                .email("admin@codeops.dev")
                .passwordHash("hash")
                .displayName("Admin User")
                .isActive(true)
                .build();
        adminUser.setId(adminUserId);
        adminUser.setCreatedAt(Instant.now());

        regularUser = User.builder()
                .email("user@codeops.dev")
                .passwordHash("hash")
                .displayName("Regular User")
                .isActive(true)
                .build();
        regularUser.setId(regularUserId);
        regularUser.setCreatedAt(Instant.now());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // --- getAllUsers ---

    @Test
    void getAllUsers_admin_success() {
        setSecurityContextWithRole(adminUserId, "ADMIN");
        Pageable pageable = PageRequest.of(0, 20);
        Page<User> userPage = new PageImpl<>(List.of(adminUser, regularUser), pageable, 2);

        when(userRepository.findAll(pageable)).thenReturn(userPage);

        Page<UserResponse> result = adminService.getAllUsers(pageable);

        assertEquals(2, result.getTotalElements());
        assertEquals("admin@codeops.dev", result.getContent().get(0).email());
        assertEquals("user@codeops.dev", result.getContent().get(1).email());
    }

    @Test
    void getAllUsers_ownerRole_success() {
        setSecurityContextWithRole(adminUserId, "OWNER");
        Pageable pageable = PageRequest.of(0, 20);
        Page<User> userPage = new PageImpl<>(List.of(adminUser), pageable, 1);

        when(userRepository.findAll(pageable)).thenReturn(userPage);

        Page<UserResponse> result = adminService.getAllUsers(pageable);

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getAllUsers_nonAdmin_throws() {
        setSecurityContext(regularUserId);
        Pageable pageable = PageRequest.of(0, 20);

        assertThrows(AccessDeniedException.class,
                () -> adminService.getAllUsers(pageable));
        verify(userRepository, never()).findAll(any(Pageable.class));
    }

    // --- getUserById ---

    @Test
    void getUserById_admin_success() {
        setSecurityContextWithRole(adminUserId, "ADMIN");
        when(userRepository.findById(regularUserId)).thenReturn(Optional.of(regularUser));

        UserResponse response = adminService.getUserById(regularUserId);

        assertEquals(regularUserId, response.id());
        assertEquals("user@codeops.dev", response.email());
        assertEquals("Regular User", response.displayName());
        assertTrue(response.isActive());
    }

    @Test
    void getUserById_ownerRole_success() {
        setSecurityContextWithRole(adminUserId, "OWNER");
        when(userRepository.findById(regularUserId)).thenReturn(Optional.of(regularUser));

        UserResponse response = adminService.getUserById(regularUserId);

        assertEquals(regularUserId, response.id());
    }

    @Test
    void getUserById_nonAdmin_throws() {
        setSecurityContext(regularUserId);

        assertThrows(AccessDeniedException.class,
                () -> adminService.getUserById(regularUserId));
        verify(userRepository, never()).findById(any(UUID.class));
    }

    @Test
    void getUserById_notFound_throws() {
        setSecurityContextWithRole(adminUserId, "ADMIN");
        UUID unknownId = UUID.randomUUID();
        when(userRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> adminService.getUserById(unknownId));
    }

    // --- updateUserStatus ---

    @Test
    void updateUserStatus_deactivate_success() {
        AdminUpdateUserRequest request = new AdminUpdateUserRequest(false);
        when(userRepository.findById(regularUserId)).thenReturn(Optional.of(regularUser));
        when(userRepository.save(any(User.class))).thenReturn(regularUser);

        UserResponse response = adminService.updateUserStatus(regularUserId, request);

        assertFalse(regularUser.getIsActive());
        verify(userRepository).save(regularUser);
    }

    @Test
    void updateUserStatus_activate_success() {
        regularUser.setIsActive(false);
        AdminUpdateUserRequest request = new AdminUpdateUserRequest(true);
        when(userRepository.findById(regularUserId)).thenReturn(Optional.of(regularUser));
        when(userRepository.save(any(User.class))).thenReturn(regularUser);

        UserResponse response = adminService.updateUserStatus(regularUserId, request);

        assertTrue(regularUser.getIsActive());
        verify(userRepository).save(regularUser);
    }

    @Test
    void updateUserStatus_nullIsActive_noChange() {
        AdminUpdateUserRequest request = new AdminUpdateUserRequest(null);
        when(userRepository.findById(regularUserId)).thenReturn(Optional.of(regularUser));
        when(userRepository.save(any(User.class))).thenReturn(regularUser);

        adminService.updateUserStatus(regularUserId, request);

        assertTrue(regularUser.getIsActive());
        verify(userRepository).save(regularUser);
    }

    @Test
    void updateUserStatus_userNotFound_throws() {
        UUID unknownId = UUID.randomUUID();
        AdminUpdateUserRequest request = new AdminUpdateUserRequest(false);
        when(userRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> adminService.updateUserStatus(unknownId, request));
        verify(userRepository, never()).save(any());
    }

    // --- getSystemSetting ---

    @Test
    void getSystemSetting_found_success() {
        SystemSetting setting = SystemSetting.builder()
                .settingKey("max.users")
                .value("100")
                .updatedBy(adminUser)
                .updatedAt(Instant.now())
                .build();

        when(systemSettingRepository.findById("max.users")).thenReturn(Optional.of(setting));

        SystemSettingResponse response = adminService.getSystemSetting("max.users");

        assertEquals("max.users", response.key());
        assertEquals("100", response.value());
        assertEquals(adminUserId, response.updatedBy());
        assertNotNull(response.updatedAt());
    }

    @Test
    void getSystemSetting_notFound_throws() {
        when(systemSettingRepository.findById("nonexistent")).thenReturn(Optional.empty());

        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class,
                () -> adminService.getSystemSetting("nonexistent"));
        assertTrue(ex.getMessage().contains("nonexistent"));
    }

    @Test
    void getSystemSetting_nullUpdatedBy_returnsNullUuid() {
        SystemSetting setting = SystemSetting.builder()
                .settingKey("feature.flag")
                .value("true")
                .updatedBy(null)
                .updatedAt(Instant.now())
                .build();

        when(systemSettingRepository.findById("feature.flag")).thenReturn(Optional.of(setting));

        SystemSettingResponse response = adminService.getSystemSetting("feature.flag");

        assertEquals("feature.flag", response.key());
        assertNull(response.updatedBy());
    }

    // --- updateSystemSetting ---

    @Test
    void updateSystemSetting_existingSetting_updates() {
        setSecurityContext(adminUserId);
        SystemSetting existing = SystemSetting.builder()
                .settingKey("max.users")
                .value("100")
                .updatedBy(adminUser)
                .updatedAt(Instant.now().minusSeconds(3600))
                .build();

        UpdateSystemSettingRequest request = new UpdateSystemSettingRequest("max.users", "200");

        when(systemSettingRepository.findById("max.users")).thenReturn(Optional.of(existing));
        when(userRepository.findById(adminUserId)).thenReturn(Optional.of(adminUser));
        when(systemSettingRepository.save(any(SystemSetting.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SystemSettingResponse response = adminService.updateSystemSetting(request);

        assertEquals("max.users", response.key());
        assertEquals("200", response.value());
        assertEquals(adminUserId, response.updatedBy());
        verify(systemSettingRepository).save(existing);
    }

    @Test
    void updateSystemSetting_newSetting_creates() {
        setSecurityContext(adminUserId);
        UpdateSystemSettingRequest request = new UpdateSystemSettingRequest("new.setting", "value123");

        when(systemSettingRepository.findById("new.setting")).thenReturn(Optional.empty());
        when(userRepository.findById(adminUserId)).thenReturn(Optional.of(adminUser));
        when(systemSettingRepository.save(any(SystemSetting.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SystemSettingResponse response = adminService.updateSystemSetting(request);

        assertEquals("new.setting", response.key());
        assertEquals("value123", response.value());
        assertEquals(adminUserId, response.updatedBy());
        verify(systemSettingRepository).save(any(SystemSetting.class));
    }

    @Test
    void updateSystemSetting_userNotFound_throws() {
        setSecurityContext(adminUserId);
        SystemSetting existing = SystemSetting.builder()
                .settingKey("max.users")
                .value("100")
                .updatedAt(Instant.now())
                .build();

        UpdateSystemSettingRequest request = new UpdateSystemSettingRequest("max.users", "200");

        when(systemSettingRepository.findById("max.users")).thenReturn(Optional.of(existing));
        when(userRepository.findById(adminUserId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> adminService.updateSystemSetting(request));
        verify(systemSettingRepository, never()).save(any());
    }

    // --- getAllSettings ---

    @Test
    void getAllSettings_returnsList() {
        SystemSetting setting1 = SystemSetting.builder()
                .settingKey("key1").value("val1").updatedBy(adminUser).updatedAt(Instant.now()).build();
        SystemSetting setting2 = SystemSetting.builder()
                .settingKey("key2").value("val2").updatedBy(null).updatedAt(Instant.now()).build();

        when(systemSettingRepository.findAll()).thenReturn(List.of(setting1, setting2));

        List<SystemSettingResponse> responses = adminService.getAllSettings();

        assertEquals(2, responses.size());
        assertEquals("key1", responses.get(0).key());
        assertEquals("val1", responses.get(0).value());
        assertEquals(adminUserId, responses.get(0).updatedBy());
        assertEquals("key2", responses.get(1).key());
        assertNull(responses.get(1).updatedBy());
    }

    @Test
    void getAllSettings_empty_returnsEmptyList() {
        when(systemSettingRepository.findAll()).thenReturn(List.of());

        List<SystemSettingResponse> responses = adminService.getAllSettings();

        assertTrue(responses.isEmpty());
    }

    // --- getUsageStats ---

    @Test
    void getUsageStats_admin_success() {
        setSecurityContextWithRole(adminUserId, "ADMIN");
        when(userRepository.count()).thenReturn(10L);
        when(userRepository.countByIsActiveTrue()).thenReturn(8L);
        when(teamRepository.count()).thenReturn(3L);
        when(projectRepository.count()).thenReturn(12L);
        when(qaJobRepository.count()).thenReturn(50L);

        Map<String, Object> stats = adminService.getUsageStats();

        assertEquals(10L, stats.get("totalUsers"));
        assertEquals(8L, stats.get("activeUsers"));
        assertEquals(3L, stats.get("totalTeams"));
        assertEquals(12L, stats.get("totalProjects"));
        assertEquals(50L, stats.get("totalJobs"));
    }

    @Test
    void getUsageStats_ownerRole_success() {
        setSecurityContextWithRole(adminUserId, "OWNER");
        when(userRepository.count()).thenReturn(5L);
        when(userRepository.countByIsActiveTrue()).thenReturn(4L);
        when(teamRepository.count()).thenReturn(2L);
        when(projectRepository.count()).thenReturn(7L);
        when(qaJobRepository.count()).thenReturn(20L);

        Map<String, Object> stats = adminService.getUsageStats();

        assertEquals(5L, stats.get("totalUsers"));
        assertEquals(4L, stats.get("activeUsers"));
    }

    @Test
    void getUsageStats_nonAdmin_throws() {
        setSecurityContext(regularUserId);

        assertThrows(AccessDeniedException.class,
                () -> adminService.getUsageStats());
        verify(userRepository, never()).count();
        verify(teamRepository, never()).count();
        verify(projectRepository, never()).count();
        verify(qaJobRepository, never()).count();
    }

    @Test
    void getUsageStats_noAuthContext_throws() {
        // No security context set at all
        assertThrows(AccessDeniedException.class,
                () -> adminService.getUsageStats());
    }

    // --- helper methods ---

    private void setSecurityContext(UUID userId) {
        var auth = new UsernamePasswordAuthenticationToken(userId, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private void setSecurityContextWithRole(UUID userId, String role) {
        var auth = new UsernamePasswordAuthenticationToken(userId, null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role)));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
