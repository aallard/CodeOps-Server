package com.codeops.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateHealthSnapshotRequest(
        @NotNull UUID projectId,
        UUID jobId,
        @NotNull Integer healthScore,
        @Size(max = 5000) String findingsBySeverity,
        Integer techDebtScore,
        Integer dependencyScore,
        BigDecimal testCoveragePercent
) {}
