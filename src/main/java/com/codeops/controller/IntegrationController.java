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

/**
 * REST controller for managing third-party integration connections, currently
 * supporting GitHub and Jira integrations scoped to teams.
 *
 * <p>All endpoints require authentication. Connection credentials (GitHub PATs,
 * Jira API tokens) are stored encrypted via AES-256-GCM and are never returned
 * in plaintext through API responses.</p>
 *
 * @see GitHubConnectionService
 * @see JiraConnectionService
 * @see AuditLogService
 */
@RestController
@RequestMapping("/api/v1/integrations")
@RequiredArgsConstructor
@Tag(name = "Integrations")
public class IntegrationController {

    private final GitHubConnectionService gitHubConnectionService;
    private final JiraConnectionService jiraConnectionService;
    private final AuditLogService auditLogService;

    // ---- GitHub Endpoints ----

    /**
     * Creates a new GitHub connection for a team.
     *
     * <p>POST {@code /api/v1/integrations/github/{teamId}}</p>
     *
     * <p>Side effect: logs a {@code GITHUB_CONNECTION_CREATED} audit entry scoped to the team.</p>
     *
     * @param teamId  the UUID of the team to create the connection for
     * @param request the GitHub connection creation payload (repository URL, PAT, etc.)
     * @return the created GitHub connection (HTTP 201)
     */
    @PostMapping("/github/{teamId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<GitHubConnectionResponse> createGitHubConnection(
            @PathVariable UUID teamId,
            @Valid @RequestBody CreateGitHubConnectionRequest request) {
        GitHubConnectionResponse response = gitHubConnectionService.createConnection(teamId, request);
        auditLogService.log(SecurityUtils.getCurrentUserId(), teamId, "GITHUB_CONNECTION_CREATED", "GITHUB_CONNECTION", response.id(), null);
        return ResponseEntity.status(201).body(response);
    }

    /**
     * Retrieves all GitHub connections for a team.
     *
     * <p>GET {@code /api/v1/integrations/github/{teamId}}</p>
     *
     * @param teamId the UUID of the team
     * @return list of GitHub connections belonging to the team
     */
    @GetMapping("/github/{teamId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<GitHubConnectionResponse>> getGitHubConnections(@PathVariable UUID teamId) {
        return ResponseEntity.ok(gitHubConnectionService.getConnections(teamId));
    }

    /**
     * Retrieves a single GitHub connection by its identifier within a team context.
     *
     * <p>GET {@code /api/v1/integrations/github/{teamId}/{connectionId}}</p>
     *
     * @param teamId       the UUID of the team (used for path scoping)
     * @param connectionId the UUID of the GitHub connection
     * @return the GitHub connection details
     */
    @GetMapping("/github/{teamId}/{connectionId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<GitHubConnectionResponse> getGitHubConnection(
            @PathVariable UUID teamId,
            @PathVariable UUID connectionId) {
        return ResponseEntity.ok(gitHubConnectionService.getConnection(connectionId));
    }

    /**
     * Deletes a GitHub connection.
     *
     * <p>DELETE {@code /api/v1/integrations/github/{teamId}/{connectionId}}</p>
     *
     * <p>Side effect: logs a {@code GITHUB_CONNECTION_DELETED} audit entry scoped to the team.</p>
     *
     * @param teamId       the UUID of the team
     * @param connectionId the UUID of the GitHub connection to delete
     * @return HTTP 204 No Content on successful deletion
     */
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

    /**
     * Creates a new Jira connection for a team.
     *
     * <p>POST {@code /api/v1/integrations/jira/{teamId}}</p>
     *
     * <p>Side effect: logs a {@code JIRA_CONNECTION_CREATED} audit entry scoped to the team.</p>
     *
     * @param teamId  the UUID of the team to create the connection for
     * @param request the Jira connection creation payload (site URL, API token, etc.)
     * @return the created Jira connection (HTTP 201)
     */
    @PostMapping("/jira/{teamId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<JiraConnectionResponse> createJiraConnection(
            @PathVariable UUID teamId,
            @Valid @RequestBody CreateJiraConnectionRequest request) {
        JiraConnectionResponse response = jiraConnectionService.createConnection(teamId, request);
        auditLogService.log(SecurityUtils.getCurrentUserId(), teamId, "JIRA_CONNECTION_CREATED", "JIRA_CONNECTION", response.id(), null);
        return ResponseEntity.status(201).body(response);
    }

    /**
     * Retrieves all Jira connections for a team.
     *
     * <p>GET {@code /api/v1/integrations/jira/{teamId}}</p>
     *
     * @param teamId the UUID of the team
     * @return list of Jira connections belonging to the team
     */
    @GetMapping("/jira/{teamId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<JiraConnectionResponse>> getJiraConnections(@PathVariable UUID teamId) {
        return ResponseEntity.ok(jiraConnectionService.getConnections(teamId));
    }

    /**
     * Retrieves a single Jira connection by its identifier within a team context.
     *
     * <p>GET {@code /api/v1/integrations/jira/{teamId}/{connectionId}}</p>
     *
     * @param teamId       the UUID of the team (used for path scoping)
     * @param connectionId the UUID of the Jira connection
     * @return the Jira connection details
     */
    @GetMapping("/jira/{teamId}/{connectionId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<JiraConnectionResponse> getJiraConnection(
            @PathVariable UUID teamId,
            @PathVariable UUID connectionId) {
        return ResponseEntity.ok(jiraConnectionService.getConnection(connectionId));
    }

    /**
     * Deletes a Jira connection.
     *
     * <p>DELETE {@code /api/v1/integrations/jira/{teamId}/{connectionId}}</p>
     *
     * <p>Side effect: logs a {@code JIRA_CONNECTION_DELETED} audit entry scoped to the team.</p>
     *
     * @param teamId       the UUID of the team
     * @param connectionId the UUID of the Jira connection to delete
     * @return HTTP 204 No Content on successful deletion
     */
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
