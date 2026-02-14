package com.codeops.dto.request;

import jakarta.validation.constraints.Size;

public record UpdatePersonaRequest(
        @Size(max = 100) String name,
        @Size(max = 5000) String description,
        @Size(max = 50000) String contentMd,
        Boolean isDefault
) {}
