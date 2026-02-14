package com.codeops.dto.response;

import com.codeops.entity.enums.AgentType;
import com.codeops.entity.enums.Scope;

import java.time.Instant;
import java.util.UUID;

public record PersonaResponse(UUID id, String name, AgentType agentType, String description, String contentMd,
                               Scope scope, UUID teamId, UUID createdBy, String createdByName, boolean isDefault,
                               int version, Instant createdAt, Instant updatedAt) {}
