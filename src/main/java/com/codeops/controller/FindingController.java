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

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<FindingResponse> createFinding(@Valid @RequestBody CreateFindingRequest request) {
        return ResponseEntity.status(201).body(findingService.createFinding(request));
    }

    @PostMapping("/batch")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<FindingResponse>> createFindings(
            @Valid @RequestBody List<CreateFindingRequest> requests) {
        return ResponseEntity.status(201).body(findingService.createFindings(requests));
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
    public ResponseEntity<List<FindingResponse>> getFindingsBySeverity(
            @PathVariable UUID jobId,
            @PathVariable Severity severity) {
        return ResponseEntity.ok(findingService.getFindingsByJobAndSeverity(jobId, severity));
    }

    @GetMapping("/job/{jobId}/agent/{agentType}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<FindingResponse>> getFindingsByAgent(
            @PathVariable UUID jobId,
            @PathVariable AgentType agentType) {
        return ResponseEntity.ok(findingService.getFindingsByJobAndAgent(jobId, agentType));
    }

    @GetMapping("/job/{jobId}/status/{status}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<FindingResponse>> getFindingsByStatus(
            @PathVariable UUID jobId,
            @PathVariable FindingStatus status) {
        return ResponseEntity.ok(findingService.getFindingsByJobAndStatus(jobId, status));
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
        return ResponseEntity.ok(findingService.updateFindingStatus(findingId, request));
    }

    @PutMapping("/bulk-status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<FindingResponse>> bulkUpdateStatus(
            @Valid @RequestBody BulkUpdateFindingsRequest request) {
        return ResponseEntity.ok(findingService.bulkUpdateFindingStatus(request));
    }
}
