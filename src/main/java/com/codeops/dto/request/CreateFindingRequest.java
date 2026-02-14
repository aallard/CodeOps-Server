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
        @Size(max = 5000) String description,
        @Size(max = 1000) String filePath,
        Integer lineNumber,
        @Size(max = 5000) String recommendation,
        @Size(max = 50000) String evidence,
        Effort effortEstimate,
        DebtCategory debtCategory
) {}
