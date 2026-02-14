package com.codeops.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateDependencyScanRequest(
        @NotNull UUID projectId,
        UUID jobId,
        String manifestFile,
        Integer totalDependencies,
        Integer outdatedCount,
        Integer vulnerableCount,
        String scanDataJson
) {}
