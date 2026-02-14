package com.codeops.dto.request;

import com.codeops.entity.enums.FindingStatus;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record BulkUpdateFindingsRequest(
        @NotEmpty List<UUID> findingIds,
        @NotNull FindingStatus status
) {}
