package com.codeops.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UpdateSystemSettingRequest(
        @NotBlank String key,
        @NotBlank String value
) {}
