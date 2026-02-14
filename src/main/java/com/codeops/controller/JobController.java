package com.codeops.controller;

import com.codeops.config.AppConstants;
import com.codeops.dto.request.CreateAgentRunRequest;
import com.codeops.dto.request.CreateBugInvestigationRequest;
import com.codeops.dto.request.CreateJobRequest;
import com.codeops.dto.request.UpdateAgentRunRequest;
import com.codeops.dto.request.UpdateBugInvestigationRequest;
import com.codeops.dto.request.UpdateJobRequest;
import com.codeops.dto.response.AgentRunResponse;
import com.codeops.dto.response.BugInvestigationResponse;
import com.codeops.dto.response.JobResponse;
import com.codeops.dto.response.JobSummaryResponse;
import com.codeops.dto.response.PageResponse;
import com.codeops.entity.enums.AgentType;
import com.codeops.security.SecurityUtils;
import com.codeops.service.AgentRunService;
import com.codeops.service.AuditLogService;
import com.codeops.service.BugInvestigationService;
import com.codeops.service.QaJobService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for QA job lifecycle management, including job CRUD, agent run
 * orchestration, and bug investigation operations.
 *
 * <p>Jobs follow a lifecycle of PENDING, RUNNING, COMPLETED, FAILED, or CANCELLED.
 * Each job can have multiple agent runs (one per agent type) and an optional bug
 * investigation. All endpoints require authentication.</p>
 *
 * @see QaJobService
 * @see AgentRunService
 * @see BugInvestigationService
 * @see AuditLogService
 */
@RestController
@RequestMapping("/api/v1/jobs")
@RequiredArgsConstructor
@Tag(name = "QA Jobs")
public class JobController {

    private final QaJobService qaJobService;
    private final AgentRunService agentRunService;
    private final BugInvestigationService bugInvestigationService;
    private final AuditLogService auditLogService;

    /**
     * Creates a new QA job.
     *
     * <p>POST {@code /api/v1/jobs}</p>
     *
     * <p>Side effect: logs a {@code JOB_CREATED} audit entry.</p>
     *
     * @param request the job creation payload containing project reference and configuration
     * @return the created job (HTTP 201)
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<JobResponse> createJob(@Valid @RequestBody CreateJobRequest request) {
        JobResponse response = qaJobService.createJob(request);
        auditLogService.log(SecurityUtils.getCurrentUserId(), null, "JOB_CREATED", "JOB", response.id(), null);
        return ResponseEntity.status(201).body(response);
    }

    /**
     * Retrieves a single QA job by its identifier.
     *
     * <p>GET {@code /api/v1/jobs/{jobId}}</p>
     *
     * @param jobId the UUID of the job to retrieve
     * @return the job details
     */
    @GetMapping("/{jobId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<JobResponse> getJob(@PathVariable UUID jobId) {
        return ResponseEntity.ok(qaJobService.getJob(jobId));
    }

    /**
     * Retrieves a paginated list of job summaries for a given project.
     *
     * <p>GET {@code /api/v1/jobs/project/{projectId}}</p>
     *
     * @param projectId the UUID of the project
     * @param page      zero-based page index (defaults to 0)
     * @param size      number of items per page (defaults to 20, capped at {@link AppConstants#MAX_PAGE_SIZE})
     * @return paginated list of job summaries, sorted by creation date descending
     */
    @GetMapping("/project/{projectId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PageResponse<JobSummaryResponse>> getJobsForProject(
            @PathVariable UUID projectId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, AppConstants.MAX_PAGE_SIZE),
                Sort.by("createdAt").descending());
        return ResponseEntity.ok(qaJobService.getJobsForProject(projectId, pageable));
    }

    /**
     * Retrieves a paginated list of jobs created by the currently authenticated user.
     *
     * <p>GET {@code /api/v1/jobs/mine}</p>
     *
     * @param page zero-based page index (defaults to 0)
     * @param size number of items per page (defaults to 20, capped at {@link AppConstants#MAX_PAGE_SIZE})
     * @return paginated list of job summaries belonging to the current user, sorted by creation date descending
     */
    @GetMapping("/mine")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PageResponse<JobSummaryResponse>> getMyJobs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, AppConstants.MAX_PAGE_SIZE),
                Sort.by("createdAt").descending());
        return ResponseEntity.ok(qaJobService.getJobsByUser(SecurityUtils.getCurrentUserId(), pageable));
    }

    /**
     * Updates an existing QA job's properties.
     *
     * <p>PUT {@code /api/v1/jobs/{jobId}}</p>
     *
     * <p>Side effect: logs a {@code JOB_UPDATED} audit entry.</p>
     *
     * @param jobId   the UUID of the job to update
     * @param request the update payload containing the new job properties
     * @return the updated job details
     */
    @PutMapping("/{jobId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<JobResponse> updateJob(@PathVariable UUID jobId,
                                                  @Valid @RequestBody UpdateJobRequest request) {
        JobResponse response = qaJobService.updateJob(jobId, request);
        auditLogService.log(SecurityUtils.getCurrentUserId(), null, "JOB_UPDATED", "JOB", jobId, null);
        return ResponseEntity.ok(response);
    }

    /**
     * Deletes a QA job by its identifier.
     *
     * <p>DELETE {@code /api/v1/jobs/{jobId}}</p>
     *
     * <p>Side effect: logs a {@code JOB_DELETED} audit entry.</p>
     *
     * @param jobId the UUID of the job to delete
     * @return HTTP 204 No Content on successful deletion
     */
    @DeleteMapping("/{jobId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteJob(@PathVariable UUID jobId) {
        qaJobService.deleteJob(jobId);
        auditLogService.log(SecurityUtils.getCurrentUserId(), null, "JOB_DELETED", "JOB", jobId, null);
        return ResponseEntity.noContent().build();
    }

    /**
     * Creates a new agent run within a job.
     *
     * <p>POST {@code /api/v1/jobs/{jobId}/agents}</p>
     *
     * <p>Side effect: logs an {@code AGENT_RUN_CREATED} audit entry.</p>
     *
     * @param jobId   the UUID of the parent job
     * @param request the agent run creation payload containing agent type and configuration
     * @return the created agent run (HTTP 201)
     */
    @PostMapping("/{jobId}/agents")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AgentRunResponse> createAgentRun(@PathVariable UUID jobId,
                                                            @Valid @RequestBody CreateAgentRunRequest request) {
        AgentRunResponse response = agentRunService.createAgentRun(request);
        auditLogService.log(SecurityUtils.getCurrentUserId(), null, "AGENT_RUN_CREATED", "AGENT_RUN", response.id(), null);
        return ResponseEntity.status(201).body(response);
    }

    /**
     * Creates multiple agent runs for a job in a single batch operation, one per agent type.
     *
     * <p>POST {@code /api/v1/jobs/{jobId}/agents/batch}</p>
     *
     * <p>Side effect: logs an {@code AGENT_RUN_CREATED} audit entry for each agent run created.</p>
     *
     * @param jobId      the UUID of the parent job
     * @param agentTypes the list of agent types to create runs for
     * @return list of created agent runs (HTTP 201)
     */
    @PostMapping("/{jobId}/agents/batch")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<AgentRunResponse>> createAgentRunsBatch(
            @PathVariable UUID jobId,
            @RequestBody List<AgentType> agentTypes) {
        List<AgentRunResponse> responses = agentRunService.createAgentRuns(jobId, agentTypes);
        responses.forEach(r -> auditLogService.log(SecurityUtils.getCurrentUserId(), null, "AGENT_RUN_CREATED", "AGENT_RUN", r.id(), null));
        return ResponseEntity.status(201).body(responses);
    }

    /**
     * Retrieves all agent runs associated with a given job.
     *
     * <p>GET {@code /api/v1/jobs/{jobId}/agents}</p>
     *
     * @param jobId the UUID of the job
     * @return list of agent runs for the job
     */
    @GetMapping("/{jobId}/agents")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<AgentRunResponse>> getAgentRuns(@PathVariable UUID jobId) {
        return ResponseEntity.ok(agentRunService.getAgentRuns(jobId));
    }

    /**
     * Updates an existing agent run's properties (e.g., status, results).
     *
     * <p>PUT {@code /api/v1/jobs/agents/{agentRunId}}</p>
     *
     * <p>Side effect: logs an {@code AGENT_RUN_UPDATED} audit entry.</p>
     *
     * @param agentRunId the UUID of the agent run to update
     * @param request    the update payload containing the new agent run properties
     * @return the updated agent run details
     */
    @PutMapping("/agents/{agentRunId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AgentRunResponse> updateAgentRun(@PathVariable UUID agentRunId,
                                                            @Valid @RequestBody UpdateAgentRunRequest request) {
        AgentRunResponse response = agentRunService.updateAgentRun(agentRunId, request);
        auditLogService.log(SecurityUtils.getCurrentUserId(), null, "AGENT_RUN_UPDATED", "AGENT_RUN", agentRunId, null);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves the bug investigation associated with a given job.
     *
     * <p>GET {@code /api/v1/jobs/{jobId}/investigation}</p>
     *
     * @param jobId the UUID of the job
     * @return the bug investigation details for the job
     */
    @GetMapping("/{jobId}/investigation")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BugInvestigationResponse> getInvestigation(@PathVariable UUID jobId) {
        return ResponseEntity.ok(bugInvestigationService.getInvestigationByJob(jobId));
    }

    /**
     * Creates a new bug investigation for a job.
     *
     * <p>POST {@code /api/v1/jobs/{jobId}/investigation}</p>
     *
     * <p>Side effect: logs an {@code INVESTIGATION_CREATED} audit entry.</p>
     *
     * @param jobId   the UUID of the parent job
     * @param request the bug investigation creation payload
     * @return the created bug investigation (HTTP 201)
     */
    @PostMapping("/{jobId}/investigation")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BugInvestigationResponse> createInvestigation(
            @PathVariable UUID jobId,
            @Valid @RequestBody CreateBugInvestigationRequest request) {
        BugInvestigationResponse response = bugInvestigationService.createInvestigation(request);
        auditLogService.log(SecurityUtils.getCurrentUserId(), null, "INVESTIGATION_CREATED", "BUG_INVESTIGATION", response.id(), null);
        return ResponseEntity.status(201).body(response);
    }

    /**
     * Updates an existing bug investigation's properties.
     *
     * <p>PUT {@code /api/v1/jobs/investigations/{investigationId}}</p>
     *
     * <p>Side effect: logs an {@code INVESTIGATION_UPDATED} audit entry.</p>
     *
     * @param investigationId the UUID of the bug investigation to update
     * @param request         the update payload containing the new investigation properties
     * @return the updated bug investigation details
     */
    @PutMapping("/investigations/{investigationId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BugInvestigationResponse> updateInvestigation(
            @PathVariable UUID investigationId,
            @Valid @RequestBody UpdateBugInvestigationRequest request) {
        BugInvestigationResponse response = bugInvestigationService.updateInvestigation(investigationId, request);
        auditLogService.log(SecurityUtils.getCurrentUserId(), null, "INVESTIGATION_UPDATED", "BUG_INVESTIGATION", investigationId, null);
        return ResponseEntity.ok(response);
    }
}
