package com.codeops.service;

import com.codeops.dto.request.MfaEmailSetupRequest;
import com.codeops.dto.request.MfaLoginRequest;
import com.codeops.dto.request.MfaResendRequest;
import com.codeops.dto.request.MfaSetupRequest;
import com.codeops.dto.request.MfaVerifyRequest;
import com.codeops.dto.response.AuthResponse;
import com.codeops.dto.response.MfaRecoveryResponse;
import com.codeops.dto.response.MfaSetupResponse;
import com.codeops.dto.response.MfaStatusResponse;
import com.codeops.dto.response.UserResponse;
import com.codeops.entity.MfaEmailCode;
import com.codeops.entity.User;
import com.codeops.entity.enums.MfaMethod;
import com.codeops.notification.EmailService;
import com.codeops.repository.MfaEmailCodeRepository;
import com.codeops.repository.TeamMemberRepository;
import com.codeops.repository.UserRepository;
import com.codeops.security.JwtTokenProvider;
import com.codeops.security.SecurityUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.samstevens.totp.code.*;
import dev.samstevens.totp.exceptions.QrGenerationException;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.ZxingPngQrGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Manages multi-factor authentication (MFA) for user accounts, supporting both
 * TOTP (authenticator app) and email-based verification codes.
 *
 * <p>Provides setup, verification, login challenge resolution, recovery code management,
 * and MFA disablement for both methods. TOTP secrets and recovery codes are encrypted at rest
 * via {@link EncryptionService} using AES-256-GCM. Email MFA codes are BCrypt-hashed and
 * stored in the {@code mfa_email_codes} table with a 10-minute TTL.</p>
 *
 * <p>The MFA login flow is two-phase:
 * <ol>
 *   <li>User submits email + password → receives an MFA challenge token (5-minute JWT)</li>
 *   <li>For TOTP: user submits challenge token + TOTP code → receives full tokens</li>
 *   <li>For Email: system sends code to user's email; user submits challenge token + email code → receives full tokens</li>
 * </ol>
 * </p>
 *
 * @see JwtTokenProvider#generateMfaChallengeToken(User)
 * @see EncryptionService
 * @see EmailService
 */
@Service
@RequiredArgsConstructor
@Transactional
public class MfaService {

    private static final Logger log = LoggerFactory.getLogger(MfaService.class);
    private static final String ISSUER = "CodeOps";
    private static final int RECOVERY_CODE_COUNT = 8;
    private static final int EMAIL_CODE_LENGTH = 6;
    private static final int EMAIL_CODE_TTL_MINUTES = 10;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final EncryptionService encryptionService;
    private final TeamMemberRepository teamMemberRepository;
    private final ObjectMapper objectMapper;
    private final MfaEmailCodeRepository mfaEmailCodeRepository;
    private final EmailService emailService;

    // ──────────────────────────────────────────────
    // TOTP Setup & Verify
    // ──────────────────────────────────────────────

    /**
     * Initiates TOTP MFA setup for the current user by generating a TOTP secret and recovery codes.
     *
     * <p>Requires the user's current password for re-authentication. The generated secret is
     * encrypted and stored on the user entity, but MFA is NOT yet enabled — the user must
     * call {@link #verifyAndEnableMfa(MfaVerifyRequest)} with a valid TOTP code to activate it.</p>
     *
     * @param request the setup request containing the user's current password
     * @return the setup response containing the TOTP secret, QR code URI, and recovery codes
     * @throws IllegalArgumentException if MFA is already enabled or the password is incorrect
     * @throws EntityNotFoundException if the current user is not found
     */
    public MfaSetupResponse setupMfa(MfaSetupRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        log.debug("setupMfa called for userId={}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        if (Boolean.TRUE.equals(user.getMfaEnabled())) {
            throw new IllegalArgumentException("MFA is already enabled");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            log.warn("MFA setup failed: wrong password for userId={}", userId);
            throw new IllegalArgumentException("Invalid password");
        }

        SecretGenerator secretGenerator = new DefaultSecretGenerator();
        String secret = secretGenerator.generate();

        List<String> recoveryCodes = generateRecoveryCodes();

        user.setMfaSecret(encryptionService.encrypt(secret));
        user.setMfaRecoveryCodes(encryptRecoveryCodes(recoveryCodes));
        userRepository.save(user);

        String qrCodeUri = generateQrCodeUri(secret, user.getEmail());

        log.info("MFA setup initiated for userId={}", userId);
        return new MfaSetupResponse(secret, qrCodeUri, recoveryCodes);
    }

    /**
     * Verifies a TOTP code against the stored (but not yet active) secret and enables TOTP MFA.
     *
     * <p>This is the second step of TOTP MFA setup. After calling {@link #setupMfa(MfaSetupRequest)},
     * the user must submit a valid TOTP code generated by their authenticator app.</p>
     *
     * @param request the verify request containing the TOTP code
     * @return the MFA status response confirming MFA is now enabled
     * @throws IllegalArgumentException if MFA is already enabled, no secret is configured, or the code is invalid
     * @throws EntityNotFoundException if the current user is not found
     */
    public MfaStatusResponse verifyAndEnableMfa(MfaVerifyRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        log.debug("verifyAndEnableMfa called for userId={}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        if (Boolean.TRUE.equals(user.getMfaEnabled())) {
            throw new IllegalArgumentException("MFA is already enabled");
        }

        if (user.getMfaSecret() == null) {
            throw new IllegalArgumentException("MFA setup has not been initiated");
        }

        String decryptedSecret = encryptionService.decrypt(user.getMfaSecret());

        if (!verifyTotpCode(decryptedSecret, request.code())) {
            log.warn("MFA verification failed: invalid code for userId={}", userId);
            throw new IllegalArgumentException("Invalid TOTP code");
        }

        user.setMfaEnabled(true);
        user.setMfaMethod(MfaMethod.TOTP);
        userRepository.save(user);

        log.info("TOTP MFA enabled for userId={}", userId);
        return new MfaStatusResponse(true, MfaMethod.TOTP.name(), countRecoveryCodes(user));
    }

    // ──────────────────────────────────────────────
    // Email MFA Setup & Verify
    // ──────────────────────────────────────────────

    /**
     * Initiates email MFA setup for the current user by generating recovery codes and
     * sending a verification code to the user's registered email.
     *
     * <p>Requires the user's current password for re-authentication. MFA is NOT yet enabled
     * — the user must call {@link #verifyEmailSetupAndEnable(MfaVerifyRequest)} with the
     * received email code to activate it.</p>
     *
     * @param request the setup request containing the user's current password
     * @return the MFA status response with recovery codes (mfaEnabled will be false until verified)
     * @throws IllegalArgumentException if MFA is already enabled or the password is incorrect
     * @throws EntityNotFoundException if the current user is not found
     */
    public MfaRecoveryResponse setupEmailMfa(MfaEmailSetupRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        log.debug("setupEmailMfa called for userId={}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        if (Boolean.TRUE.equals(user.getMfaEnabled())) {
            throw new IllegalArgumentException("MFA is already enabled");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            log.warn("Email MFA setup failed: wrong password for userId={}", userId);
            throw new IllegalArgumentException("Invalid password");
        }

        List<String> recoveryCodes = generateRecoveryCodes();
        user.setMfaRecoveryCodes(encryptRecoveryCodes(recoveryCodes));
        // Mark method as EMAIL but don't enable yet — awaiting verification
        user.setMfaMethod(MfaMethod.EMAIL);
        userRepository.save(user);

        // Send verification code
        String code = generateEmailCode();
        saveEmailCode(userId, code);
        emailService.sendMfaCode(user.getEmail(), code);

        log.info("Email MFA setup initiated for userId={}", userId);
        return new MfaRecoveryResponse(recoveryCodes);
    }

    /**
     * Verifies the email code sent during email MFA setup and enables email MFA.
     *
     * @param request the verify request containing the email code
     * @return the MFA status response confirming email MFA is now enabled
     * @throws IllegalArgumentException if MFA is already enabled, method is not EMAIL, or the code is invalid
     * @throws EntityNotFoundException if the current user is not found
     */
    public MfaStatusResponse verifyEmailSetupAndEnable(MfaVerifyRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        log.debug("verifyEmailSetupAndEnable called for userId={}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        if (Boolean.TRUE.equals(user.getMfaEnabled())) {
            throw new IllegalArgumentException("MFA is already enabled");
        }

        if (user.getMfaMethod() != MfaMethod.EMAIL) {
            throw new IllegalArgumentException("Email MFA setup has not been initiated");
        }

        if (!verifyAndConsumeEmailCode(userId, request.code())) {
            log.warn("Email MFA verification failed: invalid code for userId={}", userId);
            throw new IllegalArgumentException("Invalid verification code");
        }

        user.setMfaEnabled(true);
        userRepository.save(user);

        log.info("Email MFA enabled for userId={}", userId);
        return new MfaStatusResponse(true, MfaMethod.EMAIL.name(), countRecoveryCodes(user));
    }

    // ──────────────────────────────────────────────
    // MFA Login (TOTP + Email)
    // ──────────────────────────────────────────────

    /**
     * Completes the MFA login flow by verifying a TOTP code, email code, or recovery code
     * against the challenge token, then issuing full access and refresh tokens.
     *
     * @param request the MFA login request containing the challenge token and code
     * @return the full auth response with access token, refresh token, and user details
     * @throws IllegalArgumentException if the challenge token is invalid/expired, MFA is not enabled,
     *                                  or the code is invalid
     */
    public AuthResponse verifyMfaLogin(MfaLoginRequest request) {
        log.debug("verifyMfaLogin called");

        if (!jwtTokenProvider.validateToken(request.challengeToken())) {
            throw new IllegalArgumentException("Invalid or expired MFA challenge token");
        }

        if (!jwtTokenProvider.isMfaChallengeToken(request.challengeToken())) {
            throw new IllegalArgumentException("Invalid MFA challenge token");
        }

        UUID userId = jwtTokenProvider.getUserIdFromToken(request.challengeToken());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        if (!Boolean.TRUE.equals(user.getMfaEnabled())) {
            throw new IllegalArgumentException("MFA is not enabled for this account");
        }

        String code = request.code();
        boolean verified = false;

        // Try method-specific verification first
        if (user.getMfaMethod() == MfaMethod.TOTP && user.getMfaSecret() != null) {
            String decryptedSecret = encryptionService.decrypt(user.getMfaSecret());
            verified = verifyTotpCode(decryptedSecret, code);
        } else if (user.getMfaMethod() == MfaMethod.EMAIL) {
            verified = verifyAndConsumeEmailCode(userId, code);
        }

        // Fallback to recovery code
        if (!verified) {
            verified = consumeRecoveryCode(user, code);
        }

        if (!verified) {
            log.warn("MFA login failed: invalid code for userId={}", userId);
            throw new IllegalArgumentException("Invalid MFA code");
        }

        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        List<String> roles = getUserRoles(userId);
        String token = jwtTokenProvider.generateToken(user, roles);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user);

        log.info("MFA login success: userId={}", userId);
        return new AuthResponse(token, refreshToken, mapToUserResponse(user));
    }

    /**
     * Sends an MFA login email code to the user identified by the challenge token.
     *
     * <p>This is used for email MFA login flow and for resending expired or missed codes.
     * The endpoint is publicly accessible (no Bearer token required) since the user hasn't
     * completed MFA yet.</p>
     *
     * @param request the resend request containing the challenge token
     * @throws IllegalArgumentException if the challenge token is invalid or MFA is not email-based
     */
    public void sendLoginMfaCode(MfaResendRequest request) {
        log.debug("sendLoginMfaCode called");

        if (!jwtTokenProvider.validateToken(request.challengeToken())) {
            throw new IllegalArgumentException("Invalid or expired MFA challenge token");
        }

        if (!jwtTokenProvider.isMfaChallengeToken(request.challengeToken())) {
            throw new IllegalArgumentException("Invalid MFA challenge token");
        }

        UUID userId = jwtTokenProvider.getUserIdFromToken(request.challengeToken());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        if (user.getMfaMethod() != MfaMethod.EMAIL || !Boolean.TRUE.equals(user.getMfaEnabled())) {
            throw new IllegalArgumentException("Email MFA is not enabled for this account");
        }

        String code = generateEmailCode();
        saveEmailCode(userId, code);
        emailService.sendMfaCode(user.getEmail(), code);

        log.info("MFA login code resent for userId={}", userId);
    }

    // ──────────────────────────────────────────────
    // Disable / Recovery / Status
    // ──────────────────────────────────────────────

    /**
     * Disables MFA for the current user, clearing the stored secret, recovery codes,
     * and any pending email codes.
     *
     * @param request the setup request containing the user's current password (reused DTO)
     * @return the MFA status response confirming MFA is now disabled
     * @throws IllegalArgumentException if MFA is not enabled or the password is incorrect
     * @throws EntityNotFoundException if the current user is not found
     */
    public MfaStatusResponse disableMfa(MfaSetupRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        log.debug("disableMfa called for userId={}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        if (!Boolean.TRUE.equals(user.getMfaEnabled())) {
            throw new IllegalArgumentException("MFA is not enabled");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            log.warn("MFA disable failed: wrong password for userId={}", userId);
            throw new IllegalArgumentException("Invalid password");
        }

        clearMfaFields(user);
        userRepository.save(user);
        mfaEmailCodeRepository.deleteByUserId(userId);

        log.info("MFA disabled for userId={}", userId);
        return new MfaStatusResponse(false, MfaMethod.NONE.name(), null);
    }

    /**
     * Regenerates recovery codes for the current user, replacing any existing ones.
     *
     * @param request the setup request containing the user's current password (reused DTO)
     * @return the recovery response containing the new set of recovery codes
     * @throws IllegalArgumentException if MFA is not enabled or the password is incorrect
     * @throws EntityNotFoundException if the current user is not found
     */
    public MfaRecoveryResponse regenerateRecoveryCodes(MfaSetupRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        log.debug("regenerateRecoveryCodes called for userId={}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        if (!Boolean.TRUE.equals(user.getMfaEnabled())) {
            throw new IllegalArgumentException("MFA is not enabled");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            log.warn("Recovery code regeneration failed: wrong password for userId={}", userId);
            throw new IllegalArgumentException("Invalid password");
        }

        List<String> recoveryCodes = generateRecoveryCodes();
        user.setMfaRecoveryCodes(encryptRecoveryCodes(recoveryCodes));
        userRepository.save(user);

        log.info("Recovery codes regenerated for userId={}", userId);
        return new MfaRecoveryResponse(recoveryCodes);
    }

    /**
     * Returns the MFA status for the current user including method and recovery code count.
     *
     * @return the MFA status response
     * @throws EntityNotFoundException if the current user is not found
     */
    @Transactional(readOnly = true)
    public MfaStatusResponse getMfaStatus() {
        UUID userId = SecurityUtils.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        String method = user.getMfaMethod() != null ? user.getMfaMethod().name() : MfaMethod.NONE.name();
        Integer remaining = Boolean.TRUE.equals(user.getMfaEnabled()) ? countRecoveryCodes(user) : null;
        return new MfaStatusResponse(Boolean.TRUE.equals(user.getMfaEnabled()), method, remaining);
    }

    // ──────────────────────────────────────────────
    // Admin MFA Reset (Part C)
    // ──────────────────────────────────────────────

    /**
     * Force-resets MFA for a target user without requiring the user's password.
     * This is an admin-only operation used when a user is locked out of their account.
     *
     * @param targetUserId the UUID of the user whose MFA should be reset
     * @throws EntityNotFoundException if the target user is not found
     */
    public void adminResetMfa(UUID targetUserId) {
        log.debug("adminResetMfa called for targetUserId={}", targetUserId);

        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        clearMfaFields(user);
        userRepository.save(user);
        mfaEmailCodeRepository.deleteByUserId(targetUserId);

        log.info("Admin MFA reset for userId={}", targetUserId);
    }

    // ──────────────────────────────────────────────
    // Scheduled Cleanup
    // ──────────────────────────────────────────────

    /**
     * Periodically cleans up expired MFA email codes from the database.
     * Runs every 15 minutes.
     */
    @Scheduled(fixedRate = 900_000) // 15 minutes
    @Transactional
    public void cleanupExpiredCodes() {
        mfaEmailCodeRepository.deleteByExpiresAtBefore(Instant.now());
        log.debug("Expired MFA email codes cleaned up");
    }

    // ──────────────────────────────────────────────
    // Email MFA helpers
    // ──────────────────────────────────────────────

    private String generateEmailCode() {
        SecureRandom random = new SecureRandom();
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < EMAIL_CODE_LENGTH; i++) {
            code.append(random.nextInt(10));
        }
        return code.toString();
    }

    private void saveEmailCode(UUID userId, String code) {
        MfaEmailCode emailCode = MfaEmailCode.builder()
                .userId(userId)
                .codeHash(passwordEncoder.encode(code))
                .expiresAt(Instant.now().plus(EMAIL_CODE_TTL_MINUTES, ChronoUnit.MINUTES))
                .build();
        mfaEmailCodeRepository.save(emailCode);
    }

    private boolean verifyAndConsumeEmailCode(UUID userId, String code) {
        List<MfaEmailCode> validCodes = mfaEmailCodeRepository
                .findByUserIdAndUsedFalseAndExpiresAtAfter(userId, Instant.now());

        for (MfaEmailCode emailCode : validCodes) {
            if (passwordEncoder.matches(code, emailCode.getCodeHash())) {
                emailCode.setUsed(true);
                mfaEmailCodeRepository.save(emailCode);
                return true;
            }
        }
        return false;
    }

    /**
     * Masks an email address for display in MFA challenge responses.
     * For example, {@code "adam@example.com"} becomes {@code "a***@example.com"}.
     *
     * @param email the full email address
     * @return the masked email address
     */
    public String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "***";
        }
        int atIndex = email.indexOf('@');
        if (atIndex <= 1) {
            return email.charAt(0) + "***" + email.substring(atIndex);
        }
        return email.charAt(0) + "***" + email.substring(atIndex);
    }

    // ──────────────────────────────────────────────
    // TOTP helpers
    // ──────────────────────────────────────────────

    private boolean verifyTotpCode(String secret, String code) {
        TimeProvider timeProvider = new SystemTimeProvider();
        CodeGenerator codeGenerator = new DefaultCodeGenerator();
        CodeVerifier verifier = new DefaultCodeVerifier(codeGenerator, timeProvider);
        return verifier.isValidCode(secret, code);
    }

    // ──────────────────────────────────────────────
    // Shared helpers
    // ──────────────────────────────────────────────

    private boolean consumeRecoveryCode(User user, String code) {
        if (user.getMfaRecoveryCodes() == null) {
            return false;
        }

        List<String> codes = decryptRecoveryCodes(user.getMfaRecoveryCodes());
        if (codes.contains(code)) {
            codes = new ArrayList<>(codes);
            codes.remove(code);
            user.setMfaRecoveryCodes(encryptRecoveryCodes(codes));
            userRepository.save(user);
            log.info("Recovery code consumed for userId={}, remaining={}", user.getId(), codes.size());
            return true;
        }
        return false;
    }

    private void clearMfaFields(User user) {
        user.setMfaEnabled(false);
        user.setMfaMethod(MfaMethod.NONE);
        user.setMfaSecret(null);
        user.setMfaRecoveryCodes(null);
    }

    private List<String> generateRecoveryCodes() {
        List<String> codes = new ArrayList<>();
        SecureRandom random = new SecureRandom();
        for (int i = 0; i < RECOVERY_CODE_COUNT; i++) {
            StringBuilder code = new StringBuilder();
            for (int j = 0; j < 8; j++) {
                code.append(random.nextInt(10));
            }
            codes.add(code.toString());
        }
        return codes;
    }

    private String encryptRecoveryCodes(List<String> codes) {
        try {
            String json = objectMapper.writeValueAsString(codes);
            return encryptionService.encrypt(json);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize recovery codes", e);
        }
    }

    private List<String> decryptRecoveryCodes(String encrypted) {
        try {
            String json = encryptionService.decrypt(encrypted);
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize recovery codes", e);
        }
    }

    private Integer countRecoveryCodes(User user) {
        if (user.getMfaRecoveryCodes() == null) {
            return 0;
        }
        try {
            List<String> codes = decryptRecoveryCodes(user.getMfaRecoveryCodes());
            return codes.size();
        } catch (Exception e) {
            log.warn("Failed to count recovery codes for userId={}: {}", user.getId(), e.getMessage());
            return 0;
        }
    }

    private String generateQrCodeUri(String secret, String email) {
        QrData data = new QrData.Builder()
                .label(email)
                .secret(secret)
                .issuer(ISSUER)
                .algorithm(HashingAlgorithm.SHA1)
                .digits(6)
                .period(30)
                .build();

        try {
            ZxingPngQrGenerator generator = new ZxingPngQrGenerator();
            byte[] imageData = generator.generate(data);
            return "data:image/png;base64," + java.util.Base64.getEncoder().encodeToString(imageData);
        } catch (QrGenerationException e) {
            log.warn("QR code generation failed, returning otpauth URI instead", e);
            return data.getUri();
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
                user.getCreatedAt(),
                user.getMfaEnabled(),
                user.getMfaMethod() != null ? user.getMfaMethod().name() : "NONE"
        );
    }
}
