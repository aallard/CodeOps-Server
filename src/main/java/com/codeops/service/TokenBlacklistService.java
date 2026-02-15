package com.codeops.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Maintains an in-memory blacklist of revoked JWT token identifiers (JTI claims).
 *
 * <p>Blacklisted tokens are stored in a thread-safe {@link ConcurrentHashMap} key set.
 * This service is consulted during JWT authentication to reject tokens that have been
 * explicitly invalidated (e.g., on user logout).</p>
 *
 * <p><strong>Note:</strong> The blacklist is stored in memory only and is not persisted.
 * All blacklisted tokens are lost on application restart. The {@code expiry} parameter
 * is accepted but not currently used for automatic cleanup.</p>
 *
 * @see com.codeops.security.JwtAuthFilter
 * @see AuthController
 */
@Service
public class TokenBlacklistService {

    private static final Logger log = LoggerFactory.getLogger(TokenBlacklistService.class);

    private final ConcurrentHashMap.KeySetView<String, Boolean> blacklistedTokens = ConcurrentHashMap.newKeySet();

    /**
     * Adds a JWT token identifier to the blacklist.
     *
     * <p>If the provided JTI is {@code null}, the call is silently ignored.
     * The {@code expiry} parameter is accepted for future use but is not currently
     * used for automatic eviction.</p>
     *
     * @param jti the JWT ID (jti claim) to blacklist, or {@code null} to no-op
     * @param expiry the token's original expiration time (currently unused)
     */
    public void blacklist(String jti, Instant expiry) {
        if (jti != null) {
            blacklistedTokens.add(jti);
            log.info("Token blacklisted: jti={}, expiry={}, totalBlacklisted={}", jti, expiry, blacklistedTokens.size());
        } else {
            log.warn("blacklist called with null jti, ignoring");
        }
    }

    /**
     * Checks whether a JWT token identifier has been blacklisted.
     *
     * @param jti the JWT ID (jti claim) to check, or {@code null}
     * @return {@code true} if the JTI is non-null and has been blacklisted; {@code false} otherwise
     */
    public boolean isBlacklisted(String jti) {
        boolean result = jti != null && blacklistedTokens.contains(jti);
        if (result) {
            log.debug("Token is blacklisted: jti={}", jti);
        }
        return result;
    }
}
