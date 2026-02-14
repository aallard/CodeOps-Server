package com.codeops.security;

import com.codeops.config.JwtProperties;
import com.codeops.entity.User;
import com.codeops.service.TokenBlacklistService;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtTokenProviderTest {

    @Mock
    private TokenBlacklistService tokenBlacklistService;

    private JwtTokenProvider jwtTokenProvider;
    private JwtProperties jwtProperties;

    private static final String SECRET = "test-secret-key-minimum-32-characters-long-for-hs256-testing";

    @BeforeEach
    void setUp() {
        jwtProperties = new JwtProperties();
        jwtProperties.setSecret(SECRET);
        jwtProperties.setExpirationHours(24);
        jwtProperties.setRefreshExpirationDays(30);
        jwtTokenProvider = new JwtTokenProvider(jwtProperties, tokenBlacklistService);
    }

    @Test
    void validateSecret_validSecret_noException() {
        assertDoesNotThrow(() -> jwtTokenProvider.validateSecret());
    }

    @Test
    void validateSecret_nullSecret_throws() {
        jwtProperties.setSecret(null);
        assertThrows(IllegalStateException.class, () -> jwtTokenProvider.validateSecret());
    }

    @Test
    void validateSecret_shortSecret_throws() {
        jwtProperties.setSecret("short");
        assertThrows(IllegalStateException.class, () -> jwtTokenProvider.validateSecret());
    }

    @Test
    void validateSecret_blankSecret_throws() {
        jwtProperties.setSecret("   ");
        assertThrows(IllegalStateException.class, () -> jwtTokenProvider.validateSecret());
    }

    @Test
    void generateToken_containsExpectedClaims() {
        User user = createUser();
        String token = jwtTokenProvider.generateToken(user, List.of("ADMIN", "MEMBER"));

        assertNotNull(token);
        UUID userId = jwtTokenProvider.getUserIdFromToken(token);
        assertEquals(user.getId(), userId);

        String email = jwtTokenProvider.getEmailFromToken(token);
        assertEquals("test@codeops.dev", email);

        List<String> roles = jwtTokenProvider.getRolesFromToken(token);
        assertEquals(List.of("ADMIN", "MEMBER"), roles);
    }

    @Test
    void generateRefreshToken_isRefreshToken() {
        User user = createUser();
        String refreshToken = jwtTokenProvider.generateRefreshToken(user);

        assertNotNull(refreshToken);
        assertTrue(jwtTokenProvider.isRefreshToken(refreshToken));
    }

    @Test
    void generateToken_isNotRefreshToken() {
        User user = createUser();
        String token = jwtTokenProvider.generateToken(user, List.of());
        assertFalse(jwtTokenProvider.isRefreshToken(token));
    }

    @Test
    void validateToken_validToken_returnsTrue() {
        when(tokenBlacklistService.isBlacklisted(anyString())).thenReturn(false);

        User user = createUser();
        String token = jwtTokenProvider.generateToken(user, List.of());
        assertTrue(jwtTokenProvider.validateToken(token));
    }

    @Test
    void validateToken_blacklistedToken_returnsFalse() {
        when(tokenBlacklistService.isBlacklisted(anyString())).thenReturn(true);

        User user = createUser();
        String token = jwtTokenProvider.generateToken(user, List.of());
        assertFalse(jwtTokenProvider.validateToken(token));
    }

    @Test
    void validateToken_invalidToken_returnsFalse() {
        assertFalse(jwtTokenProvider.validateToken("invalid.token.here"));
    }

    @Test
    void validateToken_emptyToken_returnsFalse() {
        assertFalse(jwtTokenProvider.validateToken(""));
    }

    @Test
    void parseClaims_returnsValidClaims() {
        User user = createUser();
        String token = jwtTokenProvider.generateToken(user, List.of("OWNER"));

        Claims claims = jwtTokenProvider.parseClaims(token);
        assertEquals(user.getId().toString(), claims.getSubject());
        assertEquals("test@codeops.dev", claims.get("email", String.class));
        assertNotNull(claims.get("jti", String.class));
        assertNotNull(claims.getExpiration());
        assertNotNull(claims.getIssuedAt());
    }

    @Test
    void isRefreshToken_invalidToken_returnsFalse() {
        assertFalse(jwtTokenProvider.isRefreshToken("garbage"));
    }

    private User createUser() {
        User user = User.builder()
                .email("test@codeops.dev")
                .passwordHash("hashed")
                .displayName("Test User")
                .build();
        user.setId(UUID.randomUUID());
        return user;
    }
}
