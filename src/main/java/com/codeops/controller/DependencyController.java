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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/dependencies")
@RequiredArgsConstructor
@Tag(name = "Dependencies")
public class DependencyController {

    private final DependencyService dependencyService;
    private final AuditLogService auditLogService;

    @PostMapping("/scans")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DependencyScanResponse> createScan(@Valid @RequestBody CreateDependencyScanRequest request) {
        DependencyScanResponse response = dependencyService.createScan(request);
        auditLogService.log(SecurityUtils.getCurrentUserId(), null, "DEPENDENCY_SCAN_CREATED", "DEPENDENCY_SCAN", response.id(), null);
        return ResponseEntity.status(201).body(response);
    }

    @GetMapping("/scans/{scanId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DependencyScanResponse> getScan(@PathVariable UUID scanId) {
        return ResponseEntity.ok(dependencyService.getScan(scanId));
    }

    @GetMapping("/scans/project/{projectId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PageResponse<DependencyScanResponse>> getScansForProject(
            @PathVariable UUID projectId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, AppConstants.MAX_PAGE_SIZE),
                Sort.by("createdAt").descending());
        return ResponseEntity.ok(dependencyService.getScansForProject(projectId, pageable));
    }

    @GetMapping("/scans/project/{projectId}/latest")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DependencyScanResponse> getLatestScan(@PathVariable UUID projectId) {
        return ResponseEntity.ok(dependencyService.getLatestScan(projectId));
    }

    @PostMapping("/vulnerabilities")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<VulnerabilityResponse> addVulnerability(@Valid @RequestBody CreateVulnerabilityRequest request) {
        VulnerabilityResponse response = dependencyService.addVulnerability(request);
        auditLogService.log(SecurityUtils.getCurrentUserId(), null, "VULNERABILITY_ADDED", "VULNERABILITY", response.id(), null);
        return ResponseEntity.status(201).body(response);
    }

    @PostMapping("/vulnerabilities/batch")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<VulnerabilityResponse>> addVulnerabilities(@Valid @RequestBody List<CreateVulnerabilityRequest> requests) {
        List<VulnerabilityResponse> responses = dependencyService.addVulnerabilities(requests);
        responses.forEach(r -> auditLogService.log(SecurityUtils.getCurrentUserId(), null, "VULNERABILITY_ADDED", "VULNERABILITY", r.id(), null));
        return ResponseEntity.status(201).body(responses);
    }

    @GetMapping("/vulnerabilities/scan/{scanId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PageResponse<VulnerabilityResponse>> getVulnerabilities(
            @PathVariable UUID scanId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, AppConstants.MAX_PAGE_SIZE),
                Sort.by("createdAt").descending());
        return ResponseEntity.ok(dependencyService.getVulnerabilities(scanId, pageable));
    }

    @GetMapping("/vulnerabilities/scan/{scanId}/severity/{severity}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PageResponse<VulnerabilityResponse>> getVulnerabilitiesBySeverity(
            @PathVariable UUID scanId,
            @PathVariable Severity severity,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, AppConstants.MAX_PAGE_SIZE),
                Sort.by("createdAt").descending());
        return ResponseEntity.ok(dependencyService.getVulnerabilitiesBySeverity(scanId, severity, pageable));
    }

    @GetMapping("/vulnerabilities/scan/{scanId}/open")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PageResponse<VulnerabilityResponse>> getOpenVulnerabilities(
            @PathVariable UUID scanId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, AppConstants.MAX_PAGE_SIZE),
                Sort.by("createdAt").descending());
        return ResponseEntity.ok(dependencyService.getOpenVulnerabilities(scanId, pageable));
    }

    @PutMapping("/vulnerabilities/{vulnerabilityId}/status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<VulnerabilityResponse> updateVulnerabilityStatus(@PathVariable UUID vulnerabilityId,
                                                                            @RequestParam VulnerabilityStatus status) {
        VulnerabilityResponse response = dependencyService.updateVulnerabilityStatus(vulnerabilityId, status);
        auditLogService.log(SecurityUtils.getCurrentUserId(), null, "VULNERABILITY_STATUS_UPDATED", "VULNERABILITY", vulnerabilityId, status.name());
        return ResponseEntity.ok(response);
    }
}
