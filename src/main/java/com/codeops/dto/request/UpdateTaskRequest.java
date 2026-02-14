package com.codeops.dto.request;

import com.codeops.entity.enums.TaskStatus;

import java.util.UUID;

public record UpdateTaskRequest(
        TaskStatus status,
        UUID assignedTo,
        String jiraKey
) {}
