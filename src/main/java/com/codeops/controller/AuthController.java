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
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication")
public class AuthController {

    private final AuthService authService;
    private final AuditLogService auditLogService;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenBlacklistService tokenBlacklistService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        auditLogService.log(response.user().id(), null, "USER_REGISTERED", "USER", response.user().id(), null);
        return ResponseEntity.status(201).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        auditLogService.log(response.user().id(), null, "USER_LOGIN", "USER", response.user().id(), null);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> logout(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        Claims claims = jwtTokenProvider.parseClaims(token);
        tokenBlacklistService.blacklist(
                claims.get("jti", String.class),
                claims.getExpiration().toInstant()
        );
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(request);
        return ResponseEntity.ok().build();
    }
}
