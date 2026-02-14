package com.codeops.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AssignDirectiveRequest(
        @NotNull UUID projectId,
        @NotNull UUID directiveId,
        boolean enabled
) {}
