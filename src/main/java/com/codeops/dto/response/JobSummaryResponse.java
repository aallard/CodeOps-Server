package com.codeops.dto.response;

import com.codeops.entity.enums.JobMode;
import com.codeops.entity.enums.JobResult;
import com.codeops.entity.enums.JobStatus;

import java.time.Instant;
import java.util.UUID;

public record JobSummaryResponse(UUID id, String projectName, JobMode mode, JobStatus status, String name,
                                 JobResult overallResult, Integer healthScore, int totalFindings, int criticalCount,
                                 Instant completedAt, Instant createdAt) {}
