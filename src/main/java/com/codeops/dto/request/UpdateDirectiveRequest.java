package com.codeops.dto.request;

import com.codeops.entity.enums.DirectiveCategory;
import jakarta.validation.constraints.Size;

public record UpdateDirectiveRequest(
        @Size(max = 200) String name,
        String description,
        String contentMd,
        DirectiveCategory category
) {}
