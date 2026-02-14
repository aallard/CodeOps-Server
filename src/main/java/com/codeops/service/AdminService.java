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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminService {

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

    @Transactional(readOnly = true)
    public Page<UserResponse> getAllUsers(Pageable pageable) {
        verifyCurrentUserIsAdmin();
        return userRepository.findAll(pageable)
                .map(this::mapToUserResponse);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(UUID userId) {
        verifyCurrentUserIsAdmin();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        return mapToUserResponse(user);
    }

    public UserResponse updateUserStatus(UUID userId, AdminUpdateUserRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        if (request.isActive() != null) {
            user.setIsActive(request.isActive());
        }
        user = userRepository.save(user);
        return mapToUserResponse(user);
    }

    @Transactional(readOnly = true)
    public SystemSettingResponse getSystemSetting(String key) {
        SystemSetting setting = systemSettingRepository.findById(key)
                .orElseThrow(() -> new EntityNotFoundException("System setting not found: " + key));
        return mapToSettingResponse(setting);
    }

    public SystemSettingResponse updateSystemSetting(UpdateSystemSettingRequest request) {
        var existing = systemSettingRepository.findById(request.key());
        SystemSetting setting;
        if (existing.isPresent()) {
            setting = existing.get();
            setting.setValue(request.value());
            setting.setUpdatedBy(userRepository.findById(SecurityUtils.getCurrentUserId()).orElseThrow(() -> new EntityNotFoundException("User not found")));
            setting.setUpdatedAt(Instant.now());
        } else {
            setting = SystemSetting.builder()
                    .settingKey(request.key())
                    .value(request.value())
                    .updatedBy(userRepository.findById(SecurityUtils.getCurrentUserId()).orElseThrow(() -> new EntityNotFoundException("User not found")))
                    .updatedAt(Instant.now())
                    .build();
        }
        setting = systemSettingRepository.save(setting);
        return mapToSettingResponse(setting);
    }

    @Transactional(readOnly = true)
    public List<SystemSettingResponse> getAllSettings() {
        return systemSettingRepository.findAll().stream()
                .map(this::mapToSettingResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getUsageStats() {
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
                user.getCreatedAt()
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
