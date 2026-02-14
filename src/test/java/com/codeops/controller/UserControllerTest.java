package com.codeops.controller;

import com.codeops.dto.request.UpdateUserRequest;
import com.codeops.dto.response.UserResponse;
import com.codeops.service.AuditLogService;
import com.codeops.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private AuditLogService auditLogService;

    private UserController controller;

    private final UUID currentUserId = UUID.randomUUID();
    private final UUID targetUserId = UUID.randomUUID();
    private final Instant now = Instant.now();

    @BeforeEach
    void setUp() {
        controller = new UserController(userService, auditLogService);
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
        return new UserResponse(id, "user@example.com", "Test User", null, true, now, now);
    }

    @Test
    void getCurrentUser_returns200WithUser() {
        UserResponse expected = userResponse(currentUserId);
        when(userService.getCurrentUser()).thenReturn(expected);

        ResponseEntity<UserResponse> response = controller.getCurrentUser();

        assertEquals(200, response.getStatusCode().value());
        assertEquals(expected, response.getBody());
        verify(userService).getCurrentUser();
    }

    @Test
    void getUserById_returns200WithUser() {
        UserResponse expected = userResponse(targetUserId);
        when(userService.getUserById(targetUserId)).thenReturn(expected);

        ResponseEntity<UserResponse> response = controller.getUserById(targetUserId);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(expected, response.getBody());
        verify(userService).getUserById(targetUserId);
    }

    @Test
    void updateUser_returns200WithUpdatedUser() {
        UpdateUserRequest request = new UpdateUserRequest("New Name", "https://avatar.url");
        UserResponse expected = new UserResponse(targetUserId, "user@example.com", "New Name", "https://avatar.url", true, now, now);
        when(userService.updateUser(targetUserId, request)).thenReturn(expected);

        ResponseEntity<UserResponse> response = controller.updateUser(targetUserId, request);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(expected, response.getBody());
        verify(userService).updateUser(targetUserId, request);
    }

    @Test
    void searchUsers_returns200WithList() {
        String query = "test";
        List<UserResponse> expected = List.of(userResponse(currentUserId), userResponse(targetUserId));
        when(userService.searchUsers(query)).thenReturn(expected);

        ResponseEntity<List<UserResponse>> response = controller.searchUsers(query);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(expected, response.getBody());
        assertEquals(2, response.getBody().size());
        verify(userService).searchUsers(query);
    }

    @Test
    void deactivateUser_returns204AndLogsAudit() {
        setSecurityContext(currentUserId);

        ResponseEntity<Void> response = controller.deactivateUser(targetUserId);

        assertEquals(204, response.getStatusCode().value());
        assertNull(response.getBody());
        verify(userService).deactivateUser(targetUserId);
        verify(auditLogService).log(currentUserId, null, "USER_DEACTIVATED", "USER", targetUserId, null);
    }

    @Test
    void activateUser_returns204AndLogsAudit() {
        setSecurityContext(currentUserId);

        ResponseEntity<Void> response = controller.activateUser(targetUserId);

        assertEquals(204, response.getStatusCode().value());
        assertNull(response.getBody());
        verify(userService).activateUser(targetUserId);
        verify(auditLogService).log(currentUserId, null, "USER_ACTIVATED", "USER", targetUserId, null);
    }
}
