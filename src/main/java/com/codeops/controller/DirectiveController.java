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

@RestController
@RequestMapping("/api/v1/directives")
@RequiredArgsConstructor
@Tag(name = "Directives")
public class DirectiveController {

    private final DirectiveService directiveService;
    private final AuditLogService auditLogService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DirectiveResponse> createDirective(@Valid @RequestBody CreateDirectiveRequest request) {
        DirectiveResponse response = directiveService.createDirective(request);
        auditLogService.log(SecurityUtils.getCurrentUserId(), response.teamId(), "DIRECTIVE_CREATED", "DIRECTIVE", response.id(), "");
        return ResponseEntity.status(201).body(response);
    }

    @GetMapping("/{directiveId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DirectiveResponse> getDirective(@PathVariable UUID directiveId) {
        return ResponseEntity.ok(directiveService.getDirective(directiveId));
    }

    @GetMapping("/team/{teamId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<DirectiveResponse>> getDirectivesForTeam(@PathVariable UUID teamId) {
        return ResponseEntity.ok(directiveService.getDirectivesForTeam(teamId));
    }

    @GetMapping("/project/{projectId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<DirectiveResponse>> getDirectivesForProject(@PathVariable UUID projectId) {
        return ResponseEntity.ok(directiveService.getDirectivesForProject(projectId));
    }

    @PutMapping("/{directiveId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DirectiveResponse> updateDirective(@PathVariable UUID directiveId,
                                                              @Valid @RequestBody UpdateDirectiveRequest request) {
        DirectiveResponse response = directiveService.updateDirective(directiveId, request);
        auditLogService.log(SecurityUtils.getCurrentUserId(), response.teamId(), "DIRECTIVE_UPDATED", "DIRECTIVE", directiveId, "");
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{directiveId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteDirective(@PathVariable UUID directiveId) {
        DirectiveResponse directive = directiveService.getDirective(directiveId);
        directiveService.deleteDirective(directiveId);
        auditLogService.log(SecurityUtils.getCurrentUserId(), directive.teamId(), "DIRECTIVE_DELETED", "DIRECTIVE", directiveId, "");
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/assign")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProjectDirectiveResponse> assignToProject(@Valid @RequestBody AssignDirectiveRequest request) {
        ProjectDirectiveResponse response = directiveService.assignToProject(request);
        DirectiveResponse directive = directiveService.getDirective(request.directiveId());
        auditLogService.log(SecurityUtils.getCurrentUserId(), directive.teamId(), "DIRECTIVE_ASSIGNED", "DIRECTIVE", request.directiveId(), "");
        return ResponseEntity.status(201).body(response);
    }

    @DeleteMapping("/project/{projectId}/directive/{directiveId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> removeFromProject(@PathVariable UUID projectId, @PathVariable UUID directiveId) {
        DirectiveResponse directive = directiveService.getDirective(directiveId);
        directiveService.removeFromProject(projectId, directiveId);
        auditLogService.log(SecurityUtils.getCurrentUserId(), directive.teamId(), "DIRECTIVE_REMOVED", "DIRECTIVE", directiveId, "");
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/project/{projectId}/assignments")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ProjectDirectiveResponse>> getProjectDirectives(@PathVariable UUID projectId) {
        return ResponseEntity.ok(directiveService.getProjectDirectives(projectId));
    }

    @GetMapping("/project/{projectId}/enabled")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<DirectiveResponse>> getEnabledDirectives(@PathVariable UUID projectId) {
        return ResponseEntity.ok(directiveService.getEnabledDirectivesForProject(projectId));
    }

    @PutMapping("/project/{projectId}/directive/{directiveId}/toggle")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProjectDirectiveResponse> toggleDirective(@PathVariable UUID projectId,
                                                                     @PathVariable UUID directiveId,
                                                                     @RequestParam boolean enabled) {
        return ResponseEntity.ok(directiveService.toggleProjectDirective(projectId, directiveId, enabled));
    }
}
