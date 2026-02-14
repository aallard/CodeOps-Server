package com.codeops.dto.response;

import com.codeops.entity.enums.Priority;
import com.codeops.entity.enums.TaskStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TaskResponse(UUID id, UUID jobId, int taskNumber, String title, String description, String promptMd,
                           String promptS3Key, List<UUID> findingIds, Priority priority, TaskStatus status,
                           UUID assignedTo, String assignedToName, String jiraKey, Instant createdAt) {}
