package com.codeops.controller;

import com.codeops.dto.request.*;
import com.codeops.dto.response.*;
import com.codeops.security.JwtTokenProvider;
import com.codeops.security.SecurityUtils;
import com.codeops.service.AuditLogService;
import com.codeops.service.AuthService;
import com.codeops.service.MfaService;
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
    private final MfaService mfaService;
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
        if (response.user() != null) {
            auditLogService.log(response.user().id(), null, "USER_LOGIN", "USER", response.user().id(), null);
        }
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

    // ──────────────────────────────────────────────
    // MFA Endpoints
    // ──────────────────────────────────────────────

    /**
     * Initiates MFA setup by generating a TOTP secret and recovery codes.
     *
     * <p>POST {@code /api/v1/auth/mfa/setup}</p>
     *
     * <p>Requires authentication. The user must provide their current password for re-verification.
     * Returns the TOTP secret, a QR code data URI, and a set of one-time recovery codes.</p>
     *
     * @param request the MFA setup payload containing the user's current password
     * @return the MFA setup response with secret, QR URI, and recovery codes
     */
    @PostMapping("/mfa/setup")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MfaSetupResponse> setupMfa(@Valid @RequestBody MfaSetupRequest request) {
        log.debug("setupMfa called");
        MfaSetupResponse response = mfaService.setupMfa(request);
        auditLogService.log(SecurityUtils.getCurrentUserId(), null, "MFA_SETUP_INITIATED", "USER",
                SecurityUtils.getCurrentUserId(), null);
        return ResponseEntity.ok(response);
    }

    /**
     * Verifies a TOTP code and enables MFA on the account.
     *
     * <p>POST {@code /api/v1/auth/mfa/verify}</p>
     *
     * <p>Requires authentication. This is the second step of MFA setup — after receiving the secret
     * from {@code /mfa/setup}, the user submits a TOTP code from their authenticator app.</p>
     *
     * @param request the verify payload containing the TOTP code
     * @return the MFA status response confirming MFA is enabled
     */
    @PostMapping("/mfa/verify")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MfaStatusResponse> verifyMfa(@Valid @RequestBody MfaVerifyRequest request) {
        log.debug("verifyMfa called");
        MfaStatusResponse response = mfaService.verifyAndEnableMfa(request);
        auditLogService.log(SecurityUtils.getCurrentUserId(), null, "MFA_ENABLED", "USER",
                SecurityUtils.getCurrentUserId(), null);
        return ResponseEntity.ok(response);
    }

    /**
     * Completes an MFA-challenged login by verifying a TOTP code or recovery code.
     *
     * <p>POST {@code /api/v1/auth/mfa/login}</p>
     *
     * <p>Publicly accessible (no Bearer token required). The client submits the MFA challenge
     * token received from the login endpoint along with a TOTP code or recovery code.</p>
     *
     * @param request the MFA login payload containing the challenge token and code
     * @return the full auth response with access token, refresh token, and user details
     */
    @PostMapping("/mfa/login")
    public ResponseEntity<AuthResponse> mfaLogin(@Valid @RequestBody MfaLoginRequest request) {
        log.debug("mfaLogin called");
        AuthResponse response = mfaService.verifyMfaLogin(request);
        auditLogService.log(response.user().id(), null, "MFA_LOGIN", "USER", response.user().id(), null);
        return ResponseEntity.ok(response);
    }

    /**
     * Disables MFA for the current user, clearing the stored secret and recovery codes.
     *
     * <p>POST {@code /api/v1/auth/mfa/disable}</p>
     *
     * <p>Requires authentication. The user must provide their current password for re-verification.</p>
     *
     * @param request the disable payload containing the user's current password
     * @return the MFA status response confirming MFA is disabled
     */
    @PostMapping("/mfa/disable")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MfaStatusResponse> disableMfa(@Valid @RequestBody MfaSetupRequest request) {
        log.debug("disableMfa called");
        MfaStatusResponse response = mfaService.disableMfa(request);
        auditLogService.log(SecurityUtils.getCurrentUserId(), null, "MFA_DISABLED", "USER",
                SecurityUtils.getCurrentUserId(), null);
        return ResponseEntity.ok(response);
    }

    /**
     * Regenerates recovery codes for the current user.
     *
     * <p>POST {@code /api/v1/auth/mfa/recovery-codes}</p>
     *
     * <p>Requires authentication. MFA must be enabled. The user must provide their current password.</p>
     *
     * @param request the request payload containing the user's current password
     * @return the recovery response containing the new set of recovery codes
     */
    @PostMapping("/mfa/recovery-codes")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MfaRecoveryResponse> regenerateRecoveryCodes(@Valid @RequestBody MfaSetupRequest request) {
        log.debug("regenerateRecoveryCodes called");
        MfaRecoveryResponse response = mfaService.regenerateRecoveryCodes(request);
        auditLogService.log(SecurityUtils.getCurrentUserId(), null, "MFA_RECOVERY_CODES_REGENERATED", "USER",
                SecurityUtils.getCurrentUserId(), null);
        return ResponseEntity.ok(response);
    }

    /**
     * Returns the MFA status for the current user.
     *
     * <p>GET {@code /api/v1/auth/mfa/status}</p>
     *
     * <p>Requires authentication.</p>
     *
     * @return the MFA status response indicating whether MFA is enabled
     */
    @GetMapping("/mfa/status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MfaStatusResponse> getMfaStatus() {
        log.debug("getMfaStatus called");
        MfaStatusResponse response = mfaService.getMfaStatus();
        return ResponseEntity.ok(response);
    }

    // ──────────────────────────────────────────────
    // Email MFA Endpoints
    // ──────────────────────────────────────────────

    /**
     * Initiates email MFA setup by generating recovery codes and sending a verification
     * code to the user's registered email.
     *
     * <p>POST {@code /api/v1/auth/mfa/setup/email}</p>
     *
     * <p>Requires authentication. The user must provide their current password for re-verification.</p>
     *
     * @param request the email MFA setup payload containing the user's current password
     * @return the recovery response containing recovery codes
     */
    @PostMapping("/mfa/setup/email")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MfaRecoveryResponse> setupEmailMfa(@Valid @RequestBody MfaEmailSetupRequest request) {
        log.debug("setupEmailMfa called");
        MfaRecoveryResponse response = mfaService.setupEmailMfa(request);
        auditLogService.log(SecurityUtils.getCurrentUserId(), null, "EMAIL_MFA_SETUP_INITIATED", "USER",
                SecurityUtils.getCurrentUserId(), null);
        return ResponseEntity.ok(response);
    }

    /**
     * Verifies the email code sent during email MFA setup and enables email MFA.
     *
     * <p>POST {@code /api/v1/auth/mfa/verify-setup/email}</p>
     *
     * <p>Requires authentication.</p>
     *
     * @param request the verify payload containing the email code
     * @return the MFA status response confirming email MFA is enabled
     */
    @PostMapping("/mfa/verify-setup/email")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MfaStatusResponse> verifyEmailSetup(@Valid @RequestBody MfaVerifyRequest request) {
        log.debug("verifyEmailSetup called");
        MfaStatusResponse response = mfaService.verifyEmailSetupAndEnable(request);
        auditLogService.log(SecurityUtils.getCurrentUserId(), null, "EMAIL_MFA_ENABLED", "USER",
                SecurityUtils.getCurrentUserId(), null);
        return ResponseEntity.ok(response);
    }

    /**
     * Resends an MFA login code to the user's email during the email MFA login flow.
     *
     * <p>POST {@code /api/v1/auth/mfa/resend}</p>
     *
     * <p>Publicly accessible (no Bearer token required) since the user hasn't completed
     * MFA authentication yet. Requires a valid MFA challenge token.</p>
     *
     * @param request the resend payload containing the challenge token
     * @return HTTP 200 OK on successful code resend
     */
    @PostMapping("/mfa/resend")
    public ResponseEntity<Void> resendMfaCode(@Valid @RequestBody MfaResendRequest request) {
        log.debug("resendMfaCode called");
        mfaService.sendLoginMfaCode(request);
        return ResponseEntity.ok().build();
    }
}
