package com.codeops.dto.request;

import com.codeops.entity.enums.TaskStatus;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record UpdateTaskRequest(
        TaskStatus status,
        UUID assignedTo,
        @Size(max = 200) String jiraKey
) {}
