package com.codeops.service;

import com.codeops.dto.request.AdminUpdateUserRequest;
import com.codeops.dto.request.UpdateSystemSettingRequest;
import com.codeops.dto.response.SystemSettingResponse;
import com.codeops.dto.response.UserResponse;
import com.codeops.entity.SystemSetting;
import com.codeops.entity.User;
import com.codeops.repository.*;
import com.codeops.security.SecurityUtils;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Provides administrative operations for managing users, system settings, and platform usage statistics.
 *
 * <p>Most operations in this service require the calling user to have admin privileges,
 * enforced via {@link SecurityUtils#isAdmin()}. System settings are stored as key-value
 * pairs and support both creation and update semantics.</p>
 *
 * @see AdminController
 * @see UserRepository
 * @see SystemSettingRepository
 */
@Service
@RequiredArgsConstructor
@Transactional
public class AdminService {

    private static final Logger log = LoggerFactory.getLogger(AdminService.class);

    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final ProjectRepository projectRepository;
    private final QaJobRepository qaJobRepository;
    private final SystemSettingRepository systemSettingRepository;

    private void verifyCurrentUserIsAdmin() {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        if (!SecurityUtils.isAdmin()) {
            throw new org.springframework.security.access.AccessDeniedException("Admin access required");
        }
    }

    /**
     * Retrieves a paginated list of all users in the system.
     *
     * @param pageable the pagination and sorting parameters
     * @return a page of user response DTOs
     * @throws AccessDeniedException if the current user is not an admin
     */
    @Transactional(readOnly = true)
    public Page<UserResponse> getAllUsers(Pageable pageable) {
        log.debug("getAllUsers called with pageable={}", pageable);
        verifyCurrentUserIsAdmin();
        return userRepository.findAll(pageable)
                .map(this::mapToUserResponse);
    }

    /**
     * Retrieves a single user by their unique identifier.
     *
     * @param userId the UUID of the user to retrieve
     * @return the user as a response DTO
     * @throws AccessDeniedException if the current user is not an admin
     * @throws EntityNotFoundException if no user exists with the given ID
     */
    @Transactional(readOnly = true)
    public UserResponse getUserById(UUID userId) {
        log.debug("getUserById called with userId={}", userId);
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        if (!SecurityUtils.isAdmin()) {
            throw new AccessDeniedException("Admin access required");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        return mapToUserResponse(user);
    }

    /**
     * Updates a user's active status based on the provided admin request.
     *
     * <p>Currently supports toggling the {@code isActive} flag on a user account.
     * A deactivated user will be unable to log in.</p>
     *
     * @param userId  the UUID of the user to update
     * @param request the update request containing the new active status
     * @return the updated user as a response DTO
     * @throws EntityNotFoundException if no user exists with the given ID
     */
    public UserResponse updateUserStatus(UUID userId, AdminUpdateUserRequest request) {
        log.debug("updateUserStatus called with userId={}, isActive={}", userId, request.isActive());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        if (request.isActive() != null) {
            boolean previousState = Boolean.TRUE.equals(user.getIsActive());
            user.setIsActive(request.isActive());
            if (previousState && !request.isActive()) {
                log.info("User deactivated: userId={}", userId);
            } else if (!previousState && request.isActive()) {
                log.info("User activated: userId={}", userId);
            }
        }
        user = userRepository.save(user);
        return mapToUserResponse(user);
    }

    /**
     * Retrieves a single system setting by its key.
     *
     * @param key the unique setting key to look up
     * @return the system setting as a response DTO
     * @throws EntityNotFoundException if no setting exists with the given key
     */
    @Transactional(readOnly = true)
    public SystemSettingResponse getSystemSetting(String key) {
        log.debug("getSystemSetting called with key={}", key);
        SystemSetting setting = systemSettingRepository.findById(key)
                .orElseThrow(() -> new EntityNotFoundException("System setting not found: " + key));
        return mapToSettingResponse(setting);
    }

    /**
     * Creates or updates a system setting identified by the request's key.
     *
     * <p>If a setting with the given key already exists, its value and metadata are updated.
     * Otherwise, a new setting is created. The current user is recorded as the updater.</p>
     *
     * @param request the request containing the setting key and new value
     * @return the created or updated system setting as a response DTO
     * @throws EntityNotFoundException if the current user is not found
     */
    public SystemSettingResponse updateSystemSetting(UpdateSystemSettingRequest request) {
        log.debug("updateSystemSetting called with key={}", request.key());
        var existing = systemSettingRepository.findById(request.key());
        SystemSetting setting;
        if (existing.isPresent()) {
            setting = existing.get();
            setting.setValue(request.value());
            setting.setUpdatedBy(userRepository.findById(SecurityUtils.getCurrentUserId()).orElseThrow(() -> new EntityNotFoundException("User not found")));
            setting.setUpdatedAt(Instant.now());
            log.info("System setting updated: key={}", request.key());
        } else {
            setting = SystemSetting.builder()
                    .settingKey(request.key())
                    .value(request.value())
                    .updatedBy(userRepository.findById(SecurityUtils.getCurrentUserId()).orElseThrow(() -> new EntityNotFoundException("User not found")))
                    .updatedAt(Instant.now())
                    .build();
            log.info("System setting created: key={}", request.key());
        }
        setting = systemSettingRepository.save(setting);
        return mapToSettingResponse(setting);
    }

    /**
     * Retrieves all system settings.
     *
     * @return a list of all system settings as response DTOs
     */
    @Transactional(readOnly = true)
    public List<SystemSettingResponse> getAllSettings() {
        log.debug("getAllSettings called");
        List<SystemSetting> settings = systemSettingRepository.findAll();
        return settings.stream()
                .map(this::mapToSettingResponse)
                .toList();
    }

    /**
     * Retrieves platform-wide usage statistics including counts of users, teams, projects, and QA jobs.
     *
     * <p>Returns a map with keys: {@code totalUsers}, {@code activeUsers}, {@code totalTeams},
     * {@code totalProjects}, and {@code totalJobs}.</p>
     *
     * @return a map of statistic names to their numeric values
     * @throws AccessDeniedException if the current user is not an admin
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getUsageStats() {
        log.debug("getUsageStats called");
        verifyCurrentUserIsAdmin();
        long totalUsers = userRepository.count();
        long activeUsers = userRepository.countByIsActiveTrue();
        long totalTeams = teamRepository.count();
        long totalProjects = projectRepository.count();
        long totalJobs = qaJobRepository.count();
        return Map.of(
                "totalUsers", totalUsers,
                "activeUsers", activeUsers,
                "totalTeams", totalTeams,
                "totalProjects", totalProjects,
                "totalJobs", totalJobs
        );
    }

    private UserResponse mapToUserResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getAvatarUrl(),
                user.getIsActive(),
                user.getLastLoginAt(),
                user.getCreatedAt(),
                user.getMfaEnabled()
        );
    }

    private SystemSettingResponse mapToSettingResponse(SystemSetting setting) {
        return new SystemSettingResponse(
                setting.getSettingKey(),
                setting.getValue(),
                setting.getUpdatedBy() != null ? setting.getUpdatedBy().getId() : null,
                setting.getUpdatedAt()
        );
    }
}
