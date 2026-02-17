package com.codeops.controller;

import com.codeops.dto.request.ChangePasswordRequest;
import com.codeops.dto.request.LoginRequest;
import com.codeops.dto.request.RefreshTokenRequest;
import com.codeops.dto.request.RegisterRequest;
import com.codeops.dto.response.AuthResponse;
import com.codeops.dto.response.UserResponse;
import com.codeops.security.JwtTokenProvider;
import com.codeops.service.AuditLogService;
import com.codeops.service.AuthService;
import com.codeops.service.MfaService;
import com.codeops.service.TokenBlacklistService;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @Mock
    private MfaService mfaService;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private TokenBlacklistService tokenBlacklistService;

    private AuthController controller;

    private final UUID userId = UUID.randomUUID();
    private final Instant now = Instant.now();

    @BeforeEach
    void setUp() {
        controller = new AuthController(authService, mfaService, auditLogService, jwtTokenProvider, tokenBlacklistService);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void setSecurityContext(UUID userId) {
        var auth = new UsernamePasswordAuthenticationToken(userId, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private UserResponse userResponse() {
        return new UserResponse(userId, "test@example.com", "Test User", null, true, now, now, false, "NONE");
    }

    @Test
    void register_returns201WithAuthResponse() {
        RegisterRequest request = new RegisterRequest("test@example.com", "Password1!", "Test User");
        AuthResponse authResponse = new AuthResponse("access-token", "refresh-token", userResponse());
        when(authService.register(request)).thenReturn(authResponse);

        ResponseEntity<AuthResponse> response = controller.register(request);

        assertEquals(201, response.getStatusCode().value());
        assertEquals(authResponse, response.getBody());
        verify(authService).register(request);
        verify(auditLogService).log(userId, null, "USER_REGISTERED", "USER", userId, null);
    }

    @Test
    void login_returns200WithAuthResponse() {
        LoginRequest request = new LoginRequest("test@example.com", "Password1!");
        AuthResponse authResponse = new AuthResponse("access-token", "refresh-token", userResponse());
        when(authService.login(request)).thenReturn(authResponse);

        ResponseEntity<AuthResponse> response = controller.login(request);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(authResponse, response.getBody());
        verify(authService).login(request);
        verify(auditLogService).log(userId, null, "USER_LOGIN", "USER", userId, null);
    }

    @Test
    void refresh_returns200WithAuthResponse() {
        RefreshTokenRequest request = new RefreshTokenRequest("refresh-token");
        AuthResponse authResponse = new AuthResponse("new-access-token", "new-refresh-token", userResponse());
        when(authService.refreshToken(request)).thenReturn(authResponse);

        ResponseEntity<AuthResponse> response = controller.refresh(request);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(authResponse, response.getBody());
        verify(authService).refreshToken(request);
    }

    @Test
    void logout_returns204AndBlacklistsToken() {
        String token = "jwt-token-value";
        String jti = "unique-jti";
        Instant expiry = Instant.now().plusSeconds(3600).truncatedTo(java.time.temporal.ChronoUnit.MILLIS);

        Claims claims = mock(Claims.class);
        when(claims.get("jti", String.class)).thenReturn(jti);
        when(claims.getExpiration()).thenReturn(Date.from(expiry));
        when(jwtTokenProvider.parseClaims(token)).thenReturn(claims);

        ResponseEntity<Void> response = controller.logout("Bearer " + token);

        assertEquals(204, response.getStatusCode().value());
        assertNull(response.getBody());
        verify(jwtTokenProvider).parseClaims(token);
        verify(tokenBlacklistService).blacklist(jti, expiry);
    }

    @Test
    void changePassword_returns200() {
        setSecurityContext(userId);
        ChangePasswordRequest request = new ChangePasswordRequest("OldPass1!", "NewPass1!");

        ResponseEntity<Void> response = controller.changePassword(request);

        assertEquals(200, response.getStatusCode().value());
        verify(authService).changePassword(request);
    }
}
