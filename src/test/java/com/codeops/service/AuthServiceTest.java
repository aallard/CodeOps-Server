package com.codeops.service;

import com.codeops.dto.request.ChangePasswordRequest;
import com.codeops.dto.request.LoginRequest;
import com.codeops.dto.request.RefreshTokenRequest;
import com.codeops.dto.request.RegisterRequest;
import com.codeops.dto.response.AuthResponse;
import com.codeops.entity.TeamMember;
import com.codeops.entity.User;
import com.codeops.entity.enums.TeamRole;
import com.codeops.repository.TeamMemberRepository;
import com.codeops.repository.UserRepository;
import com.codeops.security.JwtTokenProvider;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private TeamMemberRepository teamMemberRepository;

    @InjectMocks
    private AuthService authService;

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
                .build();
        testUser.setId(userId);
        testUser.setCreatedAt(Instant.now());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void register_success() {
        RegisterRequest request = new RegisterRequest("new@codeops.dev", "StrongP@ss1", "New User");
        when(userRepository.existsByEmail("new@codeops.dev")).thenReturn(false);
        when(passwordEncoder.encode("StrongP@ss1")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(UUID.randomUUID());
            u.setCreatedAt(Instant.now());
            return u;
        });
        when(jwtTokenProvider.generateToken(any(User.class), anyList())).thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshToken(any(User.class))).thenReturn("refresh-token");

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals("access-token", response.token());
        assertEquals("refresh-token", response.refreshToken());
        assertEquals("New User", response.user().displayName());
    }

    @Test
    void register_duplicateEmail_throws() {
        RegisterRequest request = new RegisterRequest("existing@codeops.dev", "StrongP@ss1", "User");
        when(userRepository.existsByEmail("existing@codeops.dev")).thenReturn(true);
        assertThrows(IllegalArgumentException.class, () -> authService.register(request));
    }

    @Test
    void register_weakPassword_noUppercase_throws() {
        RegisterRequest request = new RegisterRequest("new@test.com", "weakpass1!", "User");
        when(userRepository.existsByEmail("new@test.com")).thenReturn(false);
        assertThrows(IllegalArgumentException.class, () -> authService.register(request));
    }

    @Test
    void register_weakPassword_noDigit_throws() {
        RegisterRequest request = new RegisterRequest("new@test.com", "WeakPass!", "User");
        when(userRepository.existsByEmail("new@test.com")).thenReturn(false);
        assertThrows(IllegalArgumentException.class, () -> authService.register(request));
    }

    @Test
    void register_weakPassword_noSpecialChar_throws() {
        RegisterRequest request = new RegisterRequest("new@test.com", "WeakPass1", "User");
        when(userRepository.existsByEmail("new@test.com")).thenReturn(false);
        assertThrows(IllegalArgumentException.class, () -> authService.register(request));
    }

    @Test
    void register_weakPassword_tooShort_throws() {
        RegisterRequest request = new RegisterRequest("new@test.com", "", "User");
        when(userRepository.existsByEmail("new@test.com")).thenReturn(false);
        assertThrows(IllegalArgumentException.class, () -> authService.register(request));
    }

    @Test
    void login_success() {
        LoginRequest request = new LoginRequest("test@codeops.dev", "password");
        when(userRepository.findByEmail("test@codeops.dev")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password", "encoded-password")).thenReturn(true);
        when(teamMemberRepository.findByUserId(userId)).thenReturn(List.of());
        when(jwtTokenProvider.generateToken(eq(testUser), anyList())).thenReturn("token");
        when(jwtTokenProvider.generateRefreshToken(testUser)).thenReturn("refresh");

        AuthResponse response = authService.login(request);

        assertEquals("token", response.token());
        verify(userRepository).save(testUser);
    }

    @Test
    void login_unknownEmail_throws() {
        LoginRequest request = new LoginRequest("unknown@test.com", "password");
        when(userRepository.findByEmail("unknown@test.com")).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> authService.login(request));
    }

    @Test
    void login_deactivatedAccount_throws() {
        testUser.setIsActive(false);
        LoginRequest request = new LoginRequest("test@codeops.dev", "password");
        when(userRepository.findByEmail("test@codeops.dev")).thenReturn(Optional.of(testUser));
        assertThrows(IllegalArgumentException.class, () -> authService.login(request));
    }

    @Test
    void login_wrongPassword_throws() {
        LoginRequest request = new LoginRequest("test@codeops.dev", "wrong");
        when(userRepository.findByEmail("test@codeops.dev")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrong", "encoded-password")).thenReturn(false);
        assertThrows(IllegalArgumentException.class, () -> authService.login(request));
    }

    @Test
    void login_includesRolesFromTeamMemberships() {
        LoginRequest request = new LoginRequest("test@codeops.dev", "password");
        when(userRepository.findByEmail("test@codeops.dev")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password", "encoded-password")).thenReturn(true);

        TeamMember member = TeamMember.builder().role(TeamRole.ADMIN).build();
        when(teamMemberRepository.findByUserId(userId)).thenReturn(List.of(member));
        when(jwtTokenProvider.generateToken(eq(testUser), eq(List.of("ADMIN")))).thenReturn("token");
        when(jwtTokenProvider.generateRefreshToken(testUser)).thenReturn("refresh");

        authService.login(request);
        verify(jwtTokenProvider).generateToken(eq(testUser), eq(List.of("ADMIN")));
    }

    @Test
    void login_mfaEnabled_returnsChallengeToken() {
        testUser.setMfaEnabled(true);
        LoginRequest request = new LoginRequest("test@codeops.dev", "password");
        when(userRepository.findByEmail("test@codeops.dev")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password", "encoded-password")).thenReturn(true);
        when(jwtTokenProvider.generateMfaChallengeToken(testUser)).thenReturn("mfa-challenge-token");

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertTrue(response.mfaRequired());
        assertEquals("mfa-challenge-token", response.mfaChallengeToken());
        assertNull(response.token());
        assertNull(response.refreshToken());
        assertNull(response.user());
        verify(jwtTokenProvider).generateMfaChallengeToken(testUser);
        verify(jwtTokenProvider, never()).generateToken(any(User.class), anyList());
    }

    @Test
    void login_mfaDisabled_returnsFullTokens() {
        testUser.setMfaEnabled(false);
        LoginRequest request = new LoginRequest("test@codeops.dev", "password");
        when(userRepository.findByEmail("test@codeops.dev")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password", "encoded-password")).thenReturn(true);
        when(teamMemberRepository.findByUserId(userId)).thenReturn(List.of());
        when(jwtTokenProvider.generateToken(eq(testUser), anyList())).thenReturn("token");
        when(jwtTokenProvider.generateRefreshToken(testUser)).thenReturn("refresh");

        AuthResponse response = authService.login(request);

        assertNotNull(response.token());
        assertNotNull(response.refreshToken());
        assertNotNull(response.user());
        assertNull(response.mfaRequired());
        assertNull(response.mfaChallengeToken());
    }

    @Test
    void refreshToken_success() {
        RefreshTokenRequest request = new RefreshTokenRequest("valid-refresh");
        when(jwtTokenProvider.validateToken("valid-refresh")).thenReturn(true);
        when(jwtTokenProvider.isRefreshToken("valid-refresh")).thenReturn(true);
        when(jwtTokenProvider.getUserIdFromToken("valid-refresh")).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(teamMemberRepository.findByUserId(userId)).thenReturn(List.of());
        when(jwtTokenProvider.generateToken(eq(testUser), anyList())).thenReturn("new-token");
        when(jwtTokenProvider.generateRefreshToken(testUser)).thenReturn("new-refresh");

        AuthResponse response = authService.refreshToken(request);
        assertEquals("new-token", response.token());
    }

    @Test
    void refreshToken_invalidToken_throws() {
        RefreshTokenRequest request = new RefreshTokenRequest("invalid");
        when(jwtTokenProvider.validateToken("invalid")).thenReturn(false);
        assertThrows(IllegalArgumentException.class, () -> authService.refreshToken(request));
    }

    @Test
    void refreshToken_notRefreshType_throws() {
        RefreshTokenRequest request = new RefreshTokenRequest("access-token");
        when(jwtTokenProvider.validateToken("access-token")).thenReturn(true);
        when(jwtTokenProvider.isRefreshToken("access-token")).thenReturn(false);
        assertThrows(IllegalArgumentException.class, () -> authService.refreshToken(request));
    }

    @Test
    void refreshToken_deactivatedUser_throws() {
        testUser.setIsActive(false);
        RefreshTokenRequest request = new RefreshTokenRequest("valid-refresh");
        when(jwtTokenProvider.validateToken("valid-refresh")).thenReturn(true);
        when(jwtTokenProvider.isRefreshToken("valid-refresh")).thenReturn(true);
        when(jwtTokenProvider.getUserIdFromToken("valid-refresh")).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        assertThrows(IllegalArgumentException.class, () -> authService.refreshToken(request));
    }

    @Test
    void changePassword_success() {
        setSecurityContext(userId);
        ChangePasswordRequest request = new ChangePasswordRequest("oldP@ss1", "NewP@ss2!");
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("oldP@ss1", "encoded-password")).thenReturn(true);
        when(passwordEncoder.encode("NewP@ss2!")).thenReturn("new-encoded");

        authService.changePassword(request);

        verify(userRepository).save(testUser);
    }

    @Test
    void changePassword_wrongCurrentPassword_throws() {
        setSecurityContext(userId);
        ChangePasswordRequest request = new ChangePasswordRequest("wrong", "NewP@ss2!");
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrong", "encoded-password")).thenReturn(false);
        assertThrows(IllegalArgumentException.class, () -> authService.changePassword(request));
    }

    @Test
    void changePassword_userNotFound_throws() {
        setSecurityContext(userId);
        ChangePasswordRequest request = new ChangePasswordRequest("old", "NewP@ss2!");
        when(userRepository.findById(userId)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> authService.changePassword(request));
    }

    private void setSecurityContext(UUID userId) {
        var auth = new UsernamePasswordAuthenticationToken(userId, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
