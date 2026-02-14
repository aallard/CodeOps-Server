package com.codeops.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateDependencyScanRequest(
        @NotNull UUID projectId,
        UUID jobId,
        @Size(max = 200) String manifestFile,
        Integer totalDependencies,
        Integer outdatedCount,
        Integer vulnerableCount,
        @Size(max = 50000) String scanDataJson
) {}
