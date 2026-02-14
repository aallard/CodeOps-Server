package com.codeops.dto.request;

import com.codeops.entity.enums.JobMode;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateJobRequest(
        @NotNull UUID projectId,
        @NotNull JobMode mode,
        String name,
        String branch,
        String configJson,
        String jiraTicketKey
) {}
