package com.codeops.controller;

import com.codeops.dto.request.ChangePasswordRequest;
import com.codeops.dto.request.LoginRequest;
import com.codeops.dto.request.RefreshTokenRequest;
import com.codeops.dto.request.RegisterRequest;
import com.codeops.dto.response.AuthResponse;
import com.codeops.security.JwtTokenProvider;
import com.codeops.security.SecurityUtils;
import com.codeops.service.AuditLogService;
import com.codeops.service.AuthService;
import com.codeops.service.TokenBlacklistService;
import io.jsonwebtoken.Claims;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for authentication operations including user registration,
 * login, token refresh, logout, and password management.
 *
 * <p>The {@code /register}, {@code /login}, and {@code /refresh} endpoints are
 * publicly accessible. The {@code /logout} and {@code /change-password} endpoints
 * require authentication.</p>
 *
 * @see AuthService
 * @see JwtTokenProvider
 * @see TokenBlacklistService
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;
    private final AuditLogService auditLogService;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenBlacklistService tokenBlacklistService;

    /**
     * Registers a new user account and returns authentication tokens.
     *
     * <p>POST {@code /api/v1/auth/register}</p>
     *
     * <p>Side effect: logs a {@code USER_REGISTERED} audit entry for the newly created user.</p>
     *
     * @param request the registration payload containing user details (email, password, name)
     * @return the authentication response with access and refresh tokens (HTTP 201)
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.debug("register called");
        AuthResponse response = authService.register(request);
        auditLogService.log(response.user().id(), null, "USER_REGISTERED", "USER", response.user().id(), null);
        return ResponseEntity.status(201).body(response);
    }

    /**
     * Authenticates a user with email and password and returns JWT tokens.
     *
     * <p>POST {@code /api/v1/auth/login}</p>
     *
     * <p>Side effect: logs a {@code USER_LOGIN} audit entry for the authenticated user.</p>
     *
     * @param request the login payload containing email and password
     * @return the authentication response with access and refresh tokens
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        log.debug("login called with email={}", request.email());
        AuthResponse response = authService.login(request);
        auditLogService.log(response.user().id(), null, "USER_LOGIN", "USER", response.user().id(), null);
        return ResponseEntity.ok(response);
    }

    /**
     * Refreshes an expired access token using a valid refresh token.
     *
     * <p>POST {@code /api/v1/auth/refresh}</p>
     *
     * @param request the refresh payload containing the refresh token
     * @return the authentication response with new access and refresh tokens
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        log.debug("refresh called");
        AuthResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Logs out the current user by blacklisting their JWT access token.
     *
     * <p>POST {@code /api/v1/auth/logout}</p>
     *
     * <p>Requires authentication. The token's JTI (JWT ID) is added to the blacklist
     * until the token's natural expiration time.</p>
     *
     * @param authHeader the Authorization header containing the Bearer token
     * @return HTTP 204 No Content on successful logout
     */
    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> logout(@RequestHeader("Authorization") String authHeader) {
        log.debug("logout called");
        String token = authHeader.replace("Bearer ", "");
        Claims claims = jwtTokenProvider.parseClaims(token);
        tokenBlacklistService.blacklist(
                claims.get("jti", String.class),
                claims.getExpiration().toInstant()
        );
        return ResponseEntity.noContent().build();
    }

    /**
     * Changes the password for the currently authenticated user.
     *
     * <p>POST {@code /api/v1/auth/change-password}</p>
     *
     * <p>Requires authentication. The caller must provide their current password
     * along with the new password.</p>
     *
     * @param request the change-password payload containing the current and new passwords
     * @return HTTP 200 OK on successful password change
     */
    @PostMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        log.debug("changePassword called");
        authService.changePassword(request);
        return ResponseEntity.ok().build();
    }
}
