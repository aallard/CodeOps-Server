package com.codeops.controller;

import com.codeops.config.AppConstants;
import com.codeops.dto.request.CreateTechDebtItemRequest;
import com.codeops.dto.request.UpdateTechDebtStatusRequest;
import com.codeops.dto.response.PageResponse;
import com.codeops.dto.response.TechDebtItemResponse;
import com.codeops.entity.enums.DebtCategory;
import com.codeops.entity.enums.DebtStatus;
import com.codeops.security.SecurityUtils;
import com.codeops.service.AuditLogService;
import com.codeops.service.TechDebtService;
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
@RequestMapping("/api/v1/tech-debt")
@RequiredArgsConstructor
@Tag(name = "Tech Debt")
public class TechDebtController {

    private final TechDebtService techDebtService;
    private final AuditLogService auditLogService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TechDebtItemResponse> createTechDebtItem(@Valid @RequestBody CreateTechDebtItemRequest request) {
        return ResponseEntity.status(201).body(techDebtService.createTechDebtItem(request));
    }

    @PostMapping("/batch")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<TechDebtItemResponse>> createTechDebtItems(@Valid @RequestBody List<CreateTechDebtItemRequest> requests) {
        return ResponseEntity.status(201).body(techDebtService.createTechDebtItems(requests));
    }

    @GetMapping("/{itemId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TechDebtItemResponse> getTechDebtItem(@PathVariable UUID itemId) {
        return ResponseEntity.ok(techDebtService.getTechDebtItem(itemId));
    }

    @GetMapping("/project/{projectId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PageResponse<TechDebtItemResponse>> getTechDebtForProject(
            @PathVariable UUID projectId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, AppConstants.MAX_PAGE_SIZE),
                Sort.by("createdAt").descending());
        return ResponseEntity.ok(techDebtService.getTechDebtForProject(projectId, pageable));
    }

    @GetMapping("/project/{projectId}/status/{status}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PageResponse<TechDebtItemResponse>> getTechDebtByStatus(
            @PathVariable UUID projectId,
            @PathVariable DebtStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, AppConstants.MAX_PAGE_SIZE),
                Sort.by("createdAt").descending());
        return ResponseEntity.ok(techDebtService.getTechDebtByStatus(projectId, status, pageable));
    }

    @GetMapping("/project/{projectId}/category/{category}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PageResponse<TechDebtItemResponse>> getTechDebtByCategory(
            @PathVariable UUID projectId,
            @PathVariable DebtCategory category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, AppConstants.MAX_PAGE_SIZE),
                Sort.by("createdAt").descending());
        return ResponseEntity.ok(techDebtService.getTechDebtByCategory(projectId, category, pageable));
    }

    @PutMapping("/{itemId}/status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TechDebtItemResponse> updateTechDebtStatus(@PathVariable UUID itemId,
                                                                      @Valid @RequestBody UpdateTechDebtStatusRequest request) {
        TechDebtItemResponse response = techDebtService.updateTechDebtStatus(itemId, request);
        auditLogService.log(SecurityUtils.getCurrentUserId(), null, "TECH_DEBT_STATUS_UPDATED", "TECH_DEBT", itemId, null);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{itemId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteTechDebtItem(@PathVariable UUID itemId) {
        techDebtService.deleteTechDebtItem(itemId);
        auditLogService.log(SecurityUtils.getCurrentUserId(), null, "TECH_DEBT_DELETED", "TECH_DEBT", itemId, null);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/project/{projectId}/summary")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getDebtSummary(@PathVariable UUID projectId) {
        return ResponseEntity.ok(techDebtService.getDebtSummary(projectId));
    }
}
