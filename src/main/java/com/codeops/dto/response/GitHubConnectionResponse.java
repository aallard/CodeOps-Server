package com.codeops.dto.response;

import com.codeops.entity.enums.GitHubAuthType;

import java.time.Instant;
import java.util.UUID;

public record GitHubConnectionResponse(UUID id, UUID teamId, String name, GitHubAuthType authType,
                                       String githubUsername, boolean isActive, Instant createdAt) {}
