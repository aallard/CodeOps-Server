package com.codeops.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AuthResponse(String token, String refreshToken, UserResponse user,
                           Boolean mfaRequired, String mfaChallengeToken, String maskedEmail) {

    /** Standard login response (no MFA). */
    public AuthResponse(String token, String refreshToken, UserResponse user) {
        this(token, refreshToken, user, null, null, null);
    }

    /** TOTP MFA challenge response — no tokens issued yet. */
    public static AuthResponse mfaChallenge(String challengeToken) {
        return new AuthResponse(null, null, null, true, challengeToken, null);
    }

    /** Email MFA challenge response — includes masked email hint. */
    public static AuthResponse emailMfaChallenge(String challengeToken, String maskedEmail) {
        return new AuthResponse(null, null, null, true, challengeToken, maskedEmail);
    }
}
