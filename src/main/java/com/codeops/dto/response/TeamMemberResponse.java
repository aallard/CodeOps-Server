package com.codeops.dto.response;

import com.codeops.entity.enums.TeamRole;

import java.time.Instant;
import java.util.UUID;

public record TeamMemberResponse(UUID id, UUID userId, String displayName, String email, String avatarUrl,
                                 TeamRole role, Instant joinedAt) {}
