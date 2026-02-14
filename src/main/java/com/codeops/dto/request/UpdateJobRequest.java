package com.codeops.dto.request;

import com.codeops.entity.enums.JobResult;
import com.codeops.entity.enums.JobStatus;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record UpdateJobRequest(
        JobStatus status,
        @Size(max = 50000) String summaryMd,
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
