package com.codeops.dto.request;

import com.codeops.entity.enums.AgentType;
import com.codeops.entity.enums.Scope;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreatePersonaRequest(
        @NotBlank @Size(max = 100) String name,
        AgentType agentType,
        String description,
        @NotBlank String contentMd,
        @NotNull Scope scope,
        UUID teamId,
        Boolean isDefault
) {}
