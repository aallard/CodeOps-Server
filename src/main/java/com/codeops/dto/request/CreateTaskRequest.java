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
        String description,
        String promptMd,
        String promptS3Key,
        List<UUID> findingIds,
        Priority priority
) {}
