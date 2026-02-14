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
@RequestMapping("/api/v1/compliance")
@RequiredArgsConstructor
@Tag(name = "Compliance")
public class ComplianceController {

    private final ComplianceService complianceService;
    private final AuditLogService auditLogService;

    @PostMapping("/specs")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SpecificationResponse> createSpecification(@Valid @RequestBody CreateSpecificationRequest request) {
        SpecificationResponse response = complianceService.createSpecification(request);
        auditLogService.log(SecurityUtils.getCurrentUserId(), null, "SPECIFICATION_CREATED", "SPECIFICATION", response.id(), null);
        return ResponseEntity.status(201).body(response);
    }

    @GetMapping("/specs/job/{jobId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PageResponse<SpecificationResponse>> getSpecificationsForJob(
            @PathVariable UUID jobId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, AppConstants.MAX_PAGE_SIZE),
                Sort.by("createdAt").descending());
        return ResponseEntity.ok(complianceService.getSpecificationsForJob(jobId, pageable));
    }

    @PostMapping("/items")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ComplianceItemResponse> createComplianceItem(@Valid @RequestBody CreateComplianceItemRequest request) {
        ComplianceItemResponse response = complianceService.createComplianceItem(request);
        auditLogService.log(SecurityUtils.getCurrentUserId(), null, "COMPLIANCE_ITEM_CREATED", "COMPLIANCE_ITEM", response.id(), null);
        return ResponseEntity.status(201).body(response);
    }

    @PostMapping("/items/batch")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ComplianceItemResponse>> createComplianceItems(@Valid @RequestBody List<CreateComplianceItemRequest> requests) {
        List<ComplianceItemResponse> responses = complianceService.createComplianceItems(requests);
        responses.forEach(r -> auditLogService.log(SecurityUtils.getCurrentUserId(), null, "COMPLIANCE_ITEM_CREATED", "COMPLIANCE_ITEM", r.id(), null));
        return ResponseEntity.status(201).body(responses);
    }

    @GetMapping("/items/job/{jobId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PageResponse<ComplianceItemResponse>> getComplianceItemsForJob(
            @PathVariable UUID jobId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, AppConstants.MAX_PAGE_SIZE),
                Sort.by("createdAt").descending());
        return ResponseEntity.ok(complianceService.getComplianceItemsForJob(jobId, pageable));
    }

    @GetMapping("/items/job/{jobId}/status/{status}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PageResponse<ComplianceItemResponse>> getComplianceItemsByStatus(
            @PathVariable UUID jobId,
            @PathVariable ComplianceStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, AppConstants.MAX_PAGE_SIZE),
                Sort.by("createdAt").descending());
        return ResponseEntity.ok(complianceService.getComplianceItemsByStatus(jobId, status, pageable));
    }

    @GetMapping("/summary/job/{jobId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getComplianceSummary(@PathVariable UUID jobId) {
        return ResponseEntity.ok(complianceService.getComplianceSummary(jobId));
    }
}
