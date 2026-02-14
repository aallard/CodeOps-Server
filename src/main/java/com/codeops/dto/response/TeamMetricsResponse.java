package com.codeops.dto.response;

import java.util.UUID;

public record TeamMetricsResponse(UUID teamId, int totalProjects, int totalJobs, int totalFindings,
                                  double averageHealthScore, int projectsBelowThreshold,
                                  int openCriticalFindings) {}
