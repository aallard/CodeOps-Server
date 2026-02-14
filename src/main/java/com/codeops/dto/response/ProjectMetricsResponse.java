package com.codeops.dto.response;

import java.time.Instant;
import java.util.UUID;

public record ProjectMetricsResponse(UUID projectId, String projectName, Integer currentHealthScore,
                                     Integer previousHealthScore, int totalJobs, int totalFindings,
                                     int openCritical, int openHigh, int techDebtItemCount,
                                     int openVulnerabilities, Instant lastAuditAt) {}
