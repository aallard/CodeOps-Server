package com.codeops.dto.request;

import com.codeops.entity.enums.AgentType;
import com.codeops.entity.enums.ComplianceStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateComplianceItemRequest(
        @NotNull UUID jobId,
        @NotBlank String requirement,
        UUID specId,
        @NotNull ComplianceStatus status,
        String evidence,
        AgentType agentType,
        String notes
) {}
