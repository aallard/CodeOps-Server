package com.codeops.dto.request;

import com.codeops.entity.enums.DebtStatus;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record UpdateTechDebtStatusRequest(
        @NotNull DebtStatus status,
        UUID resolvedJobId
) {}
