package com.codeops.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ProjectResponse(UUID id, UUID teamId, String name, String description, UUID githubConnectionId,
                               String repoUrl, String repoFullName, String defaultBranch, UUID jiraConnectionId,
                               String jiraProjectKey, String jiraDefaultIssueType, List<String> jiraLabels,
                               String jiraComponent, String techStack, Integer healthScore, Instant lastAuditAt,
                               boolean isArchived, Instant createdAt, Instant updatedAt) {}
