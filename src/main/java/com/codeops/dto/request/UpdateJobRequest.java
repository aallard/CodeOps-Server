package com.codeops.dto.request;

import com.codeops.entity.enums.JobResult;
import com.codeops.entity.enums.JobStatus;

import java.time.Instant;

public record UpdateJobRequest(
        JobStatus status,
        String summaryMd,
        JobResult overallResult,
        Integer healthScore,
        Integer totalFindings,
        Integer criticalCount,
        Integer highCount,
        Integer mediumCount,
        Integer lowCount,
        Instant completedAt,
        Instant startedAt
) {}
