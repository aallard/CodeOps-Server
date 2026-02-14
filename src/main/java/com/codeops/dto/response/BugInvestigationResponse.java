package com.codeops.dto.response;

import java.time.Instant;
import java.util.UUID;

public record BugInvestigationResponse(UUID id, UUID jobId, String jiraKey, String jiraSummary,
                                       String jiraDescription, String additionalContext, String rcaMd,
                                       String impactAssessmentMd, String rcaS3Key, boolean rcaPostedToJira,
                                       boolean fixTasksCreatedInJira, Instant createdAt) {}
