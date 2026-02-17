package com.codeops.service;

import com.codeops.dto.request.MfaLoginRequest;
import com.codeops.dto.request.MfaSetupRequest;
import com.codeops.dto.request.MfaVerifyRequest;
import com.codeops.dto.response.AuthResponse;
import com.codeops.dto.response.MfaRecoveryResponse;
import com.codeops.dto.response.MfaSetupResponse;
import com.codeops.dto.response.MfaStatusResponse;
import com.codeops.dto.response.UserResponse;
import com.codeops.entity.User;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Manages TOTP-based multi-factor authentication (MFA) for user accounts.
 *
 * <p>Provides setup, verification, login challenge resolution, recovery code management,
 * and MFA disablement. TOTP secrets and recovery codes are encrypted at rest via
 * {@link EncryptionService} using AES-256-GCM. Recovery codes are stored as an encrypted
 * JSON array on the User entity.</p>
 *
 * <p>The MFA login flow is two-phase:
 * <ol>
 *   <li>User submits email + password → receives an MFA challenge token (5-minute JWT)</li>
 *   <li>User submits challenge token + TOTP code → receives full access + refresh tokens</li>
 * </ol>
 * </p>
 *
 * @see JwtTokenProvider#generateMfaChallengeToken(User)
 * @see EncryptionService
 */
@Service
@RequiredArgsConstructor
@Transactional
public class MfaService {

    private static final Logger log = LoggerFactory.getLogger(MfaService.class);
    private static final String ISSUER = "CodeOps";
    private static final int RECOVERY_CODE_COUNT = 8;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final EncryptionService encryptionService;
    private final TeamMemberRepository teamMemberRepository;
    private final ObjectMapper objectMapper;

    /**
     * Initiates MFA setup for the current user by generating a TOTP secret and recovery codes.
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
     * Verifies a TOTP code against the stored (but not yet active) secret and enables MFA.
     *
     * <p>This is the second step of MFA setup. After calling {@link #setupMfa(MfaSetupRequest)},
     * the user must submit a valid TOTP code generated by their authenticator app. If the code
     * is valid, MFA is enabled on the account.</p>
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
        userRepository.save(user);

        log.info("MFA enabled for userId={}", userId);
        return new MfaStatusResponse(true);
    }

    /**
     * Completes the MFA login flow by verifying a TOTP code (or recovery code) against the
     * challenge token, then issuing full access and refresh tokens.
     *
     * <p>The challenge token is a short-lived JWT issued during the first phase of MFA login
     * (after password verification). The code can be either a 6-digit TOTP code or an 8-character
     * recovery code.</p>
     *
     * @param request the MFA login request containing the challenge token and TOTP/recovery code
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

        String decryptedSecret = encryptionService.decrypt(user.getMfaSecret());
        String code = request.code();

        // Try TOTP code first, then recovery code
        if (!verifyTotpCode(decryptedSecret, code) && !consumeRecoveryCode(user, code)) {
            log.warn("MFA login failed: invalid code for userId={}", userId);
            throw new IllegalArgumentException("Invalid MFA code");
        }

        user.setLastLoginAt(java.time.Instant.now());
        userRepository.save(user);

        List<String> roles = getUserRoles(userId);
        String token = jwtTokenProvider.generateToken(user, roles);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user);

        log.info("MFA login success: userId={}", userId);
        return new AuthResponse(token, refreshToken, mapToUserResponse(user));
    }

    /**
     * Disables MFA for the current user, clearing the stored secret and recovery codes.
     *
     * <p>Requires the user's current password for re-authentication.</p>
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

        user.setMfaEnabled(false);
        user.setMfaSecret(null);
        user.setMfaRecoveryCodes(null);
        userRepository.save(user);

        log.info("MFA disabled for userId={}", userId);
        return new MfaStatusResponse(false);
    }

    /**
     * Regenerates recovery codes for the current user, replacing any existing ones.
     *
     * <p>Requires the user's current password for re-authentication. MFA must be enabled.</p>
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
     * Returns the MFA status for the current user.
     *
     * @return the MFA status response indicating whether MFA is enabled
     * @throws EntityNotFoundException if the current user is not found
     */
    @Transactional(readOnly = true)
    public MfaStatusResponse getMfaStatus() {
        UUID userId = SecurityUtils.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        return new MfaStatusResponse(Boolean.TRUE.equals(user.getMfaEnabled()));
    }

    private boolean verifyTotpCode(String secret, String code) {
        TimeProvider timeProvider = new SystemTimeProvider();
        CodeGenerator codeGenerator = new DefaultCodeGenerator();
        CodeVerifier verifier = new DefaultCodeVerifier(codeGenerator, timeProvider);
        return verifier.isValidCode(secret, code);
    }

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

    private List<String> generateRecoveryCodes() {
        List<String> codes = new ArrayList<>();
        java.security.SecureRandom random = new java.security.SecureRandom();
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
                user.getMfaEnabled()
        );
    }
}
