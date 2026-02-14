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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Handles user authentication operations including registration, login, token refresh, and password changes.
 *
 * <p>Uses stateless JWT authentication with HS256. Login returns a 24-hour access token and a
 * 30-day refresh token. Passwords are hashed with BCrypt and validated against configurable
 * strength requirements (length, uppercase, lowercase, digit, special character).</p>
 *
 * @see AuthController
 * @see JwtTokenProvider
 * @see UserRepository
 */
@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final TeamMemberRepository teamMemberRepository;

    /**
     * Registers a new user account, hashes the password with BCrypt, and issues JWT tokens.
     *
     * <p>Validates that the email is not already registered and that the password meets
     * strength requirements. The new user is created with {@code isActive=true}. Side effect:
     * logs the registration event at INFO level.</p>
     *
     * @param request the registration request containing email, password, and display name
     * @return an auth response containing access token, refresh token, and user details
     * @throws IllegalArgumentException if the email is already registered or the password is too weak
     */
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            log.warn("Registration attempt with existing email: {}", request.email());
            throw new IllegalArgumentException("Email already registered");
        }

        validatePasswordStrength(request.password());

        User user = User.builder()
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .displayName(request.displayName())
                .isActive(true)
                .build();
        user = userRepository.save(user);
        log.info("User registered: userId={}, email={}", user.getId(), user.getEmail());

        String token = jwtTokenProvider.generateToken(user, List.of());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user);

        return new AuthResponse(token, refreshToken, mapToUserResponse(user));
    }

    /**
     * Authenticates a user with email and password, updates last login timestamp, and issues JWT tokens.
     *
     * <p>Verifies the account exists, is active, and the password matches the stored hash.
     * The generated access token includes the user's team membership roles. Side effect:
     * updates {@code lastLoginAt} on the user entity and logs success/failure at INFO/WARN level.</p>
     *
     * @param request the login request containing email and password
     * @return an auth response containing access token, refresh token, and user details
     * @throws IllegalArgumentException if credentials are invalid or the account is deactivated
     */
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> {
                    log.warn("Login failed: unknown email {}", request.email());
                    return new IllegalArgumentException("Invalid credentials");
                });

        if (!user.getIsActive()) {
            log.warn("Login failed: deactivated account userId={}", user.getId());
            throw new IllegalArgumentException("Account is deactivated");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            log.warn("Login failed: wrong password for userId={}", user.getId());
            throw new IllegalArgumentException("Invalid credentials");
        }

        user.setLastLoginAt(Instant.now());
        userRepository.save(user);
        log.info("Login success: userId={}, email={}", user.getId(), user.getEmail());

        List<String> roles = getUserRoles(user.getId());
        String token = jwtTokenProvider.generateToken(user, roles);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user);

        return new AuthResponse(token, refreshToken, mapToUserResponse(user));
    }

    /**
     * Exchanges a valid refresh token for a new access token and refresh token pair.
     *
     * <p>Validates that the provided token is a legitimate, non-expired refresh token
     * and that the associated user account is still active. Issues a fresh token pair
     * with updated roles.</p>
     *
     * @param request the refresh token request containing the current refresh token
     * @return an auth response containing a new access token, refresh token, and user details
     * @throws IllegalArgumentException if the refresh token is invalid, expired, or the account is deactivated
     */
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

    /**
     * Changes the password for the currently authenticated user.
     *
     * <p>Verifies the current password matches before applying the new one. The new password
     * must meet strength requirements (minimum length, uppercase, lowercase, digit, special character).
     * Side effect: logs the password change at INFO level.</p>
     *
     * @param request the change password request containing the current and new passwords
     * @throws EntityNotFoundException if the current user is not found
     * @throws IllegalArgumentException if the current password is incorrect or the new password is too weak
     */
    public void changePassword(ChangePasswordRequest request) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            log.warn("Password change failed: wrong current password for userId={}", currentUserId);
            throw new IllegalArgumentException("Current password is incorrect");
        }

        validatePasswordStrength(request.newPassword());

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        log.info("Password changed: userId={}", currentUserId);
    }

    private void validatePasswordStrength(String password) {
        if (password.length() < AppConstants.MIN_PASSWORD_LENGTH) {
            throw new IllegalArgumentException("Password must be at least " + AppConstants.MIN_PASSWORD_LENGTH + " characters");
        }
        if (!password.matches(".*[A-Z].*")) {
            throw new IllegalArgumentException("Password must contain at least one uppercase letter");
        }
        if (!password.matches(".*[a-z].*")) {
            throw new IllegalArgumentException("Password must contain at least one lowercase letter");
        }
        if (!password.matches(".*[0-9].*")) {
            throw new IllegalArgumentException("Password must contain at least one digit");
        }
        if (!password.matches(".*[^A-Za-z0-9].*")) {
            throw new IllegalArgumentException("Password must contain at least one special character");
        }
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
