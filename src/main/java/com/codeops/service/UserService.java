package com.codeops.service;

import com.codeops.dto.request.UpdateUserRequest;
import com.codeops.dto.response.UserResponse;
import com.codeops.entity.User;
import com.codeops.repository.UserRepository;
import com.codeops.security.SecurityUtils;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Provides user account retrieval, profile updates, and activation management.
 *
 * <p>User profiles include display name, avatar URL, and active status. Profile
 * updates are restricted to the user themselves or an administrator. Search
 * functionality allows looking up users by display name with a maximum of 20 results.</p>
 *
 * @see UserController
 * @see User
 * @see UserRepository
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    /**
     * Retrieves a user by their unique identifier.
     *
     * @param id the UUID of the user to retrieve
     * @return the user as a response DTO
     * @throws EntityNotFoundException if no user exists with the given ID
     */
    public UserResponse getUserById(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        return mapToUserResponse(user);
    }

    /**
     * Retrieves a user by their email address.
     *
     * @param email the email address of the user to retrieve
     * @return the user as a response DTO
     * @throws EntityNotFoundException if no user exists with the given email
     */
    public UserResponse getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        return mapToUserResponse(user);
    }

    /**
     * Retrieves the profile of the currently authenticated user.
     *
     * @return the current user as a response DTO
     * @throws EntityNotFoundException if the current user is not found in the database
     */
    public UserResponse getCurrentUser() {
        return getUserById(SecurityUtils.getCurrentUserId());
    }

    /**
     * Updates a user's profile fields (display name and/or avatar URL).
     *
     * <p>Only non-null fields in the request are applied. The calling user must be
     * either the user being updated or an administrator.</p>
     *
     * @param userId the ID of the user to update
     * @param request the update request containing optional display name and avatar URL
     * @return the updated user as a response DTO
     * @throws EntityNotFoundException if the user is not found
     * @throws AccessDeniedException if the current user is not authorized to update this profile
     */
    @Transactional
    public UserResponse updateUser(UUID userId, UpdateUserRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        UUID currentUserId = SecurityUtils.getCurrentUserId();
        if (!currentUserId.equals(userId) && !SecurityUtils.isAdmin()) {
            throw new AccessDeniedException("Not authorized to update this user");
        }

        if (request.displayName() != null) {
            user.setDisplayName(request.displayName());
        }
        if (request.avatarUrl() != null) {
            user.setAvatarUrl(request.avatarUrl());
        }

        user = userRepository.save(user);
        return mapToUserResponse(user);
    }

    /**
     * Searches for users by display name using a case-insensitive partial match.
     *
     * <p>Results are limited to a maximum of 20 users.</p>
     *
     * @param query the search string to match against user display names
     * @return a list of matching users as response DTOs, up to 20 results
     */
    public List<UserResponse> searchUsers(String query) {
        return userRepository.findByDisplayNameContainingIgnoreCase(query).stream()
                .limit(20)
                .map(this::mapToUserResponse)
                .toList();
    }

    /**
     * Deactivates a user account by setting its active flag to {@code false}.
     *
     * <p>Deactivated users retain their data but cannot authenticate.</p>
     *
     * @param userId the ID of the user to deactivate
     * @throws EntityNotFoundException if the user is not found
     */
    @Transactional
    public void deactivateUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        user.setIsActive(false);
        userRepository.save(user);
    }

    /**
     * Reactivates a previously deactivated user account by setting its active flag to {@code true}.
     *
     * @param userId the ID of the user to activate
     * @throws EntityNotFoundException if the user is not found
     */
    @Transactional
    public void activateUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        user.setIsActive(true);
        userRepository.save(user);
    }

    private UserResponse mapToUserResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getAvatarUrl(),
                user.getIsActive(),
                user.getLastLoginAt(),
                user.getCreatedAt()
        );
    }
}
