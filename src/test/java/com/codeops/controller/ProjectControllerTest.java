package com.codeops.controller;

import com.codeops.dto.request.CreateProjectRequest;
import com.codeops.dto.request.UpdateProjectRequest;
import com.codeops.dto.response.PageResponse;
import com.codeops.dto.response.ProjectResponse;
import com.codeops.service.AuditLogService;
import com.codeops.service.ProjectService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectControllerTest {

    @Mock
    private ProjectService projectService;

    @Mock
    private AuditLogService auditLogService;

    private ProjectController controller;

    private final UUID currentUserId = UUID.randomUUID();
    private final UUID teamId = UUID.randomUUID();
    private final UUID projectId = UUID.randomUUID();
    private final Instant now = Instant.now();

    @BeforeEach
    void setUp() {
        controller = new ProjectController(projectService, auditLogService);
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

    private ProjectResponse projectResponse() {
        return new ProjectResponse(projectId, teamId, "My Project", "Description",
                null, null, null, "main", null, null, null, List.of(), null,
                "Java", 100, null, false, now, now);
    }

    @Test
    void createProject_returns201WithProjectResponse() {
        CreateProjectRequest request = new CreateProjectRequest("My Project", "Description",
                null, null, null, null, null, null, null, null, null, "Java");
        ProjectResponse expected = projectResponse();
        when(projectService.createProject(teamId, request)).thenReturn(expected);

        ResponseEntity<ProjectResponse> response = controller.createProject(teamId, request);

        assertEquals(201, response.getStatusCode().value());
        assertEquals(expected, response.getBody());
        verify(projectService).createProject(teamId, request);
        verify(auditLogService).log(currentUserId, teamId, "PROJECT_CREATED", "PROJECT", projectId, "");
    }

    @Test
    void getProjects_returns200WithPageResponse() {
        PageResponse<ProjectResponse> expected = new PageResponse<>(
                List.of(projectResponse()), 0, 20, 1, 1, true);
        when(projectService.getAllProjectsForTeam(eq(teamId), eq(false), any(Pageable.class)))
                .thenReturn(expected);

        ResponseEntity<PageResponse<ProjectResponse>> response = controller.getProjects(teamId, false, 0, 20);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(expected, response.getBody());
    }

    @Test
    void getProjects_clampsSizeToMaxPageSize() {
        PageResponse<ProjectResponse> expected = new PageResponse<>(
                List.of(), 0, 100, 0, 0, true);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        when(projectService.getAllProjectsForTeam(eq(teamId), eq(true), any(Pageable.class)))
                .thenReturn(expected);

        controller.getProjects(teamId, true, 0, 500);

        verify(projectService).getAllProjectsForTeam(eq(teamId), eq(true), pageableCaptor.capture());
        assertEquals(100, pageableCaptor.getValue().getPageSize());
    }

    @Test
    void getProject_returns200WithProject() {
        ProjectResponse expected = projectResponse();
        when(projectService.getProject(projectId)).thenReturn(expected);

        ResponseEntity<ProjectResponse> response = controller.getProject(projectId);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(expected, response.getBody());
        verify(projectService).getProject(projectId);
    }

    @Test
    void updateProject_returns200WithUpdatedProject() {
        UpdateProjectRequest request = new UpdateProjectRequest("Updated", null, null, null,
                null, null, null, null, null, null, null, null, null);
        ProjectResponse expected = new ProjectResponse(projectId, teamId, "Updated", "Description",
                null, null, null, "main", null, null, null, List.of(), null,
                "Java", 100, null, false, now, now);
        when(projectService.updateProject(projectId, request)).thenReturn(expected);

        ResponseEntity<ProjectResponse> response = controller.updateProject(projectId, request);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(expected, response.getBody());
        verify(projectService).updateProject(projectId, request);
        verify(auditLogService).log(currentUserId, teamId, "PROJECT_UPDATED", "PROJECT", projectId, "");
    }

    @Test
    void archiveProject_returns200AndLogsAudit() {
        ProjectResponse project = projectResponse();
        when(projectService.getProject(projectId)).thenReturn(project);

        ResponseEntity<Void> response = controller.archiveProject(projectId);

        assertEquals(200, response.getStatusCode().value());
        verify(projectService).getProject(projectId);
        verify(projectService).archiveProject(projectId);
        verify(auditLogService).log(currentUserId, teamId, "PROJECT_ARCHIVED", "PROJECT", projectId, "");
    }

    @Test
    void unarchiveProject_returns200AndLogsAudit() {
        ProjectResponse project = projectResponse();
        when(projectService.getProject(projectId)).thenReturn(project);

        ResponseEntity<Void> response = controller.unarchiveProject(projectId);

        assertEquals(200, response.getStatusCode().value());
        verify(projectService).getProject(projectId);
        verify(projectService).unarchiveProject(projectId);
        verify(auditLogService).log(currentUserId, teamId, "PROJECT_UNARCHIVED", "PROJECT", projectId, "");
    }

    @Test
    void deleteProject_returns204AndLogsAudit() {
        ProjectResponse project = projectResponse();
        when(projectService.getProject(projectId)).thenReturn(project);

        ResponseEntity<Void> response = controller.deleteProject(projectId);

        assertEquals(204, response.getStatusCode().value());
        assertNull(response.getBody());
        verify(projectService).getProject(projectId);
        verify(projectService).deleteProject(projectId);
        verify(auditLogService).log(currentUserId, teamId, "PROJECT_DELETED", "PROJECT", projectId, "");
    }
}
