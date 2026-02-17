package com.codeops.security;

import com.codeops.config.JwtProperties;
import com.codeops.entity.User;
import com.codeops.service.TokenBlacklistService;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Provides JWT token generation, parsing, and validation for the CodeOps authentication system.
 *
 * <p>Generates HS256-signed access tokens (with user ID, email, and roles as claims) and
 * refresh tokens (with a {@code "type":"refresh"} claim). Token expiration is configured
 * via {@link JwtProperties}.</p>
 *
 * <p>Validates tokens by verifying the HMAC signature, checking expiration, and consulting
 * the {@link TokenBlacklistService} to reject revoked tokens. All validation failures are
 * logged at WARN level without exposing details to callers.</p>
 *
 * @see JwtProperties
 * @see JwtAuthFilter
 * @see com.codeops.service.TokenBlacklistService
 */
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

    private final JwtProperties jwtProperties;
    private final TokenBlacklistService tokenBlacklistService;

    /**
     * Validates that the JWT secret is configured and meets the minimum length requirement
     * of 32 characters. Invoked automatically after dependency injection.
     *
     * @throws IllegalStateException if the secret is null, blank, or shorter than 32 characters
     */
    @PostConstruct
    public void validateSecret() {
        String secret = jwtProperties.getSecret();
        if (secret == null || secret.isBlank() || secret.length() < 32) {
            throw new IllegalStateException("JWT secret must be at least 32 characters. Set the JWT_SECRET environment variable.");
        }
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes());
    }

    /**
     * Generates a signed JWT access token containing the user's ID as the subject, along with
     * email, roles, and a unique JTI claim for revocation support.
     *
     * <p>The token expiration is determined by {@link JwtProperties#getExpirationHours()}.</p>
     *
     * @param user  the user for whom to generate the token
     * @param roles the list of role names to embed in the token claims
     * @return the compact, signed JWT string
     */
    public String generateToken(User user, List<String> roles) {
        Instant now = Instant.now();
        Instant expiry = now.plus(jwtProperties.getExpirationHours(), ChronoUnit.HOURS);

        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("roles", roles)
                .claim("jti", UUID.randomUUID().toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(getSigningKey(), Jwts.SIG.HS256)
                .compact();
    }

    /**
     * Generates a short-lived MFA challenge token (5 minutes) used to bridge the gap between
     * password verification and TOTP code submission during two-factor login.
     *
     * <p>Contains a {@code "type":"mfa_challenge"} claim to distinguish it from access and refresh
     * tokens. This token must NOT be accepted for normal API access — only for the MFA verify endpoint.</p>
     *
     * @param user the user who has passed password verification but still needs MFA
     * @return the compact, signed MFA challenge JWT string
     */
    public String generateMfaChallengeToken(User user) {
        Instant now = Instant.now();
        Instant expiry = now.plus(5, ChronoUnit.MINUTES);

        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("type", "mfa_challenge")
                .claim("email", user.getEmail())
                .claim("jti", UUID.randomUUID().toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(getSigningKey(), Jwts.SIG.HS256)
                .compact();
    }

    /**
     * Determines whether the given JWT token is an MFA challenge token by checking for the
     * {@code "type":"mfa_challenge"} claim.
     *
     * @param token the raw JWT string (without {@code "Bearer "} prefix)
     * @return {@code true} if the token contains a {@code "type"} claim equal to {@code "mfa_challenge"},
     *         {@code false} otherwise or if the token cannot be parsed
     */
    public boolean isMfaChallengeToken(String token) {
        try {
            Claims claims = parseClaims(token);
            return "mfa_challenge".equals(claims.get("type", String.class));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Generates a signed JWT refresh token containing the user's ID as the subject, a
     * {@code "type":"refresh"} claim to distinguish it from access tokens, and a unique JTI.
     *
     * <p>The token expiration is determined by {@link JwtProperties#getRefreshExpirationDays()}.</p>
     *
     * @param user the user for whom to generate the refresh token
     * @return the compact, signed JWT refresh token string
     */
    public String generateRefreshToken(User user) {
        Instant now = Instant.now();
        Instant expiry = now.plus(jwtProperties.getRefreshExpirationDays(), ChronoUnit.DAYS);

        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("type", "refresh")
                .claim("jti", UUID.randomUUID().toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(getSigningKey(), Jwts.SIG.HS256)
                .compact();
    }

    /**
     * Extracts the user ID from the JWT token's subject claim.
     *
     * @param token the raw JWT string (without {@code "Bearer "} prefix)
     * @return the user's UUID parsed from the token subject
     * @throws io.jsonwebtoken.JwtException if the token cannot be parsed or the signature is invalid
     */
    public UUID getUserIdFromToken(String token) {
        Claims claims = parseClaims(token);
        return UUID.fromString(claims.getSubject());
    }

    /**
     * Extracts the email address from the JWT token's {@code "email"} claim.
     *
     * @param token the raw JWT string (without {@code "Bearer "} prefix)
     * @return the email address stored in the token, or {@code null} if the claim is absent
     * @throws io.jsonwebtoken.JwtException if the token cannot be parsed or the signature is invalid
     */
    public String getEmailFromToken(String token) {
        Claims claims = parseClaims(token);
        return claims.get("email", String.class);
    }

    /**
     * Extracts the list of role names from the JWT token's {@code "roles"} claim.
     *
     * @param token the raw JWT string (without {@code "Bearer "} prefix)
     * @return the list of role name strings from the token
     * @throws io.jsonwebtoken.JwtException if the token cannot be parsed or the signature is invalid
     */
    @SuppressWarnings("unchecked")
    public List<String> getRolesFromToken(String token) {
        Claims claims = parseClaims(token);
        return claims.get("roles", List.class);
    }

    /**
     * Validates a JWT token by verifying its signature, expiration, format, and blacklist status.
     *
     * <p>Logs all validation failures at WARN level without exposing details to callers.
     * Checks the token's JTI against the {@link TokenBlacklistService} to reject revoked tokens.</p>
     *
     * @param token the raw JWT string (without {@code "Bearer "} prefix)
     * @return {@code true} if the token is valid and not blacklisted, {@code false} otherwise
     */
    public boolean validateToken(String token) {
        try {
            Claims claims = parseClaims(token);
            String jti = claims.get("jti", String.class);
            if (jti != null && tokenBlacklistService.isBlacklisted(jti)) {
                log.warn("Blacklisted JWT token used, jti: {}", jti);
                return false;
            }
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("Expired JWT token: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.warn("Unsupported JWT token: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.warn("Malformed JWT token: {}", e.getMessage());
        } catch (SignatureException e) {
            log.warn("Invalid JWT signature: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("JWT claims string is empty: {}", e.getMessage());
        }
        return false;
    }

    /**
     * Determines whether the given JWT token is a refresh token by checking for the
     * {@code "type":"refresh"} claim.
     *
     * @param token the raw JWT string (without {@code "Bearer "} prefix)
     * @return {@code true} if the token contains a {@code "type"} claim equal to {@code "refresh"},
     *         {@code false} otherwise or if the token cannot be parsed
     */
    public boolean isRefreshToken(String token) {
        try {
            Claims claims = parseClaims(token);
            return "refresh".equals(claims.get("type", String.class));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Parses and verifies a JWT token, returning the claims payload.
     *
     * <p>The token signature is verified using the configured HMAC secret key.</p>
     *
     * @param token the raw JWT string (without {@code "Bearer "} prefix)
     * @return the parsed {@link Claims} from the token payload
     * @throws io.jsonwebtoken.JwtException if the token is expired, malformed, or has an invalid signature
     */
    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
