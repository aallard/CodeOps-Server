package com.codeops.dto.request;

import jakarta.validation.constraints.NotBlank;

public record MfaVerifyRequest(
        @NotBlank String code
) {}
