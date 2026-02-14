package com.codeops.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UpdateNotificationPreferenceRequest(
        @NotBlank String eventType,
        boolean inApp,
        boolean email
) {}
