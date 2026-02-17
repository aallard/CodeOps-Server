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
import com.codeops.entity.MfaEmailCode;
import com.codeops.entity.User;
import com.codeops.entity.enums.MfaMethod;
import com.codeops.notification.EmailService;
import com.codeops.repository.MfaEmailCodeRepository;
import com.codeops.repository.TeamMemberRepository;
import com.codeops.repository.UserRepository;
import com.codeops.security.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MfaServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private EncryptionService encryptionService;
    @Mock private TeamMemberRepository teamMemberRepository;
    @Mock private MfaEmailCodeRepository mfaEmailCodeRepository;
    @Mock private EmailService emailService;
    @Spy private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private MfaService mfaService;

    private User testUser;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        testUser = User.builder()
                .email("test@codeops.dev")
                .passwordHash("encoded-password")
                .displayName("Test User")
                .isActive(true)
                .mfaEnabled(false)
                .mfaMethod(MfaMethod.NONE)
                .build();
        testUser.setId(userId);
        testUser.setCreatedAt(Instant.now());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void setSecurityContext(UUID userId) {
        var auth = new UsernamePasswordAuthenticationToken(userId, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    // ──────────────────────────────────────────────
    // setupMfa (TOTP)
    // ──────────────────────────────────────────────

    @Test
    void setupMfa_success_returnsSecretAndQrAndRecoveryCodes() {
        setSecurityContext(userId);
        MfaSetupRequest request = new MfaSetupRequest("password");

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password", "encoded-password")).thenReturn(true);
        when(encryptionService.encrypt(anyString())).thenAnswer(inv -> "encrypted:" + inv.getArgument(0));

        MfaSetupResponse response = mfaService.setupMfa(request);

        assertNotNull(response.secret());
        assertFalse(response.secret().isEmpty());
        assertNotNull(response.qrCodeUri());
        assertNotNull(response.recoveryCodes());
        assertEquals(8, response.recoveryCodes().size());
        verify(userRepository).save(testUser);
        assertNotNull(testUser.getMfaSecret());
        assertNotNull(testUser.getMfaRecoveryCodes());
    }

    @Test
    void setupMfa_mfaAlreadyEnabled_throws() {
        setSecurityContext(userId);
        testUser.setMfaEnabled(true);
        MfaSetupRequest request = new MfaSetupRequest("password");

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        assertThrows(IllegalArgumentException.class, () -> mfaService.setupMfa(request));
    }

    @Test
    void setupMfa_wrongPassword_throws() {
        setSecurityContext(userId);
        MfaSetupRequest request = new MfaSetupRequest("wrong");

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrong", "encoded-password")).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> mfaService.setupMfa(request));
    }

    @Test
    void setupMfa_userNotFound_throws() {
        setSecurityContext(userId);
        MfaSetupRequest request = new MfaSetupRequest("password");

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> mfaService.setupMfa(request));
    }

    @Test
    void setupMfa_recoveryCodesAreEightDigits() {
        setSecurityContext(userId);
        MfaSetupRequest request = new MfaSetupRequest("password");

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password", "encoded-password")).thenReturn(true);
        when(encryptionService.encrypt(anyString())).thenReturn("encrypted");

        MfaSetupResponse response = mfaService.setupMfa(request);

        for (String code : response.recoveryCodes()) {
            assertEquals(8, code.length());
            assertTrue(code.matches("\\d{8}"));
        }
    }

    // ──────────────────────────────────────────────
    // verifyAndEnableMfa (TOTP)
    // ──────────────────────────────────────────────

    @Test
    void verifyAndEnableMfa_invalidCode_throws() {
        setSecurityContext(userId);
        testUser.setMfaSecret("encrypted-secret");
        MfaVerifyRequest request = new MfaVerifyRequest("000000");

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(encryptionService.decrypt("encrypted-secret")).thenReturn("JBSWY3DPEHPK3PXP");

        assertThrows(IllegalArgumentException.class, () -> mfaService.verifyAndEnableMfa(request));
    }

    @Test
    void verifyAndEnableMfa_mfaAlreadyEnabled_throws() {
        setSecurityContext(userId);
        testUser.setMfaEnabled(true);
        MfaVerifyRequest request = new MfaVerifyRequest("123456");

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        assertThrows(IllegalArgumentException.class, () -> mfaService.verifyAndEnableMfa(request));
    }

    @Test
    void verifyAndEnableMfa_noSecretConfigured_throws() {
        setSecurityContext(userId);
        testUser.setMfaSecret(null);
        MfaVerifyRequest request = new MfaVerifyRequest("123456");

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        assertThrows(IllegalArgumentException.class, () -> mfaService.verifyAndEnableMfa(request));
    }

    @Test
    void verifyAndEnableMfa_userNotFound_throws() {
        setSecurityContext(userId);
        MfaVerifyRequest request = new MfaVerifyRequest("123456");

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> mfaService.verifyAndEnableMfa(request));
    }

    // ──────────────────────────────────────────────
    // verifyMfaLogin (TOTP)
    // ──────────────────────────────────────────────

    @Test
    void verifyMfaLogin_invalidChallengeToken_throws() {
        MfaLoginRequest request = new MfaLoginRequest("bad-token", "123456");

        when(jwtTokenProvider.validateToken("bad-token")).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> mfaService.verifyMfaLogin(request));
    }

    @Test
    void verifyMfaLogin_notMfaChallengeToken_throws() {
        MfaLoginRequest request = new MfaLoginRequest("access-token", "123456");

        when(jwtTokenProvider.validateToken("access-token")).thenReturn(true);
        when(jwtTokenProvider.isMfaChallengeToken("access-token")).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> mfaService.verifyMfaLogin(request));
    }

    @Test
    void verifyMfaLogin_mfaNotEnabled_throws() {
        MfaLoginRequest request = new MfaLoginRequest("challenge-token", "123456");

        when(jwtTokenProvider.validateToken("challenge-token")).thenReturn(true);
        when(jwtTokenProvider.isMfaChallengeToken("challenge-token")).thenReturn(true);
        when(jwtTokenProvider.getUserIdFromToken("challenge-token")).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        assertThrows(IllegalArgumentException.class, () -> mfaService.verifyMfaLogin(request));
    }

    @Test
    void verifyMfaLogin_userNotFound_throws() {
        MfaLoginRequest request = new MfaLoginRequest("challenge-token", "123456");

        when(jwtTokenProvider.validateToken("challenge-token")).thenReturn(true);
        when(jwtTokenProvider.isMfaChallengeToken("challenge-token")).thenReturn(true);
        when(jwtTokenProvider.getUserIdFromToken("challenge-token")).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> mfaService.verifyMfaLogin(request));
    }

    @Test
    void verifyMfaLogin_invalidTotpCode_throws() {
        testUser.setMfaEnabled(true);
        testUser.setMfaMethod(MfaMethod.TOTP);
        testUser.setMfaSecret("encrypted-secret");
        MfaLoginRequest request = new MfaLoginRequest("challenge-token", "000000");

        when(jwtTokenProvider.validateToken("challenge-token")).thenReturn(true);
        when(jwtTokenProvider.isMfaChallengeToken("challenge-token")).thenReturn(true);
        when(jwtTokenProvider.getUserIdFromToken("challenge-token")).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(encryptionService.decrypt("encrypted-secret")).thenReturn("JBSWY3DPEHPK3PXP");

        assertThrows(IllegalArgumentException.class, () -> mfaService.verifyMfaLogin(request));
    }

    @Test
    void verifyMfaLogin_validRecoveryCode_succeeds() {
        testUser.setMfaEnabled(true);
        testUser.setMfaMethod(MfaMethod.TOTP);
        testUser.setMfaSecret("encrypted-secret");
        testUser.setMfaRecoveryCodes("encrypted-codes");
        MfaLoginRequest request = new MfaLoginRequest("challenge-token", "12345678");

        when(jwtTokenProvider.validateToken("challenge-token")).thenReturn(true);
        when(jwtTokenProvider.isMfaChallengeToken("challenge-token")).thenReturn(true);
        when(jwtTokenProvider.getUserIdFromToken("challenge-token")).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(encryptionService.decrypt("encrypted-secret")).thenReturn("JBSWY3DPEHPK3PXP");
        when(encryptionService.decrypt("encrypted-codes")).thenReturn("[\"12345678\",\"87654321\"]");
        when(encryptionService.encrypt(anyString())).thenReturn("encrypted-updated-codes");
        when(teamMemberRepository.findByUserId(userId)).thenReturn(List.of());
        when(jwtTokenProvider.generateToken(any(User.class), anyList())).thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshToken(any(User.class))).thenReturn("refresh-token");

        AuthResponse response = mfaService.verifyMfaLogin(request);

        assertNotNull(response);
        assertEquals("access-token", response.token());
        assertEquals("refresh-token", response.refreshToken());
        assertNotNull(response.user());
        verify(userRepository, atLeast(1)).save(testUser);
    }

    @Test
    void verifyMfaLogin_invalidRecoveryCode_throws() {
        testUser.setMfaEnabled(true);
        testUser.setMfaMethod(MfaMethod.TOTP);
        testUser.setMfaSecret("encrypted-secret");
        testUser.setMfaRecoveryCodes("encrypted-codes");
        MfaLoginRequest request = new MfaLoginRequest("challenge-token", "99999999");

        when(jwtTokenProvider.validateToken("challenge-token")).thenReturn(true);
        when(jwtTokenProvider.isMfaChallengeToken("challenge-token")).thenReturn(true);
        when(jwtTokenProvider.getUserIdFromToken("challenge-token")).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(encryptionService.decrypt("encrypted-secret")).thenReturn("JBSWY3DPEHPK3PXP");
        when(encryptionService.decrypt("encrypted-codes")).thenReturn("[\"12345678\",\"87654321\"]");

        assertThrows(IllegalArgumentException.class, () -> mfaService.verifyMfaLogin(request));
    }

    // ──────────────────────────────────────────────
    // verifyMfaLogin (Email)
    // ──────────────────────────────────────────────

    @Test
    void verifyMfaLogin_emailMethod_validCode_succeeds() {
        testUser.setMfaEnabled(true);
        testUser.setMfaMethod(MfaMethod.EMAIL);
        MfaLoginRequest request = new MfaLoginRequest("challenge-token", "654321");

        MfaEmailCode emailCode = MfaEmailCode.builder()
                .userId(userId)
                .codeHash("hashed-654321")
                .expiresAt(Instant.now().plus(5, ChronoUnit.MINUTES))
                .used(false)
                .build();

        when(jwtTokenProvider.validateToken("challenge-token")).thenReturn(true);
        when(jwtTokenProvider.isMfaChallengeToken("challenge-token")).thenReturn(true);
        when(jwtTokenProvider.getUserIdFromToken("challenge-token")).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(mfaEmailCodeRepository.findByUserIdAndUsedFalseAndExpiresAtAfter(eq(userId), any(Instant.class)))
                .thenReturn(List.of(emailCode));
        when(passwordEncoder.matches("654321", "hashed-654321")).thenReturn(true);
        when(teamMemberRepository.findByUserId(userId)).thenReturn(List.of());
        when(jwtTokenProvider.generateToken(any(User.class), anyList())).thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshToken(any(User.class))).thenReturn("refresh-token");

        AuthResponse response = mfaService.verifyMfaLogin(request);

        assertNotNull(response.token());
        assertTrue(emailCode.isUsed());
        verify(mfaEmailCodeRepository).save(emailCode);
    }

    @Test
    void verifyMfaLogin_emailMethod_invalidCode_throws() {
        testUser.setMfaEnabled(true);
        testUser.setMfaMethod(MfaMethod.EMAIL);
        MfaLoginRequest request = new MfaLoginRequest("challenge-token", "000000");

        when(jwtTokenProvider.validateToken("challenge-token")).thenReturn(true);
        when(jwtTokenProvider.isMfaChallengeToken("challenge-token")).thenReturn(true);
        when(jwtTokenProvider.getUserIdFromToken("challenge-token")).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(mfaEmailCodeRepository.findByUserIdAndUsedFalseAndExpiresAtAfter(eq(userId), any(Instant.class)))
                .thenReturn(List.of());

        assertThrows(IllegalArgumentException.class, () -> mfaService.verifyMfaLogin(request));
    }

    @Test
    void verifyMfaLogin_emailMethod_recoveryCode_succeeds() {
        testUser.setMfaEnabled(true);
        testUser.setMfaMethod(MfaMethod.EMAIL);
        testUser.setMfaRecoveryCodes("encrypted-codes");
        MfaLoginRequest request = new MfaLoginRequest("challenge-token", "12345678");

        when(jwtTokenProvider.validateToken("challenge-token")).thenReturn(true);
        when(jwtTokenProvider.isMfaChallengeToken("challenge-token")).thenReturn(true);
        when(jwtTokenProvider.getUserIdFromToken("challenge-token")).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(mfaEmailCodeRepository.findByUserIdAndUsedFalseAndExpiresAtAfter(eq(userId), any(Instant.class)))
                .thenReturn(List.of());
        when(encryptionService.decrypt("encrypted-codes")).thenReturn("[\"12345678\",\"87654321\"]");
        when(encryptionService.encrypt(anyString())).thenReturn("encrypted-updated");
        when(teamMemberRepository.findByUserId(userId)).thenReturn(List.of());
        when(jwtTokenProvider.generateToken(any(User.class), anyList())).thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshToken(any(User.class))).thenReturn("refresh-token");

        AuthResponse response = mfaService.verifyMfaLogin(request);

        assertNotNull(response.token());
    }

    // ──────────────────────────────────────────────
    // setupEmailMfa
    // ──────────────────────────────────────────────

    @Test
    void setupEmailMfa_success_returnsRecoveryCodes() {
        setSecurityContext(userId);
        MfaEmailSetupRequest request = new MfaEmailSetupRequest("password");

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password", "encoded-password")).thenReturn(true);
        when(encryptionService.encrypt(anyString())).thenReturn("encrypted");
        when(passwordEncoder.encode(anyString())).thenReturn("hashed-code");

        MfaRecoveryResponse response = mfaService.setupEmailMfa(request);

        assertNotNull(response.recoveryCodes());
        assertEquals(8, response.recoveryCodes().size());
        assertEquals(MfaMethod.EMAIL, testUser.getMfaMethod());
        assertFalse(testUser.getMfaEnabled());
        verify(emailService).sendMfaCode(eq("test@codeops.dev"), anyString());
        verify(mfaEmailCodeRepository).save(any(MfaEmailCode.class));
    }

    @Test
    void setupEmailMfa_mfaAlreadyEnabled_throws() {
        setSecurityContext(userId);
        testUser.setMfaEnabled(true);
        MfaEmailSetupRequest request = new MfaEmailSetupRequest("password");

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        assertThrows(IllegalArgumentException.class, () -> mfaService.setupEmailMfa(request));
    }

    @Test
    void setupEmailMfa_wrongPassword_throws() {
        setSecurityContext(userId);
        MfaEmailSetupRequest request = new MfaEmailSetupRequest("wrong");

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrong", "encoded-password")).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> mfaService.setupEmailMfa(request));
    }

    @Test
    void setupEmailMfa_userNotFound_throws() {
        setSecurityContext(userId);
        MfaEmailSetupRequest request = new MfaEmailSetupRequest("password");

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> mfaService.setupEmailMfa(request));
    }

    // ──────────────────────────────────────────────
    // verifyEmailSetupAndEnable
    // ──────────────────────────────────────────────

    @Test
    void verifyEmailSetupAndEnable_validCode_enablesMfa() {
        setSecurityContext(userId);
        testUser.setMfaMethod(MfaMethod.EMAIL);
        testUser.setMfaRecoveryCodes("encrypted-codes");
        MfaVerifyRequest request = new MfaVerifyRequest("123456");

        MfaEmailCode emailCode = MfaEmailCode.builder()
                .userId(userId)
                .codeHash("hashed-123456")
                .expiresAt(Instant.now().plus(5, ChronoUnit.MINUTES))
                .used(false)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(mfaEmailCodeRepository.findByUserIdAndUsedFalseAndExpiresAtAfter(eq(userId), any(Instant.class)))
                .thenReturn(List.of(emailCode));
        when(passwordEncoder.matches("123456", "hashed-123456")).thenReturn(true);
        when(encryptionService.decrypt("encrypted-codes")).thenReturn("[\"11111111\",\"22222222\"]");

        MfaStatusResponse response = mfaService.verifyEmailSetupAndEnable(request);

        assertTrue(response.mfaEnabled());
        assertEquals("EMAIL", response.mfaMethod());
        assertEquals(2, response.recoveryCodesRemaining());
        assertTrue(testUser.getMfaEnabled());
    }

    @Test
    void verifyEmailSetupAndEnable_invalidCode_throws() {
        setSecurityContext(userId);
        testUser.setMfaMethod(MfaMethod.EMAIL);
        MfaVerifyRequest request = new MfaVerifyRequest("000000");

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(mfaEmailCodeRepository.findByUserIdAndUsedFalseAndExpiresAtAfter(eq(userId), any(Instant.class)))
                .thenReturn(List.of());

        assertThrows(IllegalArgumentException.class, () -> mfaService.verifyEmailSetupAndEnable(request));
    }

    @Test
    void verifyEmailSetupAndEnable_mfaAlreadyEnabled_throws() {
        setSecurityContext(userId);
        testUser.setMfaEnabled(true);
        MfaVerifyRequest request = new MfaVerifyRequest("123456");

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        assertThrows(IllegalArgumentException.class, () -> mfaService.verifyEmailSetupAndEnable(request));
    }

    @Test
    void verifyEmailSetupAndEnable_notEmailMethod_throws() {
        setSecurityContext(userId);
        testUser.setMfaMethod(MfaMethod.NONE);
        MfaVerifyRequest request = new MfaVerifyRequest("123456");

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        assertThrows(IllegalArgumentException.class, () -> mfaService.verifyEmailSetupAndEnable(request));
    }

    // ──────────────────────────────────────────────
    // sendLoginMfaCode
    // ──────────────────────────────────────────────

    @Test
    void sendLoginMfaCode_success_sendsEmail() {
        testUser.setMfaEnabled(true);
        testUser.setMfaMethod(MfaMethod.EMAIL);
        MfaResendRequest request = new MfaResendRequest("challenge-token");

        when(jwtTokenProvider.validateToken("challenge-token")).thenReturn(true);
        when(jwtTokenProvider.isMfaChallengeToken("challenge-token")).thenReturn(true);
        when(jwtTokenProvider.getUserIdFromToken("challenge-token")).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode(anyString())).thenReturn("hashed-code");

        mfaService.sendLoginMfaCode(request);

        verify(emailService).sendMfaCode(eq("test@codeops.dev"), anyString());
        verify(mfaEmailCodeRepository).save(any(MfaEmailCode.class));
    }

    @Test
    void sendLoginMfaCode_invalidToken_throws() {
        MfaResendRequest request = new MfaResendRequest("bad-token");

        when(jwtTokenProvider.validateToken("bad-token")).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> mfaService.sendLoginMfaCode(request));
    }

    @Test
    void sendLoginMfaCode_notEmailMethod_throws() {
        testUser.setMfaEnabled(true);
        testUser.setMfaMethod(MfaMethod.TOTP);
        MfaResendRequest request = new MfaResendRequest("challenge-token");

        when(jwtTokenProvider.validateToken("challenge-token")).thenReturn(true);
        when(jwtTokenProvider.isMfaChallengeToken("challenge-token")).thenReturn(true);
        when(jwtTokenProvider.getUserIdFromToken("challenge-token")).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        assertThrows(IllegalArgumentException.class, () -> mfaService.sendLoginMfaCode(request));
    }

    // ──────────────────────────────────────────────
    // disableMfa
    // ──────────────────────────────────────────────

    @Test
    void disableMfa_success_clearsMfaFields() {
        setSecurityContext(userId);
        testUser.setMfaEnabled(true);
        testUser.setMfaMethod(MfaMethod.TOTP);
        testUser.setMfaSecret("encrypted-secret");
        testUser.setMfaRecoveryCodes("encrypted-codes");
        MfaSetupRequest request = new MfaSetupRequest("password");

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password", "encoded-password")).thenReturn(true);

        MfaStatusResponse response = mfaService.disableMfa(request);

        assertFalse(response.mfaEnabled());
        assertEquals("NONE", response.mfaMethod());
        assertNull(response.recoveryCodesRemaining());
        assertFalse(testUser.getMfaEnabled());
        assertEquals(MfaMethod.NONE, testUser.getMfaMethod());
        assertNull(testUser.getMfaSecret());
        assertNull(testUser.getMfaRecoveryCodes());
        verify(userRepository).save(testUser);
        verify(mfaEmailCodeRepository).deleteByUserId(userId);
    }

    @Test
    void disableMfa_mfaNotEnabled_throws() {
        setSecurityContext(userId);
        testUser.setMfaEnabled(false);
        MfaSetupRequest request = new MfaSetupRequest("password");

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        assertThrows(IllegalArgumentException.class, () -> mfaService.disableMfa(request));
    }

    @Test
    void disableMfa_wrongPassword_throws() {
        setSecurityContext(userId);
        testUser.setMfaEnabled(true);
        MfaSetupRequest request = new MfaSetupRequest("wrong");

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrong", "encoded-password")).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> mfaService.disableMfa(request));
    }

    @Test
    void disableMfa_userNotFound_throws() {
        setSecurityContext(userId);
        MfaSetupRequest request = new MfaSetupRequest("password");

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> mfaService.disableMfa(request));
    }

    // ──────────────────────────────────────────────
    // regenerateRecoveryCodes
    // ──────────────────────────────────────────────

    @Test
    void regenerateRecoveryCodes_success_returnsNewCodes() {
        setSecurityContext(userId);
        testUser.setMfaEnabled(true);
        MfaSetupRequest request = new MfaSetupRequest("password");

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password", "encoded-password")).thenReturn(true);
        when(encryptionService.encrypt(anyString())).thenReturn("encrypted");

        MfaRecoveryResponse response = mfaService.regenerateRecoveryCodes(request);

        assertNotNull(response.recoveryCodes());
        assertEquals(8, response.recoveryCodes().size());
        verify(userRepository).save(testUser);
    }

    @Test
    void regenerateRecoveryCodes_mfaNotEnabled_throws() {
        setSecurityContext(userId);
        testUser.setMfaEnabled(false);
        MfaSetupRequest request = new MfaSetupRequest("password");

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        assertThrows(IllegalArgumentException.class, () -> mfaService.regenerateRecoveryCodes(request));
    }

    @Test
    void regenerateRecoveryCodes_wrongPassword_throws() {
        setSecurityContext(userId);
        testUser.setMfaEnabled(true);
        MfaSetupRequest request = new MfaSetupRequest("wrong");

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrong", "encoded-password")).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> mfaService.regenerateRecoveryCodes(request));
    }

    @Test
    void regenerateRecoveryCodes_userNotFound_throws() {
        setSecurityContext(userId);
        MfaSetupRequest request = new MfaSetupRequest("password");

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> mfaService.regenerateRecoveryCodes(request));
    }

    // ──────────────────────────────────────────────
    // getMfaStatus
    // ──────────────────────────────────────────────

    @Test
    void getMfaStatus_mfaEnabled_returnsTrue() {
        setSecurityContext(userId);
        testUser.setMfaEnabled(true);
        testUser.setMfaMethod(MfaMethod.TOTP);
        testUser.setMfaRecoveryCodes("encrypted-codes");

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(encryptionService.decrypt("encrypted-codes")).thenReturn("[\"11111111\",\"22222222\",\"33333333\"]");

        MfaStatusResponse response = mfaService.getMfaStatus();

        assertTrue(response.mfaEnabled());
        assertEquals("TOTP", response.mfaMethod());
        assertEquals(3, response.recoveryCodesRemaining());
    }

    @Test
    void getMfaStatus_mfaDisabled_returnsFalse() {
        setSecurityContext(userId);
        testUser.setMfaEnabled(false);
        testUser.setMfaMethod(MfaMethod.NONE);

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        MfaStatusResponse response = mfaService.getMfaStatus();

        assertFalse(response.mfaEnabled());
        assertEquals("NONE", response.mfaMethod());
        assertNull(response.recoveryCodesRemaining());
    }

    @Test
    void getMfaStatus_userNotFound_throws() {
        setSecurityContext(userId);

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> mfaService.getMfaStatus());
    }

    // ──────────────────────────────────────────────
    // adminResetMfa
    // ──────────────────────────────────────────────

    @Test
    void adminResetMfa_success_clearsMfaFields() {
        testUser.setMfaEnabled(true);
        testUser.setMfaMethod(MfaMethod.TOTP);
        testUser.setMfaSecret("encrypted-secret");
        testUser.setMfaRecoveryCodes("encrypted-codes");

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        mfaService.adminResetMfa(userId);

        assertFalse(testUser.getMfaEnabled());
        assertEquals(MfaMethod.NONE, testUser.getMfaMethod());
        assertNull(testUser.getMfaSecret());
        assertNull(testUser.getMfaRecoveryCodes());
        verify(userRepository).save(testUser);
        verify(mfaEmailCodeRepository).deleteByUserId(userId);
    }

    @Test
    void adminResetMfa_userNotFound_throws() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> mfaService.adminResetMfa(userId));
    }

    // ──────────────────────────────────────────────
    // maskEmail
    // ──────────────────────────────────────────────

    @Test
    void maskEmail_standard_masksCorrectly() {
        assertEquals("a***@example.com", mfaService.maskEmail("adam@example.com"));
    }

    @Test
    void maskEmail_shortLocal_masksCorrectly() {
        assertEquals("a***@test.com", mfaService.maskEmail("a@test.com"));
    }

    @Test
    void maskEmail_null_returnsStars() {
        assertEquals("***", mfaService.maskEmail(null));
    }

    @Test
    void maskEmail_noAt_returnsStars() {
        assertEquals("***", mfaService.maskEmail("noemail"));
    }
}
