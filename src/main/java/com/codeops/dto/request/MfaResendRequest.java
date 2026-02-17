package com.codeops.dto.request;

import jakarta.validation.constraints.NotBlank;

public record MfaResendRequest(
        @NotBlank String challengeToken
) {}
