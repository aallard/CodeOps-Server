package com.codeops.dto.response;

import com.codeops.entity.enums.AgentType;
import com.codeops.entity.enums.ScheduleType;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record HealthScheduleResponse(UUID id, UUID projectId, ScheduleType scheduleType, String cronExpression,
                                     List<AgentType> agentTypes, boolean isActive, Instant lastRunAt,
                                     Instant nextRunAt, Instant createdAt) {}
