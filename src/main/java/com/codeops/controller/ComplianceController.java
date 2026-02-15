package com.codeops.controller;

import com.codeops.config.AppConstants;
import com.codeops.dto.request.CreateComplianceItemRequest;
import com.codeops.dto.request.CreateSpecificationRequest;
import com.codeops.dto.response.ComplianceItemResponse;
import com.codeops.dto.response.PageResponse;
import com.codeops.dto.response.SpecificationResponse;
import com.codeops.entity.enums.ComplianceStatus;
import com.codeops.security.SecurityUtils;
import com.codeops.service.AuditLogService;
import com.codeops.service.ComplianceService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * REST controller for compliance management operations including specifications,
 * compliance items, and compliance summaries scoped to QA jobs.
 *
 * <p>All endpoints require authentication. Authorization is enforced via
 * {@code @PreAuthorize("isAuthenticated()")} on each method.</p>
 *
 * @see ComplianceService
 * @see AuditLogService
 */
@RestController
@RequestMapping("/api/v1/compliance")
@RequiredArgsConstructor
@Tag(name = "Compliance")
public class ComplianceController {

    private static final Logger log = LoggerFactory.getLogger(ComplianceController.class);

    private final ComplianceService complianceService;
    private final AuditLogService auditLogService;

    /**
     * Creates a new compliance specification.
     *
     * <p>POST {@code /api/v1/compliance/specs}</p>
     *
     * <p>Side effect: logs a {@code SPECIFICATION_CREATED} audit entry.</p>
     *
     * @param request the specification creation payload
     * @return the created specification (HTTP 201)
     */
    @PostMapping("/specs")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SpecificationResponse> createSpecification(@Valid @RequestBody CreateSpecificationRequest request) {
        log.debug("createSpecification called");
        SpecificationResponse response = complianceService.createSpecification(request);
        auditLogService.log(SecurityUtils.getCurrentUserId(), null, "SPECIFICATION_CREATED", "SPECIFICATION", response.id(), null);
        return ResponseEntity.status(201).body(response);
    }

    /**
     * Retrieves a paginated list of specifications associated with a given job.
     *
     * <p>GET {@code /api/v1/compliance/specs/job/{jobId}}</p>
     *
     * @param jobId the UUID of the job to retrieve specifications for
     * @param page  zero-based page index (defaults to 0)
     * @param size  number of items per page (defaults to 20, capped at {@link AppConstants#MAX_PAGE_SIZE})
     * @return paginated list of specification responses, sorted by creation date descending
     */
    @GetMapping("/specs/job/{jobId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PageResponse<SpecificationResponse>> getSpecificationsForJob(
            @PathVariable UUID jobId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.debug("getSpecificationsForJob called with jobId={}", jobId);
        Pageable pageable = PageRequest.of(page, Math.min(size, AppConstants.MAX_PAGE_SIZE),
                Sort.by("createdAt").descending());
        return ResponseEntity.ok(complianceService.getSpecificationsForJob(jobId, pageable));
    }

    /**
     * Creates a single compliance item.
     *
     * <p>POST {@code /api/v1/compliance/items}</p>
     *
     * <p>Side effect: logs a {@code COMPLIANCE_ITEM_CREATED} audit entry.</p>
     *
     * @param request the compliance item creation payload
     * @return the created compliance item (HTTP 201)
     */
    @PostMapping("/items")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ComplianceItemResponse> createComplianceItem(@Valid @RequestBody CreateComplianceItemRequest request) {
        log.debug("createComplianceItem called");
        ComplianceItemResponse response = complianceService.createComplianceItem(request);
        auditLogService.log(SecurityUtils.getCurrentUserId(), null, "COMPLIANCE_ITEM_CREATED", "COMPLIANCE_ITEM", response.id(), null);
        return ResponseEntity.status(201).body(response);
    }

    /**
     * Creates multiple compliance items in a single batch operation.
     *
     * <p>POST {@code /api/v1/compliance/items/batch}</p>
     *
     * <p>Side effect: logs a {@code COMPLIANCE_ITEM_CREATED} audit entry for each item created.</p>
     *
     * @param requests the list of compliance item creation payloads
     * @return list of created compliance items (HTTP 201)
     */
    @PostMapping("/items/batch")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ComplianceItemResponse>> createComplianceItems(@Valid @RequestBody List<CreateComplianceItemRequest> requests) {
        log.debug("createComplianceItems called with batchSize={}", requests.size());
        List<ComplianceItemResponse> responses = complianceService.createComplianceItems(requests);
        responses.forEach(r -> auditLogService.log(SecurityUtils.getCurrentUserId(), null, "COMPLIANCE_ITEM_CREATED", "COMPLIANCE_ITEM", r.id(), null));
        return ResponseEntity.status(201).body(responses);
    }

    /**
     * Retrieves a paginated list of compliance items for a given job.
     *
     * <p>GET {@code /api/v1/compliance/items/job/{jobId}}</p>
     *
     * @param jobId the UUID of the job to retrieve compliance items for
     * @param page  zero-based page index (defaults to 0)
     * @param size  number of items per page (defaults to 20, capped at {@link AppConstants#MAX_PAGE_SIZE})
     * @return paginated list of compliance item responses, sorted by creation date descending
     */
    @GetMapping("/items/job/{jobId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PageResponse<ComplianceItemResponse>> getComplianceItemsForJob(
            @PathVariable UUID jobId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.debug("getComplianceItemsForJob called with jobId={}", jobId);
        Pageable pageable = PageRequest.of(page, Math.min(size, AppConstants.MAX_PAGE_SIZE),
                Sort.by("createdAt").descending());
        return ResponseEntity.ok(complianceService.getComplianceItemsForJob(jobId, pageable));
    }

    /**
     * Retrieves a paginated list of compliance items for a job filtered by compliance status.
     *
     * <p>GET {@code /api/v1/compliance/items/job/{jobId}/status/{status}}</p>
     *
     * @param jobId  the UUID of the job
     * @param status the compliance status to filter by
     * @param page   zero-based page index (defaults to 0)
     * @param size   number of items per page (defaults to 20, capped at {@link AppConstants#MAX_PAGE_SIZE})
     * @return paginated list of compliance items matching the given status, sorted by creation date descending
     */
    @GetMapping("/items/job/{jobId}/status/{status}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PageResponse<ComplianceItemResponse>> getComplianceItemsByStatus(
            @PathVariable UUID jobId,
            @PathVariable ComplianceStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.debug("getComplianceItemsByStatus called with jobId={}, status={}", jobId, status);
        Pageable pageable = PageRequest.of(page, Math.min(size, AppConstants.MAX_PAGE_SIZE),
                Sort.by("createdAt").descending());
        return ResponseEntity.ok(complianceService.getComplianceItemsByStatus(jobId, status, pageable));
    }

    /**
     * Retrieves an aggregate compliance summary for a given job, including counts
     * and pass/fail ratios across all compliance items.
     *
     * <p>GET {@code /api/v1/compliance/summary/job/{jobId}}</p>
     *
     * @param jobId the UUID of the job to summarize
     * @return a map of summary metric names to their values
     */
    @GetMapping("/summary/job/{jobId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getComplianceSummary(@PathVariable UUID jobId) {
        log.debug("getComplianceSummary called with jobId={}", jobId);
        return ResponseEntity.ok(complianceService.getComplianceSummary(jobId));
    }
}
