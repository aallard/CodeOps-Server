package com.codeops.dto.request;

import com.codeops.entity.enums.DirectiveCategory;
import jakarta.validation.constraints.Size;

public record UpdateDirectiveRequest(
        @Size(max = 200) String name,
        @Size(max = 5000) String description,
        @Size(max = 50000) String contentMd,
        DirectiveCategory category
) {}
