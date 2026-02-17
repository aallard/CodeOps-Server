package com.codeops.dto.request;

import jakarta.validation.constraints.NotBlank;

public record MfaLoginRequest(
        @NotBlank String challengeToken,
        @NotBlank String code
) {}
