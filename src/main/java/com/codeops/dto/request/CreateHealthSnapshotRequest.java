package com.codeops.dto.request;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateHealthSnapshotRequest(
        @NotNull UUID projectId,
        UUID jobId,
        @NotNull Integer healthScore,
        String findingsBySeverity,
        Integer techDebtScore,
        Integer dependencyScore,
        BigDecimal testCoveragePercent
) {}
