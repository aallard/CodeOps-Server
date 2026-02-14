package com.codeops.dto.response;

public record NotificationPreferenceResponse(
        String eventType,
        boolean inApp,
        boolean email
) {}
