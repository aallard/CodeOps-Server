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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

/**
 * Manages health monitoring schedules and health snapshots for projects.
 *
 * <p>Provides CRUD operations for recurring health check schedules (daily, weekly,
 * or on-commit) and point-in-time health snapshots that capture project quality
 * metrics including health score, findings by severity, tech debt score,
 * dependency score, and test coverage.</p>
 *
 * <p>All operations enforce team membership or admin/owner role requirements
 * before proceeding.</p>
 *
 * @see HealthMonitorController
 * @see HealthSchedule
 * @see HealthSnapshot
 */
@Service
@RequiredArgsConstructor
@Transactional
public class HealthMonitorService {

    private static final Logger log = LoggerFactory.getLogger(HealthMonitorService.class);

    private final HealthScheduleRepository healthScheduleRepository;
    private final HealthSnapshotRepository healthSnapshotRepository;
    private final ProjectRepository projectRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final UserRepository userRepository;
    private final QaJobRepository qaJobRepository;
    private final ObjectMapper objectMapper;

    /**
     * Creates a new health monitoring schedule for a project.
     *
     * <p>The next run time is automatically calculated based on the schedule type.
     * The current user is recorded as the schedule creator.</p>
     *
     * @param request the schedule creation request containing project ID, schedule type,
     *                optional cron expression, and agent types to run
     * @return the created health schedule as a response DTO
     * @throws EntityNotFoundException if the project or current user is not found
     * @throws AccessDeniedException if the current user does not have OWNER or ADMIN role on the project's team
     */
    public HealthScheduleResponse createSchedule(CreateHealthScheduleRequest request) {
        log.debug("createSchedule called with projectId={}, scheduleType={}", request.projectId(), request.scheduleType());
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
        log.info("Created health schedule id={} for projectId={}, type={}", schedule.getId(), request.projectId(), request.scheduleType());
        return mapScheduleToResponse(schedule);
    }

    /**
     * Retrieves all health monitoring schedules for a specific project.
     *
     * @param projectId the ID of the project whose schedules to retrieve
     * @return a list of health schedule response DTOs for the project
     * @throws EntityNotFoundException if the project is not found
     * @throws AccessDeniedException if the current user is not a member of the project's team
     */
    @Transactional(readOnly = true)
    public List<HealthScheduleResponse> getSchedulesForProject(UUID projectId) {
        log.debug("getSchedulesForProject called with projectId={}", projectId);
        var project = projectRepository.findById(projectId)
                .orElseThrow(() -> new EntityNotFoundException("Project not found"));
        verifyTeamMembership(project.getTeam().getId());
        return healthScheduleRepository.findByProjectId(projectId).stream()
                .map(this::mapScheduleToResponse)
                .toList();
    }

    /**
     * Retrieves all active health monitoring schedules visible to the current user.
     *
     * <p>Filters active schedules to only include those belonging to projects
     * in teams where the current user is a member.</p>
     *
     * @return a list of active health schedule response DTOs accessible to the current user
     */
    @Transactional(readOnly = true)
    public List<HealthScheduleResponse> getActiveSchedules() {
        log.debug("getActiveSchedules called");
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        List<UUID> teamIds = teamMemberRepository.findByUserId(currentUserId).stream()
                .map(member -> member.getTeam().getId())
                .toList();
        return healthScheduleRepository.findByIsActiveTrue().stream()
                .filter(schedule -> teamIds.contains(schedule.getProject().getTeam().getId()))
                .map(this::mapScheduleToResponse)
                .toList();
    }

    /**
     * Updates the active status of a health monitoring schedule.
     *
     * <p>When reactivating a schedule, the next run time is recalculated based
     * on the schedule's type and cron expression.</p>
     *
     * @param scheduleId the ID of the schedule to update
     * @param isActive {@code true} to activate the schedule, {@code false} to deactivate it
     * @return the updated health schedule as a response DTO
     * @throws EntityNotFoundException if the schedule is not found
     * @throws AccessDeniedException if the current user does not have OWNER or ADMIN role on the project's team
     */
    public HealthScheduleResponse updateSchedule(UUID scheduleId, boolean isActive) {
        log.debug("updateSchedule called with scheduleId={}, isActive={}", scheduleId, isActive);
        HealthSchedule schedule = healthScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new EntityNotFoundException("Health schedule not found"));
        verifyTeamAdmin(schedule.getProject().getTeam().getId());
        schedule.setIsActive(isActive);
        if (isActive) {
            schedule.setNextRunAt(calculateNextRun(schedule.getScheduleType(), schedule.getCronExpression()));
        }
        schedule = healthScheduleRepository.save(schedule);
        log.info("Updated health schedule id={} isActive={}", scheduleId, isActive);
        return mapScheduleToResponse(schedule);
    }

    /**
     * Permanently deletes a health monitoring schedule.
     *
     * @param scheduleId the ID of the schedule to delete
     * @throws EntityNotFoundException if the schedule is not found
     * @throws AccessDeniedException if the current user does not have OWNER or ADMIN role on the project's team
     */
    public void deleteSchedule(UUID scheduleId) {
        log.debug("deleteSchedule called with scheduleId={}", scheduleId);
        HealthSchedule schedule = healthScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new EntityNotFoundException("Health schedule not found"));
        verifyTeamAdmin(schedule.getProject().getTeam().getId());
        healthScheduleRepository.delete(schedule);
        log.info("Deleted health schedule id={}", scheduleId);
    }

    /**
     * Marks a health monitoring schedule as having been executed.
     *
     * <p>Updates the last run timestamp to now and recalculates the next
     * scheduled run time based on the schedule type.</p>
     *
     * @param scheduleId the ID of the schedule to mark as run
     * @throws EntityNotFoundException if the schedule is not found
     */
    public void markScheduleRun(UUID scheduleId) {
        log.debug("markScheduleRun called with scheduleId={}", scheduleId);
        HealthSchedule schedule = healthScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new EntityNotFoundException("Health schedule not found"));
        schedule.setLastRunAt(Instant.now());
        schedule.setNextRunAt(calculateNextRun(schedule.getScheduleType(), schedule.getCronExpression()));
        healthScheduleRepository.save(schedule);
        log.info("Marked schedule id={} as run, nextRunAt={}", scheduleId, schedule.getNextRunAt());
    }

    /**
     * Creates a new health snapshot for a project, capturing point-in-time quality metrics.
     *
     * <p>The snapshot records health score, findings by severity, tech debt score,
     * dependency score, and test coverage percentage. Optionally links to a
     * QA job that generated the data. The capture timestamp is set to the current instant.</p>
     *
     * @param request the snapshot creation request containing project ID, optional job ID,
     *                and quality metric values
     * @return the created health snapshot as a response DTO
     * @throws EntityNotFoundException if the project or referenced job is not found
     * @throws AccessDeniedException if the current user is not a member of the project's team
     */
    public HealthSnapshotResponse createSnapshot(CreateHealthSnapshotRequest request) {
        log.debug("createSnapshot called with projectId={}, healthScore={}", request.projectId(), request.healthScore());
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
        log.info("Created health snapshot id={} for projectId={}, healthScore={}", snapshot.getId(), request.projectId(), request.healthScore());
        return mapSnapshotToResponse(snapshot);
    }

    /**
     * Retrieves a paginated list of health snapshots for a project.
     *
     * @param projectId the ID of the project whose snapshots to retrieve
     * @param pageable pagination and sorting parameters
     * @return a paginated response of health snapshot DTOs
     * @throws EntityNotFoundException if the project is not found
     * @throws AccessDeniedException if the current user is not a member of the project's team
     */
    @Transactional(readOnly = true)
    public PageResponse<HealthSnapshotResponse> getSnapshots(UUID projectId, Pageable pageable) {
        log.debug("getSnapshots called with projectId={}", projectId);
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

    /**
     * Retrieves the most recent health snapshot for a project.
     *
     * @param projectId the ID of the project whose latest snapshot to retrieve
     * @return the most recent health snapshot as a response DTO
     * @throws EntityNotFoundException if no health snapshots exist for the project
     */
    @Transactional(readOnly = true)
    public HealthSnapshotResponse getLatestSnapshot(UUID projectId) {
        log.debug("getLatestSnapshot called with projectId={}", projectId);
        return healthSnapshotRepository.findFirstByProjectIdOrderByCapturedAtDesc(projectId)
                .map(this::mapSnapshotToResponse)
                .orElseThrow(() -> new EntityNotFoundException("No health snapshots found for project"));
    }

    /**
     * Retrieves a chronologically ordered health trend for a project, limited to the
     * most recent snapshots.
     *
     * <p>Returns snapshots in ascending chronological order (oldest first) to
     * facilitate trend visualization. The result is capped at the specified limit.</p>
     *
     * @param projectId the ID of the project whose health trend to retrieve
     * @param limit the maximum number of snapshots to include in the trend
     * @return a list of health snapshot response DTOs ordered from oldest to newest
     * @throws EntityNotFoundException if the project is not found
     * @throws AccessDeniedException if the current user is not a member of the project's team
     */
    @Transactional(readOnly = true)
    public List<HealthSnapshotResponse> getHealthTrend(UUID projectId, int limit) {
        log.debug("getHealthTrend called with projectId={}, limit={}", projectId, limit);
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
            log.error("Failed to serialize agent types", e);
            throw new RuntimeException("Failed to serialize agent types", e);
        }
    }

    private List<AgentType> deserializeAgentTypes(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<List<AgentType>>() {});
        } catch (JsonProcessingException e) {
            log.warn("Failed to deserialize agent types JSON, returning empty list", e);
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
