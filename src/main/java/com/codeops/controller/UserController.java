package com.codeops.controller;

import com.codeops.dto.request.UpdateUserRequest;
import com.codeops.dto.response.UserResponse;
import com.codeops.security.SecurityUtils;
import com.codeops.service.AuditLogService;
import com.codeops.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for user account management operations.
 *
 * <p>Provides endpoints for viewing and updating user profiles, searching users,
 * and managing user activation status. Most endpoints require basic authentication,
 * while activation/deactivation endpoints require ADMIN or OWNER role.</p>
 *
 * <p>User activation and deactivation operations record an audit log entry
 * via {@link AuditLogService}.</p>
 *
 * @see UserService
 * @see AuditLogService
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;
    private final AuditLogService auditLogService;

    /**
     * Retrieves the profile of the currently authenticated user.
     *
     * <p>GET /api/v1/users/me</p>
     *
     * <p>Requires authentication.</p>
     *
     * @return the current user's profile details
     */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserResponse> getCurrentUser() {
        log.debug("getCurrentUser called");
        return ResponseEntity.ok(userService.getCurrentUser());
    }

    /**
     * Retrieves a user by their identifier.
     *
     * <p>GET /api/v1/users/{id}</p>
     *
     * <p>Requires authentication.</p>
     *
     * @param id the UUID of the user to retrieve
     * @return the user's profile details
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserResponse> getUserById(@PathVariable UUID id) {
        log.debug("getUserById called with id={}", id);
        return ResponseEntity.ok(userService.getUserById(id));
    }

    /**
     * Updates a user's profile information.
     *
     * <p>PUT /api/v1/users/{id}</p>
     *
     * <p>Requires authentication.</p>
     *
     * @param id      the UUID of the user to update
     * @param request the update payload with fields to modify (e.g., display name, avatar URL)
     * @return the updated user profile
     */
    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserResponse> updateUser(@PathVariable UUID id,
                                                   @Valid @RequestBody UpdateUserRequest request) {
        log.debug("updateUser called with id={}", id);
        return ResponseEntity.ok(userService.updateUser(id, request));
    }

    /**
     * Searches for users matching a query string.
     *
     * <p>GET /api/v1/users/search?q={query}</p>
     *
     * <p>Requires authentication. The query must be between 2 and 100 characters
     * and is matched against user names and email addresses.</p>
     *
     * @param q the search query (2-100 characters, must not be blank)
     * @return a list of users matching the search criteria
     */
    @GetMapping("/search")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<UserResponse>> searchUsers(@RequestParam @NotBlank @Size(min = 2, max = 100) String q) {
        log.debug("searchUsers called with q={}", q);
        return ResponseEntity.ok(userService.searchUsers(q));
    }

    /**
     * Deactivates a user account, preventing them from logging in.
     *
     * <p>PUT /api/v1/users/{id}/deactivate</p>
     *
     * <p>Requires ADMIN or OWNER role. Logs a USER_DEACTIVATED audit event.
     * Returns HTTP 204 No Content on success.</p>
     *
     * @param id the UUID of the user to deactivate
     * @return empty response with HTTP 204 status
     */
    @PutMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OWNER')")
    public ResponseEntity<Void> deactivateUser(@PathVariable UUID id) {
        log.debug("deactivateUser called with id={}", id);
        userService.deactivateUser(id);
        auditLogService.log(SecurityUtils.getCurrentUserId(), null, "USER_DEACTIVATED", "USER", id, null);
        return ResponseEntity.noContent().build();
    }

    /**
     * Reactivates a previously deactivated user account.
     *
     * <p>PUT /api/v1/users/{id}/activate</p>
     *
     * <p>Requires ADMIN or OWNER role. Logs a USER_ACTIVATED audit event.
     * Returns HTTP 204 No Content on success.</p>
     *
     * @param id the UUID of the user to activate
     * @return empty response with HTTP 204 status
     */
    @PutMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OWNER')")
    public ResponseEntity<Void> activateUser(@PathVariable UUID id) {
        log.debug("activateUser called with id={}", id);
        userService.activateUser(id);
        auditLogService.log(SecurityUtils.getCurrentUserId(), null, "USER_ACTIVATED", "USER", id, null);
        return ResponseEntity.noContent().build();
    }
}
