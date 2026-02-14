package com.codeops.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateSystemSettingRequest(
        @NotBlank @Size(max = 200) String key,
        @NotBlank @Size(max = 5000) String value
) {}
