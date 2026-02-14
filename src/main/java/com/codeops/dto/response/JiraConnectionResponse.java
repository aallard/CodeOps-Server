package com.codeops.dto.response;

import java.time.Instant;
import java.util.UUID;

public record JiraConnectionResponse(UUID id, UUID teamId, String name, String instanceUrl, String email,
                                     boolean isActive, Instant createdAt) {}
