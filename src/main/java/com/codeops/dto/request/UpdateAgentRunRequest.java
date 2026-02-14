package com.codeops.dto.request;

import com.codeops.entity.enums.AgentResult;
import com.codeops.entity.enums.AgentStatus;

import java.time.Instant;

public record UpdateAgentRunRequest(
        AgentStatus status,
        AgentResult result,
        String reportS3Key,
        Integer score,
        Integer findingsCount,
        Integer criticalCount,
        Integer highCount,
        Instant completedAt
) {}
