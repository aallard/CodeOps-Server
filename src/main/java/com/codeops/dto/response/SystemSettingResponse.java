package com.codeops.dto.response;

import java.time.Instant;
import java.util.UUID;

public record SystemSettingResponse(String key, String value, UUID updatedBy, Instant updatedAt) {}
