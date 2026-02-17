package com.codeops.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AuthResponse(String token, String refreshToken, UserResponse user,
                           Boolean mfaRequired, String mfaChallengeToken) {

    /** Standard login response (no MFA). */
    public AuthResponse(String token, String refreshToken, UserResponse user) {
        this(token, refreshToken, user, null, null);
    }

    /** MFA challenge response — no tokens issued yet. */
    public static AuthResponse mfaChallenge(String challengeToken) {
        return new AuthResponse(null, null, null, true, challengeToken);
    }
}
