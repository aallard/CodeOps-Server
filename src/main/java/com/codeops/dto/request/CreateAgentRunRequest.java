package com.codeops.dto.request;

import com.codeops.entity.enums.AgentType;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateAgentRunRequest(
        @NotNull UUID jobId,
        @NotNull AgentType agentType
) {}
