package com.codeops.dto.response;

import com.codeops.entity.enums.AgentResult;
import com.codeops.entity.enums.AgentStatus;
import com.codeops.entity.enums.AgentType;

import java.time.Instant;
import java.util.UUID;

public record AgentRunResponse(UUID id, UUID jobId, AgentType agentType, AgentStatus status, AgentResult result,
                               String reportS3Key, Integer score, int findingsCount, int criticalCount, int highCount,
                               Instant startedAt, Instant completedAt) {}
