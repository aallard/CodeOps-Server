package com.codeops.dto.request;

import com.codeops.entity.enums.GitHubAuthType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateGitHubConnectionRequest(
        @NotBlank @Size(max = 100) String name,
        @NotNull GitHubAuthType authType,
        @NotBlank @Size(max = 10000) String credentials,
        @Size(max = 200) String githubUsername
) {}
