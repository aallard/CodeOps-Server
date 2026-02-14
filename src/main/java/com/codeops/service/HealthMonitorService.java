package com.codeops.service;

import com.codeops.dto.request.CreateHealthScheduleRequest;
import com.codeops.dto.request.CreateHealthSnapshotRequest;
import com.codeops.dto.response.HealthScheduleResponse;
import com.codeops.dto.response.HealthSnapshotResponse;
import com.codeops.dto.response.PageResponse;
import com.codeops.entity.HealthSchedule;
import com.codeops.entity.HealthSnapshot;
import com.codeops.entity.TeamMember;
import com.codeops.entity.enums.AgentType;
import com.codeops.entity.enums.ScheduleType;
import com.codeops.entity.enums.TeamRole;
import com.codeops.repository.*;
import com.codeops.security.SecurityUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class HealthMonitorService {

    private final HealthScheduleRepository healthScheduleRepository;
    private final HealthSnapshotRepository healthSnapshotRepository;
    private final ProjectRepository projectRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final UserRepository userRepository;
    private final QaJobRepository qaJobRepository;
    private final ObjectMapper objectMapper;

    public HealthScheduleResponse createSchedule(CreateHealthScheduleRequest request) {
        var project = projectRepository.findById(request.projectId())
                .orElseThrow(() -> new EntityNotFoundException("Project not found"));
        verifyTeamAdmin(project.getTeam().getId());

        HealthSchedule schedule = HealthSchedule.builder()
                .project(project)
                .scheduleType(request.scheduleType())
                .cronExpression(request.cronExpression())
                .agentTypes(serializeAgentTypes(request.agentTypes()))
                .isActive(true)
                .createdBy(userRepository.findById(SecurityUtils.getCurrentUserId()).orElseThrow(() -> new EntityNotFoundException("User not found")))
                .nextRunAt(calculateNextRun(request.scheduleType(), request.cronExpression()))
                .build();

        schedule = healthScheduleRepository.save(schedule);
        return mapScheduleToResponse(schedule);
    }

    @Transactional(readOnly = true)
    public List<HealthScheduleResponse> getSchedulesForProject(UUID projectId) {
        var project = projectRepository.findById(projectId)
                .orElseThrow(() -> new EntityNotFoundException("Project not found"));
        verifyTeamMembership(project.getTeam().getId());
        return healthScheduleRepository.findByProjectId(projectId).stream()
                .map(this::mapScheduleToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<HealthScheduleResponse> getActiveSchedules() {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        List<UUID> teamIds = teamMemberRepository.findByUserId(currentUserId).stream()
                .map(member -> member.getTeam().getId())
                .toList();
        return healthScheduleRepository.findByIsActiveTrue().stream()
                .filter(schedule -> teamIds.contains(schedule.getProject().getTeam().getId()))
                .map(this::mapScheduleToResponse)
                .toList();
    }

    public HealthScheduleResponse updateSchedule(UUID scheduleId, boolean isActive) {
        HealthSchedule schedule = healthScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new EntityNotFoundException("Health schedule not found"));
        verifyTeamAdmin(schedule.getProject().getTeam().getId());
        schedule.setIsActive(isActive);
        if (isActive) {
            schedule.setNextRunAt(calculateNextRun(schedule.getScheduleType(), schedule.getCronExpression()));
        }
        schedule = healthScheduleRepository.save(schedule);
        return mapScheduleToResponse(schedule);
    }

    public void deleteSchedule(UUID scheduleId) {
        HealthSchedule schedule = healthScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new EntityNotFoundException("Health schedule not found"));
        verifyTeamAdmin(schedule.getProject().getTeam().getId());
        healthScheduleRepository.delete(schedule);
    }

    public void markScheduleRun(UUID scheduleId) {
        HealthSchedule schedule = healthScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new EntityNotFoundException("Health schedule not found"));
        schedule.setLastRunAt(Instant.now());
        schedule.setNextRunAt(calculateNextRun(schedule.getScheduleType(), schedule.getCronExpression()));
        healthScheduleRepository.save(schedule);
    }

    public HealthSnapshotResponse createSnapshot(CreateHealthSnapshotRequest request) {
        var project = projectRepository.findById(request.projectId())
                .orElseThrow(() -> new EntityNotFoundException("Project not found"));
        verifyTeamMembership(project.getTeam().getId());

        HealthSnapshot snapshot = HealthSnapshot.builder()
                .project(project)
                .job(request.jobId() != null ? qaJobRepository.findById(request.jobId()).orElseThrow(() -> new EntityNotFoundException("Job not found")) : null)
                .healthScore(request.healthScore())
                .findingsBySeverity(request.findingsBySeverity())
                .techDebtScore(request.techDebtScore())
                .dependencyScore(request.dependencyScore())
                .testCoveragePercent(request.testCoveragePercent())
                .capturedAt(Instant.now())
                .build();

        snapshot = healthSnapshotRepository.save(snapshot);
        return mapSnapshotToResponse(snapshot);
    }

    @Transactional(readOnly = true)
    public PageResponse<HealthSnapshotResponse> getSnapshots(UUID projectId, Pageable pageable) {
        var project = projectRepository.findById(projectId)
                .orElseThrow(() -> new EntityNotFoundException("Project not found"));
        verifyTeamMembership(project.getTeam().getId());

        Page<HealthSnapshot> page = healthSnapshotRepository.findByProjectId(projectId, pageable);
        List<HealthSnapshotResponse> content = page.getContent().stream()
                .map(this::mapSnapshotToResponse)
                .toList();
        return new PageResponse<>(content, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages(), page.isLast());
    }

    @Transactional(readOnly = true)
    public HealthSnapshotResponse getLatestSnapshot(UUID projectId) {
        return healthSnapshotRepository.findFirstByProjectIdOrderByCapturedAtDesc(projectId)
                .map(this::mapSnapshotToResponse)
                .orElseThrow(() -> new EntityNotFoundException("No health snapshots found for project"));
    }

    @Transactional(readOnly = true)
    public List<HealthSnapshotResponse> getHealthTrend(UUID projectId, int limit) {
        var project = projectRepository.findById(projectId)
                .orElseThrow(() -> new EntityNotFoundException("Project not found"));
        verifyTeamMembership(project.getTeam().getId());

        List<HealthSnapshot> snapshots = healthSnapshotRepository.findByProjectIdOrderByCapturedAtDesc(projectId);
        List<HealthSnapshot> limited = snapshots.size() > limit ? snapshots.subList(0, limit) : snapshots;
        List<HealthSnapshotResponse> result = new ArrayList<>(limited.stream()
                .map(this::mapSnapshotToResponse)
                .toList());
        Collections.reverse(result);
        return result;
    }

    private HealthScheduleResponse mapScheduleToResponse(HealthSchedule schedule) {
        return new HealthScheduleResponse(
                schedule.getId(),
                schedule.getProject().getId(),
                schedule.getScheduleType(),
                schedule.getCronExpression(),
                deserializeAgentTypes(schedule.getAgentTypes()),
                schedule.getIsActive(),
                schedule.getLastRunAt(),
                schedule.getNextRunAt(),
                schedule.getCreatedAt()
        );
    }

    private HealthSnapshotResponse mapSnapshotToResponse(HealthSnapshot snapshot) {
        return new HealthSnapshotResponse(
                snapshot.getId(),
                snapshot.getProject().getId(),
                snapshot.getJob() != null ? snapshot.getJob().getId() : null,
                snapshot.getHealthScore(),
                snapshot.getFindingsBySeverity(),
                snapshot.getTechDebtScore(),
                snapshot.getDependencyScore(),
                snapshot.getTestCoveragePercent(),
                snapshot.getCapturedAt()
        );
    }

    private Instant calculateNextRun(ScheduleType type, String cronExpression) {
        if (type == ScheduleType.DAILY) return Instant.now().plus(1, ChronoUnit.DAYS);
        if (type == ScheduleType.WEEKLY) return Instant.now().plus(7, ChronoUnit.DAYS);
        return null; // ON_COMMIT — triggered externally
    }

    private String serializeAgentTypes(List<AgentType> agentTypes) {
        try {
            return objectMapper.writeValueAsString(agentTypes);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize agent types", e);
        }
    }

    private List<AgentType> deserializeAgentTypes(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<List<AgentType>>() {});
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }

    private void verifyTeamMembership(UUID teamId) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        if (!teamMemberRepository.existsByTeamIdAndUserId(teamId, currentUserId)) {
            throw new AccessDeniedException("Not a member of this team");
        }
    }

    private void verifyTeamAdmin(UUID teamId) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        TeamMember member = teamMemberRepository.findByTeamIdAndUserId(teamId, currentUserId)
                .orElseThrow(() -> new AccessDeniedException("Not a member of this team"));
        if (member.getRole() != TeamRole.OWNER && member.getRole() != TeamRole.ADMIN) {
            throw new AccessDeniedException("Requires OWNER or ADMIN role");
        }
    }
}
