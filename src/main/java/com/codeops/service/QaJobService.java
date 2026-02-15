package com.codeops.service;

import com.codeops.dto.request.CreateJobRequest;
import com.codeops.dto.request.UpdateJobRequest;
import com.codeops.dto.response.JobResponse;
import com.codeops.dto.response.JobSummaryResponse;
import com.codeops.dto.response.PageResponse;
import com.codeops.entity.QaJob;
import com.codeops.entity.TeamMember;
import com.codeops.entity.enums.JobStatus;
import com.codeops.entity.enums.TeamRole;
import com.codeops.repository.*;
import com.codeops.security.SecurityUtils;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Manages QA job lifecycle including creation, retrieval, updating, and deletion.
 *
 * <p>QA jobs represent automated quality analysis runs against a project's codebase.
 * Jobs progress through states: PENDING, RUNNING, COMPLETED, FAILED, or CANCELLED.
 * When a job completes with a health score, the associated project's health score
 * is automatically updated via {@link ProjectService#updateHealthScore(UUID, int)}.</p>
 *
 * <p>All operations enforce team membership requirements. Job deletion requires
 * admin or owner role on the project's team.</p>
 *
 * @see JobController
 * @see QaJob
 * @see ProjectService
 */
@Service
@RequiredArgsConstructor
@Transactional
public class QaJobService {

    private static final Logger log = LoggerFactory.getLogger(QaJobService.class);

    private final QaJobRepository qaJobRepository;
    private final AgentRunRepository agentRunRepository;
    private final FindingRepository findingRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final ProjectService projectService;

    /**
     * Creates a new QA job for a project with initial PENDING status.
     *
     * <p>All finding counters are initialized to zero. The current user is recorded
     * as the job initiator.</p>
     *
     * @param request the job creation request containing project ID, mode, name,
     *                branch, optional config JSON, and optional Jira ticket key
     * @return the created job as a response DTO
     * @throws EntityNotFoundException if the project or current user is not found
     * @throws AccessDeniedException if the current user is not a member of the project's team
     */
    public JobResponse createJob(CreateJobRequest request) {
        log.debug("createJob called with projectId={}, mode={}, name={}", request.projectId(), request.mode(), request.name());
        var project = projectRepository.findById(request.projectId())
                .orElseThrow(() -> new EntityNotFoundException("Project not found"));
        verifyTeamMembership(project.getTeam().getId());

        QaJob job = QaJob.builder()
                .project(project)
                .mode(request.mode())
                .status(JobStatus.PENDING)
                .name(request.name())
                .branch(request.branch())
                .configJson(request.configJson())
                .jiraTicketKey(request.jiraTicketKey())
                .totalFindings(0)
                .criticalCount(0)
                .highCount(0)
                .mediumCount(0)
                .lowCount(0)
                .startedBy(userRepository.findById(SecurityUtils.getCurrentUserId()).orElseThrow(() -> new EntityNotFoundException("User not found")))
                .build();

        job = qaJobRepository.save(job);
        log.info("QA job created: jobId={}, projectId={}, mode={}, status={}", job.getId(), request.projectId(), request.mode(), job.getStatus());
        return mapToJobResponse(job);
    }

    /**
     * Retrieves a single QA job by its ID with full detail.
     *
     * @param jobId the ID of the job to retrieve
     * @return the job as a full response DTO including all counters and metadata
     * @throws EntityNotFoundException if no job exists with the given ID
     * @throws AccessDeniedException if the current user is not a member of the job's project team
     */
    @Transactional(readOnly = true)
    public JobResponse getJob(UUID jobId) {
        log.debug("getJob called with jobId={}", jobId);
        QaJob job = qaJobRepository.findById(jobId)
                .orElseThrow(() -> new EntityNotFoundException("Job not found"));
        verifyTeamMembership(job.getProject().getTeam().getId());
        return mapToJobResponse(job);
    }

    /**
     * Retrieves a paginated list of job summaries for a specific project.
     *
     * @param projectId the ID of the project whose jobs to retrieve
     * @param pageable pagination and sorting parameters
     * @return a paginated response of job summary DTOs
     * @throws EntityNotFoundException if the project is not found
     * @throws AccessDeniedException if the current user is not a member of the project's team
     */
    @Transactional(readOnly = true)
    public PageResponse<JobSummaryResponse> getJobsForProject(UUID projectId, Pageable pageable) {
        log.debug("getJobsForProject called with projectId={}", projectId);
        var project = projectRepository.findById(projectId)
                .orElseThrow(() -> new EntityNotFoundException("Project not found"));
        verifyTeamMembership(project.getTeam().getId());

        Page<QaJob> page = qaJobRepository.findByProjectId(projectId, pageable);
        List<JobSummaryResponse> content = page.getContent().stream()
                .map(this::mapToJobSummaryResponse)
                .toList();
        return new PageResponse<>(content, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages(), page.isLast());
    }

    /**
     * Retrieves a paginated list of job summaries started by a specific user.
     *
     * @param userId the ID of the user whose jobs to retrieve
     * @param pageable pagination and sorting parameters
     * @return a paginated response of job summary DTOs for jobs initiated by the specified user
     */
    @Transactional(readOnly = true)
    public PageResponse<JobSummaryResponse> getJobsByUser(UUID userId, Pageable pageable) {
        log.debug("getJobsByUser called with userId={}", userId);
        Page<QaJob> page = qaJobRepository.findByStartedById(userId, pageable);
        List<JobSummaryResponse> content = page.getContent().stream()
                .map(this::mapToJobSummaryResponse)
                .toList();
        return new PageResponse<>(content, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages(), page.isLast());
    }

    /**
     * Updates an existing QA job's mutable fields.
     *
     * <p>Only non-null fields in the request are applied. When the status transitions
     * to COMPLETED and a health score is provided, the associated project's health
     * score is automatically updated as a side effect.</p>
     *
     * @param jobId the ID of the job to update
     * @param request the update request containing optional status, summary markdown,
     *                overall result, health score, finding counts, and timestamps
     * @return the updated job as a response DTO
     * @throws EntityNotFoundException if no job exists with the given ID
     * @throws AccessDeniedException if the current user is not a member of the job's project team
     */
    public JobResponse updateJob(UUID jobId, UpdateJobRequest request) {
        log.debug("updateJob called with jobId={}", jobId);
        QaJob job = qaJobRepository.findById(jobId)
                .orElseThrow(() -> new EntityNotFoundException("Job not found"));
        verifyTeamMembership(job.getProject().getTeam().getId());

        JobStatus previousStatus = job.getStatus();

        if (request.status() != null) job.setStatus(request.status());
        if (request.summaryMd() != null) job.setSummaryMd(request.summaryMd());
        if (request.overallResult() != null) job.setOverallResult(request.overallResult());
        if (request.healthScore() != null) job.setHealthScore(request.healthScore());
        if (request.totalFindings() != null) job.setTotalFindings(request.totalFindings());
        if (request.criticalCount() != null) job.setCriticalCount(request.criticalCount());
        if (request.highCount() != null) job.setHighCount(request.highCount());
        if (request.mediumCount() != null) job.setMediumCount(request.mediumCount());
        if (request.lowCount() != null) job.setLowCount(request.lowCount());
        if (request.completedAt() != null) job.setCompletedAt(request.completedAt());
        if (request.startedAt() != null) job.setStartedAt(request.startedAt());

        if (request.status() != null && request.status() != previousStatus) {
            log.info("QA job status transition: jobId={}, {} -> {}", jobId, previousStatus, request.status());
        }

        if (request.status() == JobStatus.COMPLETED && request.healthScore() != null) {
            log.info("QA job completed: jobId={}, healthScore={}, totalFindings={}, critical={}, high={}, medium={}, low={}",
                    jobId, request.healthScore(), request.totalFindings(), request.criticalCount(),
                    request.highCount(), request.mediumCount(), request.lowCount());
            projectService.updateHealthScore(job.getProject().getId(), request.healthScore());
        }

        job = qaJobRepository.save(job);
        return mapToJobResponse(job);
    }

    /**
     * Permanently deletes a QA job and all associated data.
     *
     * <p>Deletion requires OWNER or ADMIN role on the job's project team.</p>
     *
     * @param jobId the ID of the job to delete
     * @throws EntityNotFoundException if no job exists with the given ID
     * @throws AccessDeniedException if the current user does not have OWNER or ADMIN role on the project's team
     */
    public void deleteJob(UUID jobId) {
        log.debug("deleteJob called with jobId={}", jobId);
        QaJob job = qaJobRepository.findById(jobId)
                .orElseThrow(() -> new EntityNotFoundException("Job not found"));
        verifyTeamAdmin(job.getProject().getTeam().getId());
        qaJobRepository.delete(job);
        log.info("QA job deleted: jobId={}, projectId={}", jobId, job.getProject().getId());
    }

    private JobResponse mapToJobResponse(QaJob job) {
        return new JobResponse(
                job.getId(),
                job.getProject().getId(),
                job.getProject().getName(),
                job.getMode(),
                job.getStatus(),
                job.getName(),
                job.getBranch(),
                job.getConfigJson(),
                job.getSummaryMd(),
                job.getOverallResult(),
                job.getHealthScore(),
                job.getTotalFindings(),
                job.getCriticalCount(),
                job.getHighCount(),
                job.getMediumCount(),
                job.getLowCount(),
                job.getJiraTicketKey(),
                job.getStartedBy().getId(),
                job.getStartedBy().getDisplayName(),
                job.getStartedAt(),
                job.getCompletedAt(),
                job.getCreatedAt()
        );
    }

    private JobSummaryResponse mapToJobSummaryResponse(QaJob job) {
        return new JobSummaryResponse(
                job.getId(),
                job.getProject().getName(),
                job.getMode(),
                job.getStatus(),
                job.getName(),
                job.getOverallResult(),
                job.getHealthScore(),
                job.getTotalFindings(),
                job.getCriticalCount(),
                job.getCompletedAt(),
                job.getCreatedAt()
        );
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
