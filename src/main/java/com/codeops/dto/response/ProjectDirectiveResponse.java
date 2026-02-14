package com.codeops.dto.response;

import com.codeops.entity.enums.DirectiveCategory;

import java.util.UUID;

public record ProjectDirectiveResponse(UUID projectId, UUID directiveId, String directiveName,
                                       DirectiveCategory category, boolean enabled) {}
