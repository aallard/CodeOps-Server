package com.codeops.dto.request;

import com.codeops.entity.enums.AgentResult;
import com.codeops.entity.enums.AgentStatus;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record UpdateAgentRunRequest(
        AgentStatus status,
        AgentResult result,
        @Size(max = 1000) String reportS3Key,
        Integer score,
        Integer findingsCount,
        Integer criticalCount,
        Integer highCount,
        Instant completedAt,
        Instant startedAt
) {}
