package com.codeops.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateNotificationPreferenceRequest(
        @NotBlank @Size(max = 200) String eventType,
        boolean inApp,
        boolean email
) {}
