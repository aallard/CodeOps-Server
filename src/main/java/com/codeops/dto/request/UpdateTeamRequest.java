package com.codeops.dto.request;

import jakarta.validation.constraints.Size;

public record UpdateTeamRequest(
        @Size(max = 100) String name,
        String description,
        @Size(max = 500) String teamsWebhookUrl
) {}
