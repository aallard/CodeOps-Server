package com.codeops.controller;

import com.codeops.dto.request.AssignDirectiveRequest;
import com.codeops.dto.request.CreateDirectiveRequest;
import com.codeops.dto.request.UpdateDirectiveRequest;
import com.codeops.dto.response.DirectiveResponse;
import com.codeops.dto.response.ProjectDirectiveResponse;
import com.codeops.entity.enums.DirectiveCategory;
import com.codeops.entity.enums.DirectiveScope;
import com.codeops.service.AuditLogService;
import com.codeops.service.DirectiveService;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DirectiveControllerTest {

    @Mock
    private DirectiveService directiveService;

    @Mock
    private AuditLogService auditLogService;

    private DirectiveController controller;

    private final UUID currentUserId = UUID.randomUUID();
    private final UUID teamId = UUID.randomUUID();
    private final UUID projectId = UUID.randomUUID();
    private final UUID directiveId = UUID.randomUUID();
    private final Instant now = Instant.now();

    @BeforeEach
    void setUp() {
        controller = new DirectiveController(directiveService, auditLogService);
        setSecurityContext(currentUserId);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void setSecurityContext(UUID userId) {
        var auth = new UsernamePasswordAuthenticationToken(userId, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private DirectiveResponse directiveResponse() {
        return new DirectiveResponse(directiveId, "Code Standards", "Description", "# Standards",
                DirectiveCategory.STANDARDS, DirectiveScope.TEAM, teamId, null,
                currentUserId, "Test User", 1, now, now);
    }

    @Test
    void createDirective_returns201WithDirectiveResponse() {
        CreateDirectiveRequest request = new CreateDirectiveRequest("Code Standards", "Description",
                "# Standards", DirectiveCategory.STANDARDS, DirectiveScope.TEAM, teamId, null);
        DirectiveResponse expected = directiveResponse();
        when(directiveService.createDirective(request)).thenReturn(expected);

        ResponseEntity<DirectiveResponse> response = controller.createDirective(request);

        assertEquals(201, response.getStatusCode().value());
        assertEquals(expected, response.getBody());
        verify(directiveService).createDirective(request);
        verify(auditLogService).log(currentUserId, teamId, "DIRECTIVE_CREATED", "DIRECTIVE", directiveId, "");
    }

    @Test
    void getDirective_returns200WithDirective() {
        DirectiveResponse expected = directiveResponse();
        when(directiveService.getDirective(directiveId)).thenReturn(expected);

        ResponseEntity<DirectiveResponse> response = controller.getDirective(directiveId);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(expected, response.getBody());
        verify(directiveService).getDirective(directiveId);
    }

    @Test
    void getDirectivesForTeam_returns200WithList() {
        List<DirectiveResponse> expected = List.of(directiveResponse());
        when(directiveService.getDirectivesForTeam(teamId)).thenReturn(expected);

        ResponseEntity<List<DirectiveResponse>> response = controller.getDirectivesForTeam(teamId);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(expected, response.getBody());
        verify(directiveService).getDirectivesForTeam(teamId);
    }

    @Test
    void getDirectivesForProject_returns200WithList() {
        DirectiveResponse projectDirective = new DirectiveResponse(directiveId, "Project Directive", "Desc",
                "# Content", DirectiveCategory.CONTEXT, DirectiveScope.PROJECT, null, projectId,
                currentUserId, "Test User", 1, now, now);
        List<DirectiveResponse> expected = List.of(projectDirective);
        when(directiveService.getDirectivesForProject(projectId)).thenReturn(expected);

        ResponseEntity<List<DirectiveResponse>> response = controller.getDirectivesForProject(projectId);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(expected, response.getBody());
        verify(directiveService).getDirectivesForProject(projectId);
    }

    @Test
    void updateDirective_returns200WithUpdatedDirective() {
        UpdateDirectiveRequest request = new UpdateDirectiveRequest("Updated Name", null, null, null);
        DirectiveResponse expected = new DirectiveResponse(directiveId, "Updated Name", "Description", "# Standards",
                DirectiveCategory.STANDARDS, DirectiveScope.TEAM, teamId, null,
                currentUserId, "Test User", 1, now, now);
        when(directiveService.updateDirective(directiveId, request)).thenReturn(expected);

        ResponseEntity<DirectiveResponse> response = controller.updateDirective(directiveId, request);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(expected, response.getBody());
        verify(directiveService).updateDirective(directiveId, request);
        verify(auditLogService).log(currentUserId, teamId, "DIRECTIVE_UPDATED", "DIRECTIVE", directiveId, "");
    }

    @Test
    void deleteDirective_returns204AndLogsAudit() {
        DirectiveResponse directive = directiveResponse();
        when(directiveService.getDirective(directiveId)).thenReturn(directive);

        ResponseEntity<Void> response = controller.deleteDirective(directiveId);

        assertEquals(204, response.getStatusCode().value());
        assertNull(response.getBody());
        verify(directiveService).getDirective(directiveId);
        verify(directiveService).deleteDirective(directiveId);
        verify(auditLogService).log(currentUserId, teamId, "DIRECTIVE_DELETED", "DIRECTIVE", directiveId, "");
    }

    @Test
    void assignToProject_returns201WithProjectDirectiveResponse() {
        AssignDirectiveRequest request = new AssignDirectiveRequest(projectId, directiveId, true);
        ProjectDirectiveResponse pdResponse = new ProjectDirectiveResponse(
                projectId, directiveId, "Code Standards", DirectiveCategory.STANDARDS, true);
        DirectiveResponse directive = directiveResponse();
        when(directiveService.assignToProject(request)).thenReturn(pdResponse);
        when(directiveService.getDirective(directiveId)).thenReturn(directive);

        ResponseEntity<ProjectDirectiveResponse> response = controller.assignToProject(request);

        assertEquals(201, response.getStatusCode().value());
        assertEquals(pdResponse, response.getBody());
        verify(directiveService).assignToProject(request);
        verify(directiveService).getDirective(directiveId);
        verify(auditLogService).log(currentUserId, teamId, "DIRECTIVE_ASSIGNED", "DIRECTIVE", directiveId, "");
    }

    @Test
    void removeFromProject_returns204AndLogsAudit() {
        DirectiveResponse directive = directiveResponse();
        when(directiveService.getDirective(directiveId)).thenReturn(directive);

        ResponseEntity<Void> response = controller.removeFromProject(projectId, directiveId);

        assertEquals(204, response.getStatusCode().value());
        assertNull(response.getBody());
        verify(directiveService).getDirective(directiveId);
        verify(directiveService).removeFromProject(projectId, directiveId);
        verify(auditLogService).log(currentUserId, teamId, "DIRECTIVE_REMOVED", "DIRECTIVE", directiveId, "");
    }

    @Test
    void getProjectDirectives_returns200WithList() {
        ProjectDirectiveResponse pd = new ProjectDirectiveResponse(
                projectId, directiveId, "Code Standards", DirectiveCategory.STANDARDS, true);
        List<ProjectDirectiveResponse> expected = List.of(pd);
        when(directiveService.getProjectDirectives(projectId)).thenReturn(expected);

        ResponseEntity<List<ProjectDirectiveResponse>> response = controller.getProjectDirectives(projectId);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(expected, response.getBody());
        verify(directiveService).getProjectDirectives(projectId);
    }

    @Test
    void getEnabledDirectives_returns200WithList() {
        List<DirectiveResponse> expected = List.of(directiveResponse());
        when(directiveService.getEnabledDirectivesForProject(projectId)).thenReturn(expected);

        ResponseEntity<List<DirectiveResponse>> response = controller.getEnabledDirectives(projectId);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(expected, response.getBody());
        verify(directiveService).getEnabledDirectivesForProject(projectId);
    }

    @Test
    void toggleDirective_returns200WithProjectDirectiveResponse() {
        ProjectDirectiveResponse expected = new ProjectDirectiveResponse(
                projectId, directiveId, "Code Standards", DirectiveCategory.STANDARDS, false);
        when(directiveService.toggleProjectDirective(projectId, directiveId, false)).thenReturn(expected);

        ResponseEntity<ProjectDirectiveResponse> response = controller.toggleDirective(projectId, directiveId, false);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(expected, response.getBody());
        verify(directiveService).toggleProjectDirective(projectId, directiveId, false);
    }
}
