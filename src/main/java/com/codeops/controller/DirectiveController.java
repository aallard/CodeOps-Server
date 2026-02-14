package com.codeops.controller;

import com.codeops.dto.request.AssignDirectiveRequest;
import com.codeops.dto.request.CreateDirectiveRequest;
import com.codeops.dto.request.UpdateDirectiveRequest;
import com.codeops.dto.response.DirectiveResponse;
import com.codeops.dto.response.ProjectDirectiveResponse;
import com.codeops.security.SecurityUtils;
import com.codeops.service.AuditLogService;
import com.codeops.service.DirectiveService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for directive management operations including CRUD on directives,
 * assigning directives to projects, and toggling their enabled state.
 *
 * <p>Directives are reusable rules or instructions that can be assigned to projects
 * via a project-directive join table with an enabled flag. All endpoints require
 * authentication.</p>
 *
 * @see DirectiveService
 * @see AuditLogService
 */
@RestController
@RequestMapping("/api/v1/directives")
@RequiredArgsConstructor
@Tag(name = "Directives")
public class DirectiveController {

    private final DirectiveService directiveService;
    private final AuditLogService auditLogService;

    /**
     * Creates a new directive.
     *
     * <p>POST {@code /api/v1/directives}</p>
     *
     * <p>Side effect: logs a {@code DIRECTIVE_CREATED} audit entry scoped to the directive's team.</p>
     *
     * @param request the directive creation payload
     * @return the created directive (HTTP 201)
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DirectiveResponse> createDirective(@Valid @RequestBody CreateDirectiveRequest request) {
        DirectiveResponse response = directiveService.createDirective(request);
        auditLogService.log(SecurityUtils.getCurrentUserId(), response.teamId(), "DIRECTIVE_CREATED", "DIRECTIVE", response.id(), "");
        return ResponseEntity.status(201).body(response);
    }

    /**
     * Retrieves a single directive by its identifier.
     *
     * <p>GET {@code /api/v1/directives/{directiveId}}</p>
     *
     * @param directiveId the UUID of the directive to retrieve
     * @return the directive details
     */
    @GetMapping("/{directiveId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DirectiveResponse> getDirective(@PathVariable UUID directiveId) {
        return ResponseEntity.ok(directiveService.getDirective(directiveId));
    }

    /**
     * Retrieves all directives belonging to a specific team.
     *
     * <p>GET {@code /api/v1/directives/team/{teamId}}</p>
     *
     * @param teamId the UUID of the team
     * @return list of directives owned by the team
     */
    @GetMapping("/team/{teamId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<DirectiveResponse>> getDirectivesForTeam(@PathVariable UUID teamId) {
        return ResponseEntity.ok(directiveService.getDirectivesForTeam(teamId));
    }

    /**
     * Retrieves all directives assigned to a specific project (both enabled and disabled).
     *
     * <p>GET {@code /api/v1/directives/project/{projectId}}</p>
     *
     * @param projectId the UUID of the project
     * @return list of directives assigned to the project
     */
    @GetMapping("/project/{projectId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<DirectiveResponse>> getDirectivesForProject(@PathVariable UUID projectId) {
        return ResponseEntity.ok(directiveService.getDirectivesForProject(projectId));
    }

    /**
     * Updates an existing directive's properties.
     *
     * <p>PUT {@code /api/v1/directives/{directiveId}}</p>
     *
     * <p>Side effect: logs a {@code DIRECTIVE_UPDATED} audit entry scoped to the directive's team.</p>
     *
     * @param directiveId the UUID of the directive to update
     * @param request     the update payload containing the new directive properties
     * @return the updated directive details
     */
    @PutMapping("/{directiveId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DirectiveResponse> updateDirective(@PathVariable UUID directiveId,
                                                              @Valid @RequestBody UpdateDirectiveRequest request) {
        DirectiveResponse response = directiveService.updateDirective(directiveId, request);
        auditLogService.log(SecurityUtils.getCurrentUserId(), response.teamId(), "DIRECTIVE_UPDATED", "DIRECTIVE", directiveId, "");
        return ResponseEntity.ok(response);
    }

    /**
     * Deletes a directive by its identifier.
     *
     * <p>DELETE {@code /api/v1/directives/{directiveId}}</p>
     *
     * <p>Side effect: logs a {@code DIRECTIVE_DELETED} audit entry scoped to the directive's team.</p>
     *
     * @param directiveId the UUID of the directive to delete
     * @return HTTP 204 No Content on successful deletion
     */
    @DeleteMapping("/{directiveId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteDirective(@PathVariable UUID directiveId) {
        DirectiveResponse directive = directiveService.getDirective(directiveId);
        directiveService.deleteDirective(directiveId);
        auditLogService.log(SecurityUtils.getCurrentUserId(), directive.teamId(), "DIRECTIVE_DELETED", "DIRECTIVE", directiveId, "");
        return ResponseEntity.noContent().build();
    }

    /**
     * Assigns a directive to a project via the project-directives join table.
     *
     * <p>POST {@code /api/v1/directives/assign}</p>
     *
     * <p>Side effect: logs a {@code DIRECTIVE_ASSIGNED} audit entry scoped to the directive's team.</p>
     *
     * @param request the assignment payload containing the directive and project identifiers
     * @return the created project-directive association (HTTP 201)
     */
    @PostMapping("/assign")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProjectDirectiveResponse> assignToProject(@Valid @RequestBody AssignDirectiveRequest request) {
        ProjectDirectiveResponse response = directiveService.assignToProject(request);
        DirectiveResponse directive = directiveService.getDirective(request.directiveId());
        auditLogService.log(SecurityUtils.getCurrentUserId(), directive.teamId(), "DIRECTIVE_ASSIGNED", "DIRECTIVE", request.directiveId(), "");
        return ResponseEntity.status(201).body(response);
    }

    /**
     * Removes a directive assignment from a project.
     *
     * <p>DELETE {@code /api/v1/directives/project/{projectId}/directive/{directiveId}}</p>
     *
     * <p>Side effect: logs a {@code DIRECTIVE_REMOVED} audit entry scoped to the directive's team.</p>
     *
     * @param projectId   the UUID of the project
     * @param directiveId the UUID of the directive to remove from the project
     * @return HTTP 204 No Content on successful removal
     */
    @DeleteMapping("/project/{projectId}/directive/{directiveId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> removeFromProject(@PathVariable UUID projectId, @PathVariable UUID directiveId) {
        DirectiveResponse directive = directiveService.getDirective(directiveId);
        directiveService.removeFromProject(projectId, directiveId);
        auditLogService.log(SecurityUtils.getCurrentUserId(), directive.teamId(), "DIRECTIVE_REMOVED", "DIRECTIVE", directiveId, "");
        return ResponseEntity.noContent().build();
    }

    /**
     * Retrieves all project-directive associations for a given project, including
     * the enabled/disabled state of each assignment.
     *
     * <p>GET {@code /api/v1/directives/project/{projectId}/assignments}</p>
     *
     * @param projectId the UUID of the project
     * @return list of project-directive association responses
     */
    @GetMapping("/project/{projectId}/assignments")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ProjectDirectiveResponse>> getProjectDirectives(@PathVariable UUID projectId) {
        return ResponseEntity.ok(directiveService.getProjectDirectives(projectId));
    }

    /**
     * Retrieves only the enabled directives for a given project.
     *
     * <p>GET {@code /api/v1/directives/project/{projectId}/enabled}</p>
     *
     * @param projectId the UUID of the project
     * @return list of directives that are currently enabled for the project
     */
    @GetMapping("/project/{projectId}/enabled")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<DirectiveResponse>> getEnabledDirectives(@PathVariable UUID projectId) {
        return ResponseEntity.ok(directiveService.getEnabledDirectivesForProject(projectId));
    }

    /**
     * Toggles the enabled/disabled state of a directive assignment for a project.
     *
     * <p>PUT {@code /api/v1/directives/project/{projectId}/directive/{directiveId}/toggle}</p>
     *
     * @param projectId   the UUID of the project
     * @param directiveId the UUID of the directive
     * @param enabled     {@code true} to enable, {@code false} to disable the directive for the project
     * @return the updated project-directive association
     */
    @PutMapping("/project/{projectId}/directive/{directiveId}/toggle")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProjectDirectiveResponse> toggleDirective(@PathVariable UUID projectId,
                                                                     @PathVariable UUID directiveId,
                                                                     @RequestParam boolean enabled) {
        return ResponseEntity.ok(directiveService.toggleProjectDirective(projectId, directiveId, enabled));
    }
}
