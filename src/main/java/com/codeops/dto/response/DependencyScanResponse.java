package com.codeops.dto.response;

import java.time.Instant;
import java.util.UUID;

public record DependencyScanResponse(UUID id, UUID projectId, UUID jobId, String manifestFile,
                                     int totalDependencies, int outdatedCount, int vulnerableCount,
                                     Instant createdAt) {}
