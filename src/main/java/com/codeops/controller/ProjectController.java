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
        auditLogService.log(SecurityUtils.getCurrentUserId(), teamId, "PROJECT_CREATED", "PROJECT", response.id(), "");
        return ResponseEntity.status(201).body(response);
    }

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
        auditLogService.log(SecurityUtils.getCurrentUserId(), response.teamId(), "PROJECT_UPDATED", "PROJECT", projectId, "");
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{projectId}/archive")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> archiveProject(@PathVariable UUID projectId) {
        ProjectResponse project = projectService.getProject(projectId);
        projectService.archiveProject(projectId);
        auditLogService.log(SecurityUtils.getCurrentUserId(), project.teamId(), "PROJECT_ARCHIVED", "PROJECT", projectId, "");
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{projectId}/unarchive")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> unarchiveProject(@PathVariable UUID projectId) {
        ProjectResponse project = projectService.getProject(projectId);
        projectService.unarchiveProject(projectId);
        auditLogService.log(SecurityUtils.getCurrentUserId(), project.teamId(), "PROJECT_UNARCHIVED", "PROJECT", projectId, "");
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{projectId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteProject(@PathVariable UUID projectId) {
        ProjectResponse project = projectService.getProject(projectId);
        projectService.deleteProject(projectId);
        auditLogService.log(SecurityUtils.getCurrentUserId(), project.teamId(), "PROJECT_DELETED", "PROJECT", projectId, "");
        return ResponseEntity.noContent().build();
    }
}
