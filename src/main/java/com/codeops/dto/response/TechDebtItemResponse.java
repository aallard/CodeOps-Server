package com.codeops.dto.response;

import com.codeops.entity.enums.BusinessImpact;
import com.codeops.entity.enums.DebtCategory;
import com.codeops.entity.enums.DebtStatus;
import com.codeops.entity.enums.Effort;

import java.time.Instant;
import java.util.UUID;

public record TechDebtItemResponse(UUID id, UUID projectId, DebtCategory category, String title, String description,
                                   String filePath, Effort effortEstimate, BusinessImpact businessImpact,
                                   DebtStatus status, UUID firstDetectedJobId, UUID resolvedJobId,
                                   Instant createdAt, Instant updatedAt) {}
