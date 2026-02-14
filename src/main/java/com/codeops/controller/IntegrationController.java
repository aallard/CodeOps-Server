package com.codeops.controller;

import com.codeops.dto.request.CreateGitHubConnectionRequest;
import com.codeops.dto.request.CreateJiraConnectionRequest;
import com.codeops.dto.response.GitHubConnectionResponse;
import com.codeops.dto.response.JiraConnectionResponse;
import com.codeops.security.SecurityUtils;
import com.codeops.service.AuditLogService;
import com.codeops.service.GitHubConnectionService;
import com.codeops.service.JiraConnectionService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/integrations")
@RequiredArgsConstructor
@Tag(name = "Integrations")
public class IntegrationController {

    private final GitHubConnectionService gitHubConnectionService;
    private final JiraConnectionService jiraConnectionService;
    private final AuditLogService auditLogService;

    // ---- GitHub Endpoints ----

    @PostMapping("/github/{teamId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<GitHubConnectionResponse> createGitHubConnection(
            @PathVariable UUID teamId,
            @Valid @RequestBody CreateGitHubConnectionRequest request) {
        GitHubConnectionResponse response = gitHubConnectionService.createConnection(teamId, request);
        auditLogService.log(SecurityUtils.getCurrentUserId(), teamId, "GITHUB_CONNECTION_CREATED", "GITHUB_CONNECTION", response.id(), null);
        return ResponseEntity.status(201).body(response);
    }

    @GetMapping("/github/{teamId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<GitHubConnectionResponse>> getGitHubConnections(@PathVariable UUID teamId) {
        return ResponseEntity.ok(gitHubConnectionService.getConnections(teamId));
    }

    @GetMapping("/github/{teamId}/{connectionId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<GitHubConnectionResponse> getGitHubConnection(
            @PathVariable UUID teamId,
            @PathVariable UUID connectionId) {
        return ResponseEntity.ok(gitHubConnectionService.getConnection(connectionId));
    }

    @DeleteMapping("/github/{teamId}/{connectionId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteGitHubConnection(
            @PathVariable UUID teamId,
            @PathVariable UUID connectionId) {
        gitHubConnectionService.deleteConnection(connectionId);
        auditLogService.log(SecurityUtils.getCurrentUserId(), teamId, "GITHUB_CONNECTION_DELETED", "GITHUB_CONNECTION", connectionId, null);
        return ResponseEntity.noContent().build();
    }

    // ---- Jira Endpoints ----

    @PostMapping("/jira/{teamId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<JiraConnectionResponse> createJiraConnection(
            @PathVariable UUID teamId,
            @Valid @RequestBody CreateJiraConnectionRequest request) {
        JiraConnectionResponse response = jiraConnectionService.createConnection(teamId, request);
        auditLogService.log(SecurityUtils.getCurrentUserId(), teamId, "JIRA_CONNECTION_CREATED", "JIRA_CONNECTION", response.id(), null);
        return ResponseEntity.status(201).body(response);
    }

    @GetMapping("/jira/{teamId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<JiraConnectionResponse>> getJiraConnections(@PathVariable UUID teamId) {
        return ResponseEntity.ok(jiraConnectionService.getConnections(teamId));
    }

    @GetMapping("/jira/{teamId}/{connectionId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<JiraConnectionResponse> getJiraConnection(
            @PathVariable UUID teamId,
            @PathVariable UUID connectionId) {
        return ResponseEntity.ok(jiraConnectionService.getConnection(connectionId));
    }

    @DeleteMapping("/jira/{teamId}/{connectionId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteJiraConnection(
            @PathVariable UUID teamId,
            @PathVariable UUID connectionId) {
        jiraConnectionService.deleteConnection(connectionId);
        auditLogService.log(SecurityUtils.getCurrentUserId(), teamId, "JIRA_CONNECTION_DELETED", "JIRA_CONNECTION", connectionId, null);
        return ResponseEntity.noContent().build();
    }
}
