package com.codeops.controller;

import com.codeops.config.AppConstants;
import com.codeops.dto.request.BulkUpdateFindingsRequest;
import com.codeops.dto.request.CreateFindingRequest;
import com.codeops.dto.request.UpdateFindingStatusRequest;
import com.codeops.dto.response.FindingResponse;
import com.codeops.dto.response.PageResponse;
import com.codeops.entity.enums.AgentType;
import com.codeops.entity.enums.FindingStatus;
import com.codeops.entity.enums.Severity;
import com.codeops.security.SecurityUtils;
import com.codeops.service.AuditLogService;
import com.codeops.service.FindingService;
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
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for finding management operations including creation, retrieval,
 * filtering, severity counts, and status updates for QA job findings.
 *
 * <p>Findings represent issues discovered during QA agent runs (e.g., code quality
 * violations, security issues). All endpoints require authentication.</p>
 *
 * @see FindingService
 * @see AuditLogService
 */
@RestController
@RequestMapping("/api/v1/findings")
@RequiredArgsConstructor
@Tag(name = "Findings")
public class FindingController {

    private final FindingService findingService;
    private final AuditLogService auditLogService;

    /**
     * Creates a single finding.
     *
     * <p>POST {@code /api/v1/findings}</p>
     *
     * <p>Side effect: logs a {@code FINDING_CREATED} audit entry.</p>
     *
     * @param request the finding creation payload
     * @return the created finding (HTTP 201)
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<FindingResponse> createFinding(@Valid @RequestBody CreateFindingRequest request) {
        FindingResponse response = findingService.createFinding(request);
        auditLogService.log(SecurityUtils.getCurrentUserId(), null, "FINDING_CREATED", "FINDING", response.id(), null);
        return ResponseEntity.status(201).body(response);
    }

    /**
     * Creates multiple findings in a single batch operation.
     *
     * <p>POST {@code /api/v1/findings/batch}</p>
     *
     * <p>Side effect: logs a {@code FINDING_CREATED} audit entry for each finding created.</p>
     *
     * @param requests the list of finding creation payloads
     * @return list of created findings (HTTP 201)
     */
    @PostMapping("/batch")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<FindingResponse>> createFindings(
            @Valid @RequestBody List<CreateFindingRequest> requests) {
        List<FindingResponse> responses = findingService.createFindings(requests);
        responses.forEach(r -> auditLogService.log(SecurityUtils.getCurrentUserId(), null, "FINDING_CREATED", "FINDING", r.id(), null));
        return ResponseEntity.status(201).body(responses);
    }

    /**
     * Retrieves a single finding by its identifier.
     *
     * <p>GET {@code /api/v1/findings/{findingId}}</p>
     *
     * @param findingId the UUID of the finding to retrieve
     * @return the finding details
     */
    @GetMapping("/{findingId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<FindingResponse> getFinding(@PathVariable UUID findingId) {
        return ResponseEntity.ok(findingService.getFinding(findingId));
    }

    /**
     * Retrieves a paginated list of all findings for a given job.
     *
     * <p>GET {@code /api/v1/findings/job/{jobId}}</p>
     *
     * @param jobId the UUID of the job
     * @param page  zero-based page index (defaults to 0)
     * @param size  number of items per page (defaults to 20, capped at {@link AppConstants#MAX_PAGE_SIZE})
     * @return paginated list of findings, sorted by creation date descending
     */
    @GetMapping("/job/{jobId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PageResponse<FindingResponse>> getFindingsForJob(
            @PathVariable UUID jobId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, AppConstants.MAX_PAGE_SIZE),
                Sort.by("createdAt").descending());
        return ResponseEntity.ok(findingService.getFindingsForJob(jobId, pageable));
    }

    /**
     * Retrieves a paginated list of findings for a job filtered by severity level.
     *
     * <p>GET {@code /api/v1/findings/job/{jobId}/severity/{severity}}</p>
     *
     * @param jobId    the UUID of the job
     * @param severity the severity level to filter by (e.g., CRITICAL, HIGH, MEDIUM, LOW)
     * @param page     zero-based page index (defaults to 0)
     * @param size     number of items per page (defaults to 20, capped at {@link AppConstants#MAX_PAGE_SIZE})
     * @return paginated list of findings matching the given severity, sorted by creation date descending
     */
    @GetMapping("/job/{jobId}/severity/{severity}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PageResponse<FindingResponse>> getFindingsBySeverity(
            @PathVariable UUID jobId,
            @PathVariable Severity severity,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, AppConstants.MAX_PAGE_SIZE),
                Sort.by("createdAt").descending());
        return ResponseEntity.ok(findingService.getFindingsByJobAndSeverity(jobId, severity, pageable));
    }

    /**
     * Retrieves a paginated list of findings for a job filtered by the agent type that produced them.
     *
     * <p>GET {@code /api/v1/findings/job/{jobId}/agent/{agentType}}</p>
     *
     * @param jobId     the UUID of the job
     * @param agentType the type of QA agent to filter by
     * @param page      zero-based page index (defaults to 0)
     * @param size      number of items per page (defaults to 20, capped at {@link AppConstants#MAX_PAGE_SIZE})
     * @return paginated list of findings produced by the specified agent, sorted by creation date descending
     */
    @GetMapping("/job/{jobId}/agent/{agentType}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PageResponse<FindingResponse>> getFindingsByAgent(
            @PathVariable UUID jobId,
            @PathVariable AgentType agentType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, AppConstants.MAX_PAGE_SIZE),
                Sort.by("createdAt").descending());
        return ResponseEntity.ok(findingService.getFindingsByJobAndAgent(jobId, agentType, pageable));
    }

    /**
     * Retrieves a paginated list of findings for a job filtered by finding status.
     *
     * <p>GET {@code /api/v1/findings/job/{jobId}/status/{status}}</p>
     *
     * @param jobId  the UUID of the job
     * @param status the finding status to filter by
     * @param page   zero-based page index (defaults to 0)
     * @param size   number of items per page (defaults to 20, capped at {@link AppConstants#MAX_PAGE_SIZE})
     * @return paginated list of findings matching the given status, sorted by creation date descending
     */
    @GetMapping("/job/{jobId}/status/{status}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PageResponse<FindingResponse>> getFindingsByStatus(
            @PathVariable UUID jobId,
            @PathVariable FindingStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, AppConstants.MAX_PAGE_SIZE),
                Sort.by("createdAt").descending());
        return ResponseEntity.ok(findingService.getFindingsByJobAndStatus(jobId, status, pageable));
    }

    /**
     * Retrieves finding counts grouped by severity level for a given job.
     *
     * <p>GET {@code /api/v1/findings/job/{jobId}/counts}</p>
     *
     * @param jobId the UUID of the job
     * @return a map of severity levels to their respective finding counts
     */
    @GetMapping("/job/{jobId}/counts")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<Severity, Long>> getSeverityCounts(@PathVariable UUID jobId) {
        return ResponseEntity.ok(findingService.countFindingsBySeverity(jobId));
    }

    /**
     * Updates the status of a single finding.
     *
     * <p>PUT {@code /api/v1/findings/{findingId}/status}</p>
     *
     * <p>Side effect: logs a {@code FINDING_STATUS_UPDATED} audit entry with the new status name.</p>
     *
     * @param findingId the UUID of the finding to update
     * @param request   the status update payload containing the new status
     * @return the updated finding details
     */
    @PutMapping("/{findingId}/status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<FindingResponse> updateFindingStatus(
            @PathVariable UUID findingId,
            @Valid @RequestBody UpdateFindingStatusRequest request) {
        FindingResponse response = findingService.updateFindingStatus(findingId, request);
        auditLogService.log(SecurityUtils.getCurrentUserId(), null, "FINDING_STATUS_UPDATED", "FINDING", findingId, request.status().name());
        return ResponseEntity.ok(response);
    }

    /**
     * Updates the status of multiple findings in a single bulk operation.
     *
     * <p>PUT {@code /api/v1/findings/bulk-status}</p>
     *
     * <p>Side effect: logs a {@code FINDING_STATUS_UPDATED} audit entry for each finding updated.</p>
     *
     * @param request the bulk update payload containing finding IDs and the target status
     * @return list of updated finding details
     */
    @PutMapping("/bulk-status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<FindingResponse>> bulkUpdateStatus(
            @Valid @RequestBody BulkUpdateFindingsRequest request) {
        List<FindingResponse> responses = findingService.bulkUpdateFindingStatus(request);
        responses.forEach(r -> auditLogService.log(SecurityUtils.getCurrentUserId(), null, "FINDING_STATUS_UPDATED", "FINDING", r.id(), request.status().name()));
        return ResponseEntity.ok(responses);
    }
}
