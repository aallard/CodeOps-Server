package com.codeops.controller;

import com.codeops.config.AppConstants;
import com.codeops.dto.request.CreateProjectRequest;
import com.codeops.dto.request.UpdateProjectRequest;
import com.codeops.dto.response.PageResponse;
import com.codeops.dto.response.ProjectResponse;
import com.codeops.security.SecurityUtils;
import com.codeops.service.AuditLogService;
import com.codeops.service.ProjectService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for project management operations.
 *
 * <p>Projects represent codebases or repositories being monitored by CodeOps.
 * Each project belongs to a team. All endpoints require authentication, and
 * team membership is verified at the service layer.</p>
 *
 * <p>Mutating operations (create, update, archive, unarchive, delete) record
 * an audit log entry via {@link AuditLogService}.</p>
 *
 * @see ProjectService
 * @see AuditLogService
 */
@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
@Tag(name = "Projects")
public class ProjectController {

    private final ProjectService projectService;
    private final AuditLogService auditLogService;

    /**
     * Creates a new project within a team.
     *
     * <p>POST /api/v1/projects/{teamId}</p>
     *
     * <p>Requires authentication. Logs a PROJECT_CREATED audit event.</p>
     *
     * @param teamId  the UUID of the team the project will belong to
     * @param request the project creation payload including name and configuration
     * @return the created project with HTTP 201 status
     */
    @PostMapping("/{teamId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProjectResponse> createProject(@PathVariable UUID teamId,
                                                         @Valid @RequestBody CreateProjectRequest request) {
        ProjectResponse response = projectService.createProject(teamId, request);
        auditLogService.log(SecurityUtils.getCurrentUserId(), teamId, "PROJECT_CREATED", "PROJECT", response.id(), "");
        return ResponseEntity.status(201).body(response);
    }

    /**
     * Retrieves a paginated list of projects for a team.
     *
     * <p>GET /api/v1/projects/team/{teamId}?includeArchived={includeArchived}&amp;page={page}&amp;size={size}</p>
     *
     * <p>Requires authentication. Results are sorted by creation date descending.
     * Archived projects are excluded by default unless {@code includeArchived} is true.
     * Page size is capped at {@link AppConstants#MAX_PAGE_SIZE}.</p>
     *
     * @param teamId          the UUID of the team whose projects to list
     * @param includeArchived whether to include archived projects (defaults to false)
     * @param page            zero-based page index (defaults to 0)
     * @param size            number of items per page (defaults to 20, capped at MAX_PAGE_SIZE)
     * @return a paginated list of projects for the team
     */
    @GetMapping("/team/{teamId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PageResponse<ProjectResponse>> getProjects(
            @PathVariable UUID teamId,
            @RequestParam(defaultValue = "false") boolean includeArchived,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, AppConstants.MAX_PAGE_SIZE),
                Sort.by("createdAt").descending());
        return ResponseEntity.ok(projectService.getAllProjectsForTeam(teamId, includeArchived, pageable));
    }

    /**
     * Retrieves a project by its identifier.
     *
     * <p>GET /api/v1/projects/{projectId}</p>
     *
     * <p>Requires authentication.</p>
     *
     * @param projectId the UUID of the project to retrieve
     * @return the project details
     */
    @GetMapping("/{projectId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProjectResponse> getProject(@PathVariable UUID projectId) {
        return ResponseEntity.ok(projectService.getProject(projectId));
    }

    /**
     * Updates an existing project.
     *
     * <p>PUT /api/v1/projects/{projectId}</p>
     *
     * <p>Requires authentication. Logs a PROJECT_UPDATED audit event.</p>
     *
     * @param projectId the UUID of the project to update
     * @param request   the update payload with fields to modify
     * @return the updated project
     */
    @PutMapping("/{projectId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProjectResponse> updateProject(@PathVariable UUID projectId,
                                                         @Valid @RequestBody UpdateProjectRequest request) {
        ProjectResponse response = projectService.updateProject(projectId, request);
        auditLogService.log(SecurityUtils.getCurrentUserId(), response.teamId(), "PROJECT_UPDATED", "PROJECT", projectId, "");
        return ResponseEntity.ok(response);
    }

    /**
     * Archives a project, hiding it from default project listings.
     *
     * <p>PUT /api/v1/projects/{projectId}/archive</p>
     *
     * <p>Requires authentication. Logs a PROJECT_ARCHIVED audit event.</p>
     *
     * @param projectId the UUID of the project to archive
     * @return empty response with HTTP 200 status
     */
    @PutMapping("/{projectId}/archive")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> archiveProject(@PathVariable UUID projectId) {
        ProjectResponse project = projectService.getProject(projectId);
        projectService.archiveProject(projectId);
        auditLogService.log(SecurityUtils.getCurrentUserId(), project.teamId(), "PROJECT_ARCHIVED", "PROJECT", projectId, "");
        return ResponseEntity.ok().build();
    }

    /**
     * Unarchives a previously archived project, restoring it to default listings.
     *
     * <p>PUT /api/v1/projects/{projectId}/unarchive</p>
     *
     * <p>Requires authentication. Logs a PROJECT_UNARCHIVED audit event.</p>
     *
     * @param projectId the UUID of the project to unarchive
     * @return empty response with HTTP 200 status
     */
    @PutMapping("/{projectId}/unarchive")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> unarchiveProject(@PathVariable UUID projectId) {
        ProjectResponse project = projectService.getProject(projectId);
        projectService.unarchiveProject(projectId);
        auditLogService.log(SecurityUtils.getCurrentUserId(), project.teamId(), "PROJECT_UNARCHIVED", "PROJECT", projectId, "");
        return ResponseEntity.ok().build();
    }

    /**
     * Permanently deletes a project.
     *
     * <p>DELETE /api/v1/projects/{projectId}</p>
     *
     * <p>Requires authentication. Logs a PROJECT_DELETED audit event.
     * Returns HTTP 204 No Content on success.</p>
     *
     * @param projectId the UUID of the project to delete
     * @return empty response with HTTP 204 status
     */
    @DeleteMapping("/{projectId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteProject(@PathVariable UUID projectId) {
        ProjectResponse project = projectService.getProject(projectId);
        projectService.deleteProject(projectId);
        auditLogService.log(SecurityUtils.getCurrentUserId(), project.teamId(), "PROJECT_DELETED", "PROJECT", projectId, "");
        return ResponseEntity.noContent().build();
    }
}
