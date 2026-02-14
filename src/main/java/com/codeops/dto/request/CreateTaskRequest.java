package com.codeops.dto.request;

import com.codeops.entity.enums.Priority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record CreateTaskRequest(
        @NotNull UUID jobId,
        @NotNull Integer taskNumber,
        @NotBlank @Size(max = 500) String title,
        @Size(max = 5000) String description,
        @Size(max = 50000) String promptMd,
        @Size(max = 1000) String promptS3Key,
        List<UUID> findingIds,
        Priority priority
) {}
