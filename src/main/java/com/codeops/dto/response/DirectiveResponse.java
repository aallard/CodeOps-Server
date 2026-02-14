package com.codeops.dto.response;

import com.codeops.entity.enums.DirectiveCategory;
import com.codeops.entity.enums.DirectiveScope;

import java.time.Instant;
import java.util.UUID;

public record DirectiveResponse(UUID id, String name, String description, String contentMd,
                                DirectiveCategory category, DirectiveScope scope, UUID teamId, UUID projectId,
                                UUID createdBy, String createdByName, int version, Instant createdAt,
                                Instant updatedAt) {}
