package com.codeops.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateBugInvestigationRequest(
        @NotNull UUID jobId,
        @Size(max = 200) String jiraKey,
        @Size(max = 500) String jiraSummary,
        @Size(max = 50000) String jiraDescription,
        @Size(max = 50000) String jiraCommentsJson,
        @Size(max = 50000) String jiraAttachmentsJson,
        @Size(max = 50000) String jiraLinkedIssues,
        @Size(max = 50000) String additionalContext
) {}
