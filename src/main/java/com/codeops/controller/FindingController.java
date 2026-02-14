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

@RestController
@RequestMapping("/api/v1/findings")
@RequiredArgsConstructor
@Tag(name = "Findings")
public class FindingController {

    private final FindingService findingService;
    private final AuditLogService auditLogService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<FindingResponse> createFinding(@Valid @RequestBody CreateFindingRequest request) {
        FindingResponse response = findingService.createFinding(request);
        auditLogService.log(SecurityUtils.getCurrentUserId(), null, "FINDING_CREATED", "FINDING", response.id(), null);
        return ResponseEntity.status(201).body(response);
    }

    @PostMapping("/batch")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<FindingResponse>> createFindings(
            @Valid @RequestBody List<CreateFindingRequest> requests) {
        List<FindingResponse> responses = findingService.createFindings(requests);
        responses.forEach(r -> auditLogService.log(SecurityUtils.getCurrentUserId(), null, "FINDING_CREATED", "FINDING", r.id(), null));
        return ResponseEntity.status(201).body(responses);
    }

    @GetMapping("/{findingId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<FindingResponse> getFinding(@PathVariable UUID findingId) {
        return ResponseEntity.ok(findingService.getFinding(findingId));
    }

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

    @GetMapping("/job/{jobId}/counts")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<Severity, Long>> getSeverityCounts(@PathVariable UUID jobId) {
        return ResponseEntity.ok(findingService.countFindingsBySeverity(jobId));
    }

    @PutMapping("/{findingId}/status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<FindingResponse> updateFindingStatus(
            @PathVariable UUID findingId,
            @Valid @RequestBody UpdateFindingStatusRequest request) {
        FindingResponse response = findingService.updateFindingStatus(findingId, request);
        auditLogService.log(SecurityUtils.getCurrentUserId(), null, "FINDING_STATUS_UPDATED", "FINDING", findingId, request.status().name());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/bulk-status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<FindingResponse>> bulkUpdateStatus(
            @Valid @RequestBody BulkUpdateFindingsRequest request) {
        List<FindingResponse> responses = findingService.bulkUpdateFindingStatus(request);
        responses.forEach(r -> auditLogService.log(SecurityUtils.getCurrentUserId(), null, "FINDING_STATUS_UPDATED", "FINDING", r.id(), request.status().name()));
        return ResponseEntity.ok(responses);
    }
}
