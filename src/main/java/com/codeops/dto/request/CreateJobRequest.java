package com.codeops.dto.request;

import com.codeops.entity.enums.JobMode;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateJobRequest(
        @NotNull UUID projectId,
        @NotNull JobMode mode,
        @Size(max = 200) String name,
        @Size(max = 200) String branch,
        @Size(max = 50000) String configJson,
        @Size(max = 200) String jiraTicketKey
) {}
