package com.codeops.dto.response;

import com.codeops.entity.enums.AgentType;
import com.codeops.entity.enums.DebtCategory;
import com.codeops.entity.enums.Effort;
import com.codeops.entity.enums.FindingStatus;
import com.codeops.entity.enums.Severity;

import java.time.Instant;
import java.util.UUID;

public record FindingResponse(UUID id, UUID jobId, AgentType agentType, Severity severity, String title,
                              String description, String filePath, Integer lineNumber, String recommendation,
                              String evidence, Effort effortEstimate, DebtCategory debtCategory,
                              FindingStatus status, UUID statusChangedBy, Instant statusChangedAt,
                              Instant createdAt) {}
