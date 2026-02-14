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

/**
 * REST controller for technical debt tracking and management.
 *
 * <p>Tech debt items represent code quality issues, architectural shortcomings,
 * or deferred maintenance identified in projects. Items can be categorized,
 * filtered by status or category, and managed through a lifecycle. All endpoints
 * require authentication.</p>
 *
 * <p>Status updates and deletions record an audit log entry via {@link AuditLogService}.</p>
 *
 * @see TechDebtService
 * @see AuditLogService
 */
@RestController
@RequestMapping("/api/v1/tech-debt")
@RequiredArgsConstructor
@Tag(name = "Tech Debt")
public class TechDebtController {

    private final TechDebtService techDebtService;
    private final AuditLogService auditLogService;

    /**
     * Creates a single tech debt item.
     *
     * <p>POST /api/v1/tech-debt</p>
     *
     * <p>Requires authentication.</p>
     *
     * @param request the tech debt item creation payload including title, category, and project association
     * @return the created tech debt item with HTTP 201 status
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TechDebtItemResponse> createTechDebtItem(@Valid @RequestBody CreateTechDebtItemRequest request) {
        return ResponseEntity.status(201).body(techDebtService.createTechDebtItem(request));
    }

    /**
     * Creates multiple tech debt items in a single batch operation.
     *
     * <p>POST /api/v1/tech-debt/batch</p>
     *
     * <p>Requires authentication.</p>
     *
     * @param requests the list of tech debt item creation payloads
     * @return the list of created tech debt items with HTTP 201 status
     */
    @PostMapping("/batch")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<TechDebtItemResponse>> createTechDebtItems(@Valid @RequestBody List<CreateTechDebtItemRequest> requests) {
        return ResponseEntity.status(201).body(techDebtService.createTechDebtItems(requests));
    }

    /**
     * Retrieves a single tech debt item by its identifier.
     *
     * <p>GET /api/v1/tech-debt/{itemId}</p>
     *
     * <p>Requires authentication.</p>
     *
     * @param itemId the UUID of the tech debt item to retrieve
     * @return the tech debt item details
     */
    @GetMapping("/{itemId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TechDebtItemResponse> getTechDebtItem(@PathVariable UUID itemId) {
        return ResponseEntity.ok(techDebtService.getTechDebtItem(itemId));
    }

    /**
     * Retrieves a paginated list of all tech debt items for a project.
     *
     * <p>GET /api/v1/tech-debt/project/{projectId}?page={page}&amp;size={size}</p>
     *
     * <p>Requires authentication. Results are sorted by creation date descending.
     * Page size is capped at {@link AppConstants#MAX_PAGE_SIZE}.</p>
     *
     * @param projectId the UUID of the project whose tech debt items to list
     * @param page      zero-based page index (defaults to 0)
     * @param size      number of items per page (defaults to 20, capped at MAX_PAGE_SIZE)
     * @return a paginated list of tech debt items for the project
     */
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

    /**
     * Retrieves a paginated list of tech debt items for a project filtered by status.
     *
     * <p>GET /api/v1/tech-debt/project/{projectId}/status/{status}?page={page}&amp;size={size}</p>
     *
     * <p>Requires authentication. Results are sorted by creation date descending.
     * Page size is capped at {@link AppConstants#MAX_PAGE_SIZE}.</p>
     *
     * @param projectId the UUID of the project
     * @param status    the debt status to filter by (e.g., OPEN, IN_PROGRESS, RESOLVED)
     * @param page      zero-based page index (defaults to 0)
     * @param size      number of items per page (defaults to 20, capped at MAX_PAGE_SIZE)
     * @return a paginated list of tech debt items matching the specified status
     */
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

    /**
     * Retrieves a paginated list of tech debt items for a project filtered by category.
     *
     * <p>GET /api/v1/tech-debt/project/{projectId}/category/{category}?page={page}&amp;size={size}</p>
     *
     * <p>Requires authentication. Results are sorted by creation date descending.
     * Page size is capped at {@link AppConstants#MAX_PAGE_SIZE}.</p>
     *
     * @param projectId the UUID of the project
     * @param category  the debt category to filter by (e.g., CODE_SMELL, ARCHITECTURE, DEPENDENCY)
     * @param page      zero-based page index (defaults to 0)
     * @param size      number of items per page (defaults to 20, capped at MAX_PAGE_SIZE)
     * @return a paginated list of tech debt items matching the specified category
     */
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

    /**
     * Updates the status of a tech debt item.
     *
     * <p>PUT /api/v1/tech-debt/{itemId}/status</p>
     *
     * <p>Requires authentication. Logs a TECH_DEBT_STATUS_UPDATED audit event.</p>
     *
     * @param itemId  the UUID of the tech debt item to update
     * @param request the status update payload containing the new status
     * @return the updated tech debt item
     */
    @PutMapping("/{itemId}/status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TechDebtItemResponse> updateTechDebtStatus(@PathVariable UUID itemId,
                                                                      @Valid @RequestBody UpdateTechDebtStatusRequest request) {
        TechDebtItemResponse response = techDebtService.updateTechDebtStatus(itemId, request);
        auditLogService.log(SecurityUtils.getCurrentUserId(), null, "TECH_DEBT_STATUS_UPDATED", "TECH_DEBT", itemId, null);
        return ResponseEntity.ok(response);
    }

    /**
     * Permanently deletes a tech debt item.
     *
     * <p>DELETE /api/v1/tech-debt/{itemId}</p>
     *
     * <p>Requires authentication. Logs a TECH_DEBT_DELETED audit event.
     * Returns HTTP 204 No Content on success.</p>
     *
     * @param itemId the UUID of the tech debt item to delete
     * @return empty response with HTTP 204 status
     */
    @DeleteMapping("/{itemId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteTechDebtItem(@PathVariable UUID itemId) {
        techDebtService.deleteTechDebtItem(itemId);
        auditLogService.log(SecurityUtils.getCurrentUserId(), null, "TECH_DEBT_DELETED", "TECH_DEBT", itemId, null);
        return ResponseEntity.noContent().build();
    }

    /**
     * Retrieves an aggregated summary of tech debt for a project.
     *
     * <p>GET /api/v1/tech-debt/project/{projectId}/summary</p>
     *
     * <p>Requires authentication. Returns counts and breakdowns by status
     * and category for the project's tech debt items.</p>
     *
     * @param projectId the UUID of the project to summarize
     * @return a map containing summary statistics (counts by status, category, etc.)
     */
    @GetMapping("/project/{projectId}/summary")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getDebtSummary(@PathVariable UUID projectId) {
        return ResponseEntity.ok(techDebtService.getDebtSummary(projectId));
    }
}
