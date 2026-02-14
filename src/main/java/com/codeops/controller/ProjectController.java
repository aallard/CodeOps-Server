package com.codeops.controller;

import com.codeops.dto.request.CreateProjectRequest;
import com.codeops.dto.request.UpdateProjectRequest;
import com.codeops.dto.response.ProjectResponse;
import com.codeops.security.SecurityUtils;
import com.codeops.service.AuditLogService;
import com.codeops.service.ProjectService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
@Tag(name = "Projects")
public class ProjectController {

    private final ProjectService projectService;
    private final AuditLogService auditLogService;

    @PostMapping("/{teamId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProjectResponse> createProject(@PathVariable UUID teamId,
                                                         @Valid @RequestBody CreateProjectRequest request) {
        ProjectResponse response = projectService.createProject(teamId, request);
        auditLogService.log(SecurityUtils.getCurrentUserId(), teamId, "PROJECT_CREATED", "PROJECT", response.id(), null);
        return ResponseEntity.status(201).body(response);
    }

    @GetMapping("/team/{teamId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ProjectResponse>> getProjects(
            @PathVariable UUID teamId,
            @RequestParam(defaultValue = "false") boolean includeArchived) {
        return ResponseEntity.ok(projectService.getAllProjectsForTeam(teamId, includeArchived));
    }

    @GetMapping("/{projectId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProjectResponse> getProject(@PathVariable UUID projectId) {
        return ResponseEntity.ok(projectService.getProject(projectId));
    }

    @PutMapping("/{projectId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProjectResponse> updateProject(@PathVariable UUID projectId,
                                                         @Valid @RequestBody UpdateProjectRequest request) {
        ProjectResponse response = projectService.updateProject(projectId, request);
        auditLogService.log(SecurityUtils.getCurrentUserId(), null, "PROJECT_UPDATED", "PROJECT", projectId, null);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{projectId}/archive")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> archiveProject(@PathVariable UUID projectId) {
        projectService.archiveProject(projectId);
        auditLogService.log(SecurityUtils.getCurrentUserId(), null, "PROJECT_ARCHIVED", "PROJECT", projectId, null);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{projectId}/unarchive")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> unarchiveProject(@PathVariable UUID projectId) {
        projectService.unarchiveProject(projectId);
        auditLogService.log(SecurityUtils.getCurrentUserId(), null, "PROJECT_UNARCHIVED", "PROJECT", projectId, null);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{projectId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteProject(@PathVariable UUID projectId) {
        projectService.deleteProject(projectId);
        auditLogService.log(SecurityUtils.getCurrentUserId(), null, "PROJECT_DELETED", "PROJECT", projectId, null);
        return ResponseEntity.noContent().build();
    }
}
