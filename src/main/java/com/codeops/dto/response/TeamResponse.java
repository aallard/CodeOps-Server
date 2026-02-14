package com.codeops.dto.response;

import java.time.Instant;
import java.util.UUID;

public record TeamResponse(UUID id, String name, String description, UUID ownerId, String ownerName,
                           String teamsWebhookUrl, int memberCount, Instant createdAt, Instant updatedAt) {}
