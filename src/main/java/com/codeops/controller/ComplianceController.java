package com.codeops.controller;

import com.codeops.dto.request.CreateComplianceItemRequest;
import com.codeops.dto.request.CreateSpecificationRequest;
import com.codeops.dto.response.ComplianceItemResponse;
import com.codeops.dto.response.SpecificationResponse;
import com.codeops.entity.enums.ComplianceStatus;
import com.codeops.service.ComplianceService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

    @PostMapping("/specs")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SpecificationResponse> createSpecification(@Valid @RequestBody CreateSpecificationRequest request) {
        return ResponseEntity.status(201).body(complianceService.createSpecification(request));
    }

    @GetMapping("/specs/job/{jobId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<SpecificationResponse>> getSpecificationsForJob(@PathVariable UUID jobId) {
        return ResponseEntity.ok(complianceService.getSpecificationsForJob(jobId));
    }

    @PostMapping("/items")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ComplianceItemResponse> createComplianceItem(@Valid @RequestBody CreateComplianceItemRequest request) {
        return ResponseEntity.status(201).body(complianceService.createComplianceItem(request));
    }

    @PostMapping("/items/batch")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ComplianceItemResponse>> createComplianceItems(@Valid @RequestBody List<CreateComplianceItemRequest> requests) {
        return ResponseEntity.status(201).body(complianceService.createComplianceItems(requests));
    }

    @GetMapping("/items/job/{jobId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ComplianceItemResponse>> getComplianceItemsForJob(@PathVariable UUID jobId) {
        return ResponseEntity.ok(complianceService.getComplianceItemsForJob(jobId));
    }

    @GetMapping("/items/job/{jobId}/status/{status}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ComplianceItemResponse>> getComplianceItemsByStatus(@PathVariable UUID jobId,
                                                                                    @PathVariable ComplianceStatus status) {
        return ResponseEntity.ok(complianceService.getComplianceItemsByStatus(jobId, status));
    }

    @GetMapping("/summary/job/{jobId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getComplianceSummary(@PathVariable UUID jobId) {
        return ResponseEntity.ok(complianceService.getComplianceSummary(jobId));
    }
}
