package com.codeops.dto.response;

import com.codeops.entity.enums.InvitationStatus;
import com.codeops.entity.enums.TeamRole;

import java.time.Instant;
import java.util.UUID;

public record InvitationResponse(UUID id, String email, TeamRole role, InvitationStatus status,
                                 String invitedByName, Instant expiresAt, Instant createdAt) {}
