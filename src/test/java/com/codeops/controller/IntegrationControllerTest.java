package com.codeops.controller;

import com.codeops.dto.request.CreateGitHubConnectionRequest;
import com.codeops.dto.request.CreateJiraConnectionRequest;
import com.codeops.dto.response.GitHubConnectionResponse;
import com.codeops.dto.response.JiraConnectionResponse;
import com.codeops.entity.enums.GitHubAuthType;
import com.codeops.service.AuditLogService;
import com.codeops.service.GitHubConnectionService;
import com.codeops.service.JiraConnectionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IntegrationControllerTest {

    @Mock
    private GitHubConnectionService gitHubConnectionService;

    @Mock
    private JiraConnectionService jiraConnectionService;

    @Mock
    private AuditLogService auditLogService;

    private IntegrationController controller;

    private final UUID userId = UUID.randomUUID();
    private final UUID teamId = UUID.randomUUID();
    private final UUID connectionId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        controller = new IntegrationController(gitHubConnectionService, jiraConnectionService, auditLogService);
        setSecurityContext(userId);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void setSecurityContext(UUID userId) {
        var auth = new UsernamePasswordAuthenticationToken(userId, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private GitHubConnectionResponse buildGitHubResponse(UUID id) {
        return new GitHubConnectionResponse(id, teamId, "My GitHub", GitHubAuthType.PAT,
                "octocat", true, Instant.now());
    }

    private JiraConnectionResponse buildJiraResponse(UUID id) {
        return new JiraConnectionResponse(id, teamId, "My Jira", "https://myorg.atlassian.net",
                "admin@myorg.com", true, Instant.now());
    }

    // ---- GitHub Tests ----

    @Test
    void createGitHubConnection_returnsCreatedWithBody() {
        CreateGitHubConnectionRequest request = new CreateGitHubConnectionRequest("My GitHub",
                GitHubAuthType.PAT, "ghp_abc123", "octocat");
        GitHubConnectionResponse response = buildGitHubResponse(connectionId);
        when(gitHubConnectionService.createConnection(teamId, request)).thenReturn(response);

        ResponseEntity<GitHubConnectionResponse> result = controller.createGitHubConnection(teamId, request);

        assertThat(result.getStatusCode().value()).isEqualTo(201);
        assertThat(result.getBody()).isEqualTo(response);
        verify(gitHubConnectionService).createConnection(teamId, request);
        verify(auditLogService).log(userId, teamId, "GITHUB_CONNECTION_CREATED", "GITHUB_CONNECTION", connectionId, null);
    }

    @Test
    void getGitHubConnections_returnsOkWithList() {
        List<GitHubConnectionResponse> responses = List.of(buildGitHubResponse(connectionId));
        when(gitHubConnectionService.getConnections(teamId)).thenReturn(responses);

        ResponseEntity<List<GitHubConnectionResponse>> result = controller.getGitHubConnections(teamId);

        assertThat(result.getStatusCode().value()).isEqualTo(200);
        assertThat(result.getBody()).hasSize(1);
        verify(gitHubConnectionService).getConnections(teamId);
    }

    @Test
    void getGitHubConnection_returnsOkWithBody() {
        GitHubConnectionResponse response = buildGitHubResponse(connectionId);
        when(gitHubConnectionService.getConnection(connectionId)).thenReturn(response);

        ResponseEntity<GitHubConnectionResponse> result = controller.getGitHubConnection(teamId, connectionId);

        assertThat(result.getStatusCode().value()).isEqualTo(200);
        assertThat(result.getBody()).isEqualTo(response);
        verify(gitHubConnectionService).getConnection(connectionId);
    }

    @Test
    void deleteGitHubConnection_returnsNoContent() {
        ResponseEntity<Void> result = controller.deleteGitHubConnection(teamId, connectionId);

        assertThat(result.getStatusCode().value()).isEqualTo(204);
        assertThat(result.getBody()).isNull();
        verify(gitHubConnectionService).deleteConnection(connectionId);
        verify(auditLogService).log(userId, teamId, "GITHUB_CONNECTION_DELETED", "GITHUB_CONNECTION", connectionId, null);
    }

    // ---- Jira Tests ----

    @Test
    void createJiraConnection_returnsCreatedWithBody() {
        CreateJiraConnectionRequest request = new CreateJiraConnectionRequest("My Jira",
                "https://myorg.atlassian.net", "admin@myorg.com", "api-token-123");
        JiraConnectionResponse response = buildJiraResponse(connectionId);
        when(jiraConnectionService.createConnection(teamId, request)).thenReturn(response);

        ResponseEntity<JiraConnectionResponse> result = controller.createJiraConnection(teamId, request);

        assertThat(result.getStatusCode().value()).isEqualTo(201);
        assertThat(result.getBody()).isEqualTo(response);
        verify(jiraConnectionService).createConnection(teamId, request);
        verify(auditLogService).log(userId, teamId, "JIRA_CONNECTION_CREATED", "JIRA_CONNECTION", connectionId, null);
    }

    @Test
    void getJiraConnections_returnsOkWithList() {
        List<JiraConnectionResponse> responses = List.of(buildJiraResponse(connectionId));
        when(jiraConnectionService.getConnections(teamId)).thenReturn(responses);

        ResponseEntity<List<JiraConnectionResponse>> result = controller.getJiraConnections(teamId);

        assertThat(result.getStatusCode().value()).isEqualTo(200);
        assertThat(result.getBody()).hasSize(1);
        verify(jiraConnectionService).getConnections(teamId);
    }

    @Test
    void getJiraConnection_returnsOkWithBody() {
        JiraConnectionResponse response = buildJiraResponse(connectionId);
        when(jiraConnectionService.getConnection(connectionId)).thenReturn(response);

        ResponseEntity<JiraConnectionResponse> result = controller.getJiraConnection(teamId, connectionId);

        assertThat(result.getStatusCode().value()).isEqualTo(200);
        assertThat(result.getBody()).isEqualTo(response);
        verify(jiraConnectionService).getConnection(connectionId);
    }

    @Test
    void deleteJiraConnection_returnsNoContent() {
        ResponseEntity<Void> result = controller.deleteJiraConnection(teamId, connectionId);

        assertThat(result.getStatusCode().value()).isEqualTo(204);
        assertThat(result.getBody()).isNull();
        verify(jiraConnectionService).deleteConnection(connectionId);
        verify(auditLogService).log(userId, teamId, "JIRA_CONNECTION_DELETED", "JIRA_CONNECTION", connectionId, null);
    }
}
