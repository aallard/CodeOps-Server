package com.codeops.dto.response;

import com.codeops.entity.enums.AgentType;
import com.codeops.entity.enums.ComplianceStatus;

import java.time.Instant;
import java.util.UUID;

public record ComplianceItemResponse(UUID id, UUID jobId, String requirement, UUID specId, String specName,
                                     ComplianceStatus status, String evidence, AgentType agentType, String notes,
                                     Instant createdAt) {}
