package com.codeops.dto.request;

import com.codeops.entity.enums.AgentType;
import com.codeops.entity.enums.ComplianceStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateComplianceItemRequest(
        @NotNull UUID jobId,
        @NotBlank @Size(max = 5000) String requirement,
        UUID specId,
        @NotNull ComplianceStatus status,
        @Size(max = 50000) String evidence,
        AgentType agentType,
        @Size(max = 5000) String notes
) {}
