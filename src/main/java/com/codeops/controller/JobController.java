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

@RestController
@RequestMapping("/api/v1/jobs")
@RequiredArgsConstructor
@Tag(name = "QA Jobs")
public class JobController {

    private final QaJobService qaJobService;
    private final AgentRunService agentRunService;
    private final BugInvestigationService bugInvestigationService;
    private final AuditLogService auditLogService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<JobResponse> createJob(@Valid @RequestBody CreateJobRequest request) {
        JobResponse response = qaJobService.createJob(request);
        auditLogService.log(SecurityUtils.getCurrentUserId(), null, "JOB_CREATED", "JOB", response.id(), null);
        return ResponseEntity.status(201).body(response);
    }

    @GetMapping("/{jobId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<JobResponse> getJob(@PathVariable UUID jobId) {
        return ResponseEntity.ok(qaJobService.getJob(jobId));
    }

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

    @GetMapping("/mine")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<JobSummaryResponse>> getMyJobs() {
        return ResponseEntity.ok(qaJobService.getJobsByUser(SecurityUtils.getCurrentUserId()));
    }

    @PutMapping("/{jobId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<JobResponse> updateJob(@PathVariable UUID jobId,
                                                  @Valid @RequestBody UpdateJobRequest request) {
        return ResponseEntity.ok(qaJobService.updateJob(jobId, request));
    }

    @DeleteMapping("/{jobId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteJob(@PathVariable UUID jobId) {
        qaJobService.deleteJob(jobId);
        auditLogService.log(SecurityUtils.getCurrentUserId(), null, "JOB_DELETED", "JOB", jobId, null);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{jobId}/agents")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AgentRunResponse> createAgentRun(@PathVariable UUID jobId,
                                                            @Valid @RequestBody CreateAgentRunRequest request) {
        return ResponseEntity.status(201).body(agentRunService.createAgentRun(request));
    }

    @PostMapping("/{jobId}/agents/batch")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<AgentRunResponse>> createAgentRunsBatch(
            @PathVariable UUID jobId,
            @RequestBody List<AgentType> agentTypes) {
        return ResponseEntity.status(201).body(agentRunService.createAgentRuns(jobId, agentTypes));
    }

    @GetMapping("/{jobId}/agents")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<AgentRunResponse>> getAgentRuns(@PathVariable UUID jobId) {
        return ResponseEntity.ok(agentRunService.getAgentRuns(jobId));
    }

    @PutMapping("/agents/{agentRunId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AgentRunResponse> updateAgentRun(@PathVariable UUID agentRunId,
                                                            @Valid @RequestBody UpdateAgentRunRequest request) {
        return ResponseEntity.ok(agentRunService.updateAgentRun(agentRunId, request));
    }

    @GetMapping("/{jobId}/investigation")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BugInvestigationResponse> getInvestigation(@PathVariable UUID jobId) {
        return ResponseEntity.ok(bugInvestigationService.getInvestigationByJob(jobId));
    }

    @PostMapping("/{jobId}/investigation")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BugInvestigationResponse> createInvestigation(
            @PathVariable UUID jobId,
            @Valid @RequestBody CreateBugInvestigationRequest request) {
        return ResponseEntity.status(201).body(bugInvestigationService.createInvestigation(request));
    }

    @PutMapping("/investigations/{investigationId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BugInvestigationResponse> updateInvestigation(
            @PathVariable UUID investigationId,
            @Valid @RequestBody UpdateBugInvestigationRequest request) {
        return ResponseEntity.ok(bugInvestigationService.updateInvestigation(investigationId, request));
    }
}
