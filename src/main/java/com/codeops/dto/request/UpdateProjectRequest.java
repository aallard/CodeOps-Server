package com.codeops.dto.request;

import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record UpdateProjectRequest(
        @Size(max = 200) String name,
        String description,
        UUID githubConnectionId,
        String repoUrl,
        String repoFullName,
        String defaultBranch,
        UUID jiraConnectionId,
        String jiraProjectKey,
        String jiraDefaultIssueType,
        List<String> jiraLabels,
        String jiraComponent,
        String techStack,
        Boolean isArchived
) {}
