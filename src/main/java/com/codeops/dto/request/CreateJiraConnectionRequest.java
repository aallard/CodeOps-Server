package com.codeops.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateJiraConnectionRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Size(max = 500) String instanceUrl,
        @NotBlank @Email String email,
        @NotBlank String apiToken
) {}
