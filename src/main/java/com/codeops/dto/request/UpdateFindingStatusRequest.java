package com.codeops.dto.request;

import com.codeops.entity.enums.FindingStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateFindingStatusRequest(
        @NotNull FindingStatus status
) {}
