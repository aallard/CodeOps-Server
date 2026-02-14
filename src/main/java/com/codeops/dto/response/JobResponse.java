package com.codeops.dto.response;

import com.codeops.entity.enums.JobMode;
import com.codeops.entity.enums.JobResult;
import com.codeops.entity.enums.JobStatus;

import java.time.Instant;
import java.util.UUID;

public record JobResponse(UUID id, UUID projectId, String projectName, JobMode mode, JobStatus status, String name,
                          String branch, String configJson, String summaryMd, JobResult overallResult,
                          Integer healthScore, int totalFindings, int criticalCount, int highCount, int mediumCount,
                          int lowCount, String jiraTicketKey, UUID startedBy, String startedByName,
                          Instant startedAt, Instant completedAt, Instant createdAt) {}
