package com.codeops.dto.response;

import java.time.Instant;
import java.util.UUID;

public record AuditLogResponse(Long id, UUID userId, String userName, UUID teamId, String action,
                                String entityType, UUID entityId, String details, String ipAddress,
                                Instant createdAt) {}
