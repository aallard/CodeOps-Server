package com.codeops.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record CreateProjectRequest(
        @NotBlank @Size(max = 200) String name,
        @Size(max = 5000) String description,
        UUID githubConnectionId,
        @Size(max = 2000) String repoUrl,
        @Size(max = 200) String repoFullName,
        @Size(max = 200) String defaultBranch,
        UUID jiraConnectionId,
        @Size(max = 50) String jiraProjectKey,
        @Size(max = 200) String jiraDefaultIssueType,
        @Size(max = 100) List<@Size(max = 100) String> jiraLabels,
        @Size(max = 200) String jiraComponent,
        @Size(max = 5000) String techStack
) {}
