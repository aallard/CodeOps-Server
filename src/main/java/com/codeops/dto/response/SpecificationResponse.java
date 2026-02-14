package com.codeops.dto.response;

import com.codeops.entity.enums.SpecType;

import java.time.Instant;
import java.util.UUID;

public record SpecificationResponse(UUID id, UUID jobId, String name, SpecType specType, String s3Key,
                                    Instant createdAt) {}
