package com.codeops.controller;

import com.codeops.dto.request.CreateDependencyScanRequest;
import com.codeops.dto.request.CreateVulnerabilityRequest;
import com.codeops.dto.response.DependencyScanResponse;
import com.codeops.dto.response.VulnerabilityResponse;
import com.codeops.entity.enums.Severity;
import com.codeops.entity.enums.VulnerabilityStatus;
import com.codeops.service.DependencyService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

    @PostMapping("/scans")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DependencyScanResponse> createScan(@Valid @RequestBody CreateDependencyScanRequest request) {
        return ResponseEntity.status(201).body(dependencyService.createScan(request));
    }

    @GetMapping("/scans/{scanId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DependencyScanResponse> getScan(@PathVariable UUID scanId) {
        return ResponseEntity.ok(dependencyService.getScan(scanId));
    }

    @GetMapping("/scans/project/{projectId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<DependencyScanResponse>> getScansForProject(@PathVariable UUID projectId) {
        return ResponseEntity.ok(dependencyService.getScansForProject(projectId));
    }

    @GetMapping("/scans/project/{projectId}/latest")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DependencyScanResponse> getLatestScan(@PathVariable UUID projectId) {
        return ResponseEntity.ok(dependencyService.getLatestScan(projectId));
    }

    @PostMapping("/vulnerabilities")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<VulnerabilityResponse> addVulnerability(@Valid @RequestBody CreateVulnerabilityRequest request) {
        return ResponseEntity.status(201).body(dependencyService.addVulnerability(request));
    }

    @PostMapping("/vulnerabilities/batch")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<VulnerabilityResponse>> addVulnerabilities(@Valid @RequestBody List<CreateVulnerabilityRequest> requests) {
        return ResponseEntity.status(201).body(dependencyService.addVulnerabilities(requests));
    }

    @GetMapping("/vulnerabilities/scan/{scanId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<VulnerabilityResponse>> getVulnerabilities(@PathVariable UUID scanId) {
        return ResponseEntity.ok(dependencyService.getVulnerabilities(scanId));
    }

    @GetMapping("/vulnerabilities/scan/{scanId}/severity/{severity}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<VulnerabilityResponse>> getVulnerabilitiesBySeverity(@PathVariable UUID scanId,
                                                                                     @PathVariable Severity severity) {
        return ResponseEntity.ok(dependencyService.getVulnerabilitiesBySeverity(scanId, severity));
    }

    @GetMapping("/vulnerabilities/scan/{scanId}/open")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<VulnerabilityResponse>> getOpenVulnerabilities(@PathVariable UUID scanId) {
        return ResponseEntity.ok(dependencyService.getOpenVulnerabilities(scanId));
    }

    @PutMapping("/vulnerabilities/{vulnerabilityId}/status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<VulnerabilityResponse> updateVulnerabilityStatus(@PathVariable UUID vulnerabilityId,
                                                                            @RequestParam VulnerabilityStatus status) {
        return ResponseEntity.ok(dependencyService.updateVulnerabilityStatus(vulnerabilityId, status));
    }
}
