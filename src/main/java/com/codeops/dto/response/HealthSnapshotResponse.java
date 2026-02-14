package com.codeops.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record HealthSnapshotResponse(UUID id, UUID projectId, UUID jobId, int healthScore,
                                     String findingsBySeverity, Integer techDebtScore, Integer dependencyScore,
                                     BigDecimal testCoveragePercent, Instant capturedAt) {}
