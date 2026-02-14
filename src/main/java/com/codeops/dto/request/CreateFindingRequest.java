package com.codeops.dto.request;

import com.codeops.entity.enums.AgentType;
import com.codeops.entity.enums.DebtCategory;
import com.codeops.entity.enums.Effort;
import com.codeops.entity.enums.Severity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateFindingRequest(
        @NotNull UUID jobId,
        @NotNull AgentType agentType,
        @NotNull Severity severity,
        @NotBlank @Size(max = 500) String title,
        String description,
        String filePath,
        Integer lineNumber,
        String recommendation,
        String evidence,
        Effort effortEstimate,
        DebtCategory debtCategory
) {}
