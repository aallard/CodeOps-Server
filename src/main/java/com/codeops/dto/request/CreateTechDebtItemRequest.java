package com.codeops.dto.request;

import com.codeops.entity.enums.BusinessImpact;
import com.codeops.entity.enums.DebtCategory;
import com.codeops.entity.enums.Effort;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateTechDebtItemRequest(
        @NotNull UUID projectId,
        @NotNull DebtCategory category,
        @NotBlank @Size(max = 500) String title,
        @Size(max = 5000) String description,
        @Size(max = 1000) String filePath,
        Effort effortEstimate,
        BusinessImpact businessImpact,
        UUID firstDetectedJobId
) {}
