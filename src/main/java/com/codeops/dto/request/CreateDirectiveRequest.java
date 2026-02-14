package com.codeops.dto.request;

import com.codeops.entity.enums.DirectiveCategory;
import com.codeops.entity.enums.DirectiveScope;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateDirectiveRequest(
        @NotBlank @Size(max = 200) String name,
        @Size(max = 5000) String description,
        @NotBlank @Size(max = 50000) String contentMd,
        DirectiveCategory category,
        @NotNull DirectiveScope scope,
        UUID teamId,
        UUID projectId
) {}
