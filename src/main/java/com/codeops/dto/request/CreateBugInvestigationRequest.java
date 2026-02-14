package com.codeops.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateBugInvestigationRequest(
        @NotNull UUID jobId,
        String jiraKey,
        String jiraSummary,
        String jiraDescription,
        String jiraCommentsJson,
        String jiraAttachmentsJson,
        String jiraLinkedIssues,
        String additionalContext
) {}
