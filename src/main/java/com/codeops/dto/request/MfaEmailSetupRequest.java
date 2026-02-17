package com.codeops.dto.request;

import jakarta.validation.constraints.NotBlank;

public record MfaEmailSetupRequest(
        @NotBlank String password
) {}
