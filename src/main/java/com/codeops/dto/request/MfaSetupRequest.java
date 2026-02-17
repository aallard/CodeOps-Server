package com.codeops.dto.request;

import jakarta.validation.constraints.NotBlank;

public record MfaSetupRequest(
        @NotBlank String password
) {}
