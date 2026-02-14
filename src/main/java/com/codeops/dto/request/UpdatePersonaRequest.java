package com.codeops.dto.request;

import jakarta.validation.constraints.Size;

public record UpdatePersonaRequest(
        @Size(max = 100) String name,
        String description,
        String contentMd,
        Boolean isDefault
) {}
