package com.codeops.controller;

import com.codeops.config.AppConstants;
import com.codeops.dto.request.CreateDependencyScanRequest;
import com.codeops.dto.request.CreateVulnerabilityRequest;
import com.codeops.dto.response.DependencyScanResponse;
import com.codeops.dto.response.PageResponse;
import com.codeops.dto.response.VulnerabilityResponse;
import com.codeops.entity.enums.Severity;
import com.codeops.entity.enums.VulnerabilityStatus;
import com.codeops.security.SecurityUtils;
import com.codeops.service.AuditLogService;
import com.codeops.service.DependencyService;
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
import java.util.UUID;

/**
 * REST controller for dependency scanning and vulnerability management operations.
 *
 * <p>Provides endpoints for creating and retrieving dependency scans, recording
 * vulnerabilities discovered during scans, and updating vulnerability statuses.
 * All endpoints require authentication.</p>
 *
 * @see DependencyService
 * @see AuditLogService
 */
@RestController
@RequestMapping("/api/v1/dependencies")
@RequiredArgsConstructor
@Tag(name = "Dependencies")
public class DependencyController {

    private static final Logger log = LoggerFactory.getLogger(DependencyController.class);

    private final DependencyService dependencyService;
    private final AuditLogService auditLogService;

    /**
     * Creates a new dependency scan record.
     *
     * <p>POST {@code /api/v1/dependencies/scans}</p>
     *
     * <p>Side effect: logs a {@code DEPENDENCY_SCAN_CREATED} audit entry.</p>
     *
     * @param request the dependency scan creation payload
     * @return the created dependency scan (HTTP 201)
     */
    @PostMapping("/scans")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DependencyScanResponse> createScan(@Valid @RequestBody CreateDependencyScanRequest request) {
        log.debug("createScan called");
        DependencyScanResponse response = dependencyService.createScan(request);
        auditLogService.log(SecurityUtils.getCurrentUserId(), null, "DEPENDENCY_SCAN_CREATED", "DEPENDENCY_SCAN", response.id(), null);
        return ResponseEntity.status(201).body(response);
    }

    /**
     * Retrieves a single dependency scan by its identifier.
     *
     * <p>GET {@code /api/v1/dependencies/scans/{scanId}}</p>
     *
     * @param scanId the UUID of the scan to retrieve
     * @return the dependency scan details
     */
    @GetMapping("/scans/{scanId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DependencyScanResponse> getScan(@PathVariable UUID scanId) {
        log.debug("getScan called with scanId={}", scanId);
        return ResponseEntity.ok(dependencyService.getScan(scanId));
    }

    /**
     * Retrieves a paginated list of dependency scans for a given project.
     *
     * <p>GET {@code /api/v1/dependencies/scans/project/{projectId}}</p>
     *
     * @param projectId the UUID of the project
     * @param page      zero-based page index (defaults to 0)
     * @param size      number of items per page (defaults to 20, capped at {@link AppConstants#MAX_PAGE_SIZE})
     * @return paginated list of dependency scans, sorted by creation date descending
     */
    @GetMapping("/scans/project/{projectId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PageResponse<DependencyScanResponse>> getScansForProject(
            @PathVariable UUID projectId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.debug("getScansForProject called with projectId={}", projectId);
        Pageable pageable = PageRequest.of(page, Math.min(size, AppConstants.MAX_PAGE_SIZE),
                Sort.by("createdAt").descending());
        return ResponseEntity.ok(dependencyService.getScansForProject(projectId, pageable));
    }

    /**
     * Retrieves the most recent dependency scan for a given project.
     *
     * <p>GET {@code /api/v1/dependencies/scans/project/{projectId}/latest}</p>
     *
     * @param projectId the UUID of the project
     * @return the latest dependency scan for the project
     */
    @GetMapping("/scans/project/{projectId}/latest")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DependencyScanResponse> getLatestScan(@PathVariable UUID projectId) {
        log.debug("getLatestScan called with projectId={}", projectId);
        return ResponseEntity.ok(dependencyService.getLatestScan(projectId));
    }

    /**
     * Adds a single vulnerability record to a dependency scan.
     *
     * <p>POST {@code /api/v1/dependencies/vulnerabilities}</p>
     *
     * <p>Side effect: logs a {@code VULNERABILITY_ADDED} audit entry.</p>
     *
     * @param request the vulnerability creation payload
     * @return the created vulnerability (HTTP 201)
     */
    @PostMapping("/vulnerabilities")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<VulnerabilityResponse> addVulnerability(@Valid @RequestBody CreateVulnerabilityRequest request) {
        log.debug("addVulnerability called");
        VulnerabilityResponse response = dependencyService.addVulnerability(request);
        auditLogService.log(SecurityUtils.getCurrentUserId(), null, "VULNERABILITY_ADDED", "VULNERABILITY", response.id(), null);
        return ResponseEntity.status(201).body(response);
    }

    /**
     * Adds multiple vulnerability records in a single batch operation.
     *
     * <p>POST {@code /api/v1/dependencies/vulnerabilities/batch}</p>
     *
     * <p>Side effect: logs a {@code VULNERABILITY_ADDED} audit entry for each vulnerability created.</p>
     *
     * @param requests the list of vulnerability creation payloads
     * @return list of created vulnerabilities (HTTP 201)
     */
    @PostMapping("/vulnerabilities/batch")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<VulnerabilityResponse>> addVulnerabilities(@Valid @RequestBody List<CreateVulnerabilityRequest> requests) {
        log.debug("addVulnerabilities called with batchSize={}", requests.size());
        List<VulnerabilityResponse> responses = dependencyService.addVulnerabilities(requests);
        responses.forEach(r -> auditLogService.log(SecurityUtils.getCurrentUserId(), null, "VULNERABILITY_ADDED", "VULNERABILITY", r.id(), null));
        return ResponseEntity.status(201).body(responses);
    }

    /**
     * Retrieves a paginated list of all vulnerabilities for a given dependency scan.
     *
     * <p>GET {@code /api/v1/dependencies/vulnerabilities/scan/{scanId}}</p>
     *
     * @param scanId the UUID of the dependency scan
     * @param page   zero-based page index (defaults to 0)
     * @param size   number of items per page (defaults to 20, capped at {@link AppConstants#MAX_PAGE_SIZE})
     * @return paginated list of vulnerabilities, sorted by creation date descending
     */
    @GetMapping("/vulnerabilities/scan/{scanId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PageResponse<VulnerabilityResponse>> getVulnerabilities(
            @PathVariable UUID scanId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.debug("getVulnerabilities called with scanId={}", scanId);
        Pageable pageable = PageRequest.of(page, Math.min(size, AppConstants.MAX_PAGE_SIZE),
                Sort.by("createdAt").descending());
        return ResponseEntity.ok(dependencyService.getVulnerabilities(scanId, pageable));
    }

    /**
     * Retrieves a paginated list of vulnerabilities for a scan filtered by severity level.
     *
     * <p>GET {@code /api/v1/dependencies/vulnerabilities/scan/{scanId}/severity/{severity}}</p>
     *
     * @param scanId   the UUID of the dependency scan
     * @param severity the severity level to filter by (e.g., CRITICAL, HIGH, MEDIUM, LOW)
     * @param page     zero-based page index (defaults to 0)
     * @param size     number of items per page (defaults to 20, capped at {@link AppConstants#MAX_PAGE_SIZE})
     * @return paginated list of vulnerabilities matching the given severity, sorted by creation date descending
     */
    @GetMapping("/vulnerabilities/scan/{scanId}/severity/{severity}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PageResponse<VulnerabilityResponse>> getVulnerabilitiesBySeverity(
            @PathVariable UUID scanId,
            @PathVariable Severity severity,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.debug("getVulnerabilitiesBySeverity called with scanId={}, severity={}", scanId, severity);
        Pageable pageable = PageRequest.of(page, Math.min(size, AppConstants.MAX_PAGE_SIZE),
                Sort.by("createdAt").descending());
        return ResponseEntity.ok(dependencyService.getVulnerabilitiesBySeverity(scanId, severity, pageable));
    }

    /**
     * Retrieves a paginated list of open (unresolved) vulnerabilities for a given scan.
     *
     * <p>GET {@code /api/v1/dependencies/vulnerabilities/scan/{scanId}/open}</p>
     *
     * @param scanId the UUID of the dependency scan
     * @param page   zero-based page index (defaults to 0)
     * @param size   number of items per page (defaults to 20, capped at {@link AppConstants#MAX_PAGE_SIZE})
     * @return paginated list of open vulnerabilities, sorted by creation date descending
     */
    @GetMapping("/vulnerabilities/scan/{scanId}/open")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PageResponse<VulnerabilityResponse>> getOpenVulnerabilities(
            @PathVariable UUID scanId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.debug("getOpenVulnerabilities called with scanId={}", scanId);
        Pageable pageable = PageRequest.of(page, Math.min(size, AppConstants.MAX_PAGE_SIZE),
                Sort.by("createdAt").descending());
        return ResponseEntity.ok(dependencyService.getOpenVulnerabilities(scanId, pageable));
    }

    /**
     * Updates the status of a specific vulnerability (e.g., from OPEN to RESOLVED or IGNORED).
     *
     * <p>PUT {@code /api/v1/dependencies/vulnerabilities/{vulnerabilityId}/status}</p>
     *
     * <p>Side effect: logs a {@code VULNERABILITY_STATUS_UPDATED} audit entry with the new status name.</p>
     *
     * @param vulnerabilityId the UUID of the vulnerability to update
     * @param status          the new vulnerability status
     * @return the updated vulnerability details
     */
    @PutMapping("/vulnerabilities/{vulnerabilityId}/status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<VulnerabilityResponse> updateVulnerabilityStatus(@PathVariable UUID vulnerabilityId,
                                                                            @RequestParam VulnerabilityStatus status) {
        log.debug("updateVulnerabilityStatus called with vulnerabilityId={}, status={}", vulnerabilityId, status);
        VulnerabilityResponse response = dependencyService.updateVulnerabilityStatus(vulnerabilityId, status);
        auditLogService.log(SecurityUtils.getCurrentUserId(), null, "VULNERABILITY_STATUS_UPDATED", "VULNERABILITY", vulnerabilityId, status.name());
        return ResponseEntity.ok(response);
    }
}
