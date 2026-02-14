package com.codeops.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for JWT token generation and validation, bound to the
 * {@code codeops.jwt} prefix in application properties.
 *
 * <p>Properties:</p>
 * <ul>
 *   <li>{@code codeops.jwt.secret} — the HMAC signing secret (minimum 32 characters)</li>
 *   <li>{@code codeops.jwt.expiration-hours} — access token lifetime in hours (default: 24)</li>
 *   <li>{@code codeops.jwt.refresh-expiration-days} — refresh token lifetime in days (default: 30)</li>
 * </ul>
 *
 * @see com.codeops.security.JwtTokenProvider
 * @see com.codeops.CodeOpsApplication
 */
@ConfigurationProperties(prefix = "codeops.jwt")
@Getter
@Setter
public class JwtProperties {
    private String secret;
    private int expirationHours = 24;
    private int refreshExpirationDays = 30;
}
