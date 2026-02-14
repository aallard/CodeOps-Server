package com.codeops.dto.request;

import com.codeops.entity.enums.SpecType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateSpecificationRequest(
        @NotNull UUID jobId,
        @NotBlank @Size(max = 200) String name,
        SpecType specType,
        @NotBlank @Size(max = 1000) String s3Key
) {}
