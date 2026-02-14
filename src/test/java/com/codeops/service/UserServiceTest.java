package com.codeops.service;

import com.codeops.dto.request.UpdateUserRequest;
import com.codeops.dto.response.UserResponse;
import com.codeops.entity.User;
import com.codeops.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        testUser = User.builder()
                .email("test@codeops.dev")
                .passwordHash("hash")
                .displayName("Test User")
                .isActive(true)
                .build();
        testUser.setId(userId);
        testUser.setCreatedAt(Instant.now());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getUserById_success() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        UserResponse response = userService.getUserById(userId);
        assertEquals(userId, response.id());
        assertEquals("test@codeops.dev", response.email());
    }

    @Test
    void getUserById_notFound_throws() {
        when(userRepository.findById(any(UUID.class))).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> userService.getUserById(UUID.randomUUID()));
    }

    @Test
    void getUserByEmail_success() {
        when(userRepository.findByEmail("test@codeops.dev")).thenReturn(Optional.of(testUser));
        UserResponse response = userService.getUserByEmail("test@codeops.dev");
        assertEquals("test@codeops.dev", response.email());
    }

    @Test
    void getUserByEmail_notFound_throws() {
        when(userRepository.findByEmail("unknown@test.com")).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> userService.getUserByEmail("unknown@test.com"));
    }

    @Test
    void getCurrentUser_returnsCurrentUser() {
        setSecurityContext(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        UserResponse response = userService.getCurrentUser();
        assertEquals(userId, response.id());
    }

    @Test
    void updateUser_selfUpdate_success() {
        setSecurityContext(userId);
        UpdateUserRequest request = new UpdateUserRequest("New Name", "https://avatar.url");
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        UserResponse response = userService.updateUser(userId, request);
        assertEquals("New Name", testUser.getDisplayName());
        assertEquals("https://avatar.url", testUser.getAvatarUrl());
    }

    @Test
    void updateUser_differentUser_notAdmin_throws() {
        UUID otherUserId = UUID.randomUUID();
        setSecurityContext(otherUserId);
        UpdateUserRequest request = new UpdateUserRequest("Name", null);
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        assertThrows(AccessDeniedException.class, () -> userService.updateUser(userId, request));
    }

    @Test
    void updateUser_adminCanUpdateOthers() {
        UUID adminId = UUID.randomUUID();
        setSecurityContextWithRole(adminId, "ADMIN");
        UpdateUserRequest request = new UpdateUserRequest("Admin Updated", null);
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        userService.updateUser(userId, request);
        assertEquals("Admin Updated", testUser.getDisplayName());
    }

    @Test
    void updateUser_nullFields_notUpdated() {
        setSecurityContext(userId);
        UpdateUserRequest request = new UpdateUserRequest(null, null);
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        userService.updateUser(userId, request);
        assertEquals("Test User", testUser.getDisplayName());
    }

    @Test
    void searchUsers_returnsResults() {
        when(userRepository.findByDisplayNameContainingIgnoreCase("test")).thenReturn(List.of(testUser));
        List<UserResponse> results = userService.searchUsers("test");
        assertEquals(1, results.size());
    }

    @Test
    void deactivateUser_success() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        userService.deactivateUser(userId);
        assertFalse(testUser.getIsActive());
    }

    @Test
    void activateUser_success() {
        testUser.setIsActive(false);
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        userService.activateUser(userId);
        assertTrue(testUser.getIsActive());
    }

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
