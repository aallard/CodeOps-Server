package com.codeops.entity.enums;

/**
 * Enumerates the supported multi-factor authentication methods for user accounts.
 *
 * <ul>
 *   <li>{@link #NONE} — MFA is not enabled</li>
 *   <li>{@link #TOTP} — Time-based One-Time Password via authenticator app</li>
 *   <li>{@link #EMAIL} — Verification code sent to the user's registered email</li>
 * </ul>
 */
public enum MfaMethod {
    NONE,
    TOTP,
    EMAIL
}
