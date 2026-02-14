package com.codeops.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTeamRequest(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 5000) String description,
        @Size(max = 500) String teamsWebhookUrl
) {}
