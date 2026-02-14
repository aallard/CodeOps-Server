package com.codeops.dto.request;

import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
        @Size(max = 100) String displayName,
        @Size(max = 500) String avatarUrl
) {}
