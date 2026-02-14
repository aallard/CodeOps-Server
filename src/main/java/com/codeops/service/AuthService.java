package com.codeops.service;

import com.codeops.config.AppConstants;
import com.codeops.dto.request.ChangePasswordRequest;
import com.codeops.dto.request.LoginRequest;
import com.codeops.dto.request.RefreshTokenRequest;
import com.codeops.dto.request.RegisterRequest;
import com.codeops.dto.response.AuthResponse;
import com.codeops.dto.response.UserResponse;
import com.codeops.entity.User;
import com.codeops.repository.TeamMemberRepository;
import com.codeops.repository.UserRepository;
import com.codeops.security.JwtTokenProvider;
import com.codeops.security.SecurityUtils;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final TeamMemberRepository teamMemberRepository;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email already registered");
        }

        User user = User.builder()
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .displayName(request.displayName())
                .isActive(true)
                .build();
        user = userRepository.save(user);

        String token = jwtTokenProvider.generateToken(user, List.of());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user);

        return new AuthResponse(token, refreshToken, mapToUserResponse(user));
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

        if (!user.getIsActive()) {
            throw new IllegalArgumentException("Account is deactivated");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid credentials");
        }

        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        List<String> roles = getUserRoles(user.getId());
        String token = jwtTokenProvider.generateToken(user, roles);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user);

        return new AuthResponse(token, refreshToken, mapToUserResponse(user));
    }

    public AuthResponse refreshToken(RefreshTokenRequest request) {
        if (!jwtTokenProvider.validateToken(request.refreshToken())) {
            throw new IllegalArgumentException("Invalid refresh token");
        }

        if (!jwtTokenProvider.isRefreshToken(request.refreshToken())) {
            throw new IllegalArgumentException("Invalid refresh token");
        }

        UUID userId = jwtTokenProvider.getUserIdFromToken(request.refreshToken());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (!user.getIsActive()) {
            throw new IllegalArgumentException("Account is deactivated");
        }

        List<String> roles = getUserRoles(user.getId());
        String token = jwtTokenProvider.generateToken(user, roles);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user);

        return new AuthResponse(token, refreshToken, mapToUserResponse(user));
    }

    public void changePassword(ChangePasswordRequest request) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }

        if (request.newPassword().length() < AppConstants.MIN_PASSWORD_LENGTH) {
            throw new IllegalArgumentException("Password must be at least " + AppConstants.MIN_PASSWORD_LENGTH + " characters");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }

    private List<String> getUserRoles(UUID userId) {
        return teamMemberRepository.findByUserId(userId).stream()
                .map(member -> member.getRole().name())
                .distinct()
                .toList();
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
