package com.codeops.dto.response;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(UUID id, String email, String displayName, String avatarUrl, boolean isActive,
                           Instant lastLoginAt, Instant createdAt) {}
