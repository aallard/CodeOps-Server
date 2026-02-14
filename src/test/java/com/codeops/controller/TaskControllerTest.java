package com.codeops.controller;

import com.codeops.dto.request.CreateTaskRequest;
import com.codeops.dto.request.UpdateTaskRequest;
import com.codeops.dto.response.PageResponse;
import com.codeops.dto.response.TaskResponse;
import com.codeops.entity.enums.Priority;
import com.codeops.entity.enums.TaskStatus;
import com.codeops.service.AuditLogService;
import com.codeops.service.RemediationTaskService;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskControllerTest {

    @Mock
    private RemediationTaskService remediationTaskService;

    @Mock
    private AuditLogService auditLogService;

    private TaskController controller;

    private final UUID userId = UUID.randomUUID();
    private final UUID taskId = UUID.randomUUID();
    private final UUID jobId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        controller = new TaskController(remediationTaskService, auditLogService);
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

    private TaskResponse buildTaskResponse(UUID id) {
        return new TaskResponse(id, jobId, 1, "Fix SQL injection", "Description",
                "## Prompt", "prompts/key.md", List.of(UUID.randomUUID()),
                Priority.P1, TaskStatus.PENDING, null, null, null, Instant.now());
    }

    @Test
    void createTask_returnsCreatedWithBody() {
        CreateTaskRequest request = new CreateTaskRequest(jobId, 1, "Fix SQL injection",
                "Description", "## Prompt", "prompts/key.md", List.of(UUID.randomUUID()), Priority.P1);
        TaskResponse response = buildTaskResponse(taskId);
        when(remediationTaskService.createTask(request)).thenReturn(response);

        ResponseEntity<TaskResponse> result = controller.createTask(request);

        assertThat(result.getStatusCode().value()).isEqualTo(201);
        assertThat(result.getBody()).isEqualTo(response);
        verify(remediationTaskService).createTask(request);
        verify(auditLogService).log(userId, null, "TASK_CREATED", "TASK", taskId, null);
    }

    @Test
    void createTasks_batch_returnsCreatedWithList() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        CreateTaskRequest req1 = new CreateTaskRequest(jobId, 1, "Task 1", null, null, null, null, Priority.P1);
        CreateTaskRequest req2 = new CreateTaskRequest(jobId, 2, "Task 2", null, null, null, null, Priority.P2);
        List<CreateTaskRequest> requests = List.of(req1, req2);
        List<TaskResponse> responses = List.of(buildTaskResponse(id1), buildTaskResponse(id2));
        when(remediationTaskService.createTasks(requests)).thenReturn(responses);

        ResponseEntity<List<TaskResponse>> result = controller.createTasks(requests);

        assertThat(result.getStatusCode().value()).isEqualTo(201);
        assertThat(result.getBody()).hasSize(2);
        verify(remediationTaskService).createTasks(requests);
        verify(auditLogService, times(2)).log(eq(userId), isNull(), eq("TASK_CREATED"),
                eq("TASK"), any(UUID.class), isNull());
    }

    @Test
    void getTasksForJob_returnsOkWithPage() {
        PageResponse<TaskResponse> page = new PageResponse<>(
                List.of(buildTaskResponse(taskId)), 0, 20, 1, 1, true);
        when(remediationTaskService.getTasksForJob(eq(jobId), any())).thenReturn(page);

        ResponseEntity<PageResponse<TaskResponse>> result = controller.getTasksForJob(jobId, 0, 20);

        assertThat(result.getStatusCode().value()).isEqualTo(200);
        assertThat(result.getBody().content()).hasSize(1);
        verify(remediationTaskService).getTasksForJob(eq(jobId), any());
    }

    @Test
    void getTask_returnsOkWithBody() {
        TaskResponse response = buildTaskResponse(taskId);
        when(remediationTaskService.getTask(taskId)).thenReturn(response);

        ResponseEntity<TaskResponse> result = controller.getTask(taskId);

        assertThat(result.getStatusCode().value()).isEqualTo(200);
        assertThat(result.getBody()).isEqualTo(response);
        verify(remediationTaskService).getTask(taskId);
    }

    @Test
    void getAssignedTasks_returnsOkWithPage() {
        PageResponse<TaskResponse> page = new PageResponse<>(
                List.of(buildTaskResponse(taskId)), 0, 20, 1, 1, true);
        when(remediationTaskService.getTasksAssignedToUser(eq(userId), any())).thenReturn(page);

        ResponseEntity<PageResponse<TaskResponse>> result = controller.getAssignedTasks(0, 20);

        assertThat(result.getStatusCode().value()).isEqualTo(200);
        assertThat(result.getBody().content()).hasSize(1);
        verify(remediationTaskService).getTasksAssignedToUser(eq(userId), any());
    }

    @Test
    void updateTask_returnsOkWithBody() {
        UpdateTaskRequest request = new UpdateTaskRequest(TaskStatus.ASSIGNED, userId, "PROJ-456");
        TaskResponse response = buildTaskResponse(taskId);
        when(remediationTaskService.updateTask(taskId, request)).thenReturn(response);

        ResponseEntity<TaskResponse> result = controller.updateTask(taskId, request);

        assertThat(result.getStatusCode().value()).isEqualTo(200);
        assertThat(result.getBody()).isEqualTo(response);
        verify(remediationTaskService).updateTask(taskId, request);
        verify(auditLogService).log(userId, null, "TASK_UPDATED", "TASK", taskId, null);
    }
}
