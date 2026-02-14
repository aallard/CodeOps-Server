package com.codeops.dto.request;

import com.codeops.entity.enums.AgentType;
import com.codeops.entity.enums.ScheduleType;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record CreateHealthScheduleRequest(
        @NotNull UUID projectId,
        @NotNull ScheduleType scheduleType,
        String cronExpression,
        @NotEmpty List<AgentType> agentTypes
) {}
