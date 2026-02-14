package com.codeops.service;

import com.codeops.dto.request.CreateTaskRequest;
import com.codeops.dto.request.UpdateTaskRequest;
import com.codeops.dto.response.PageResponse;
import com.codeops.dto.response.TaskResponse;
import com.codeops.entity.*;
import com.codeops.entity.enums.Priority;
import com.codeops.entity.enums.TaskStatus;
import com.codeops.repository.*;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RemediationTaskServiceTest {

    @Mock private RemediationTaskRepository remediationTaskRepository;
    @Mock private QaJobRepository qaJobRepository;
    @Mock private UserRepository userRepository;
    @Mock private TeamMemberRepository teamMemberRepository;
    @Mock private FindingRepository findingRepository;
    @Mock private S3StorageService s3StorageService;

    @InjectMocks
    private RemediationTaskService remediationTaskService;

    private UUID userId;
    private UUID teamId;
    private UUID jobId;
    private UUID taskId;
    private UUID projectId;
    private Team team;
    private Project project;
    private QaJob job;
    private RemediationTask task;
    private User assignedUser;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        teamId = UUID.randomUUID();
        jobId = UUID.randomUUID();
        taskId = UUID.randomUUID();
        projectId = UUID.randomUUID();

        team = Team.builder().name("Test Team").build();
        team.setId(teamId);

        project = Project.builder().team(team).name("Test Project").build();
        project.setId(projectId);

        job = QaJob.builder().project(project).build();
        job.setId(jobId);
        job.setCreatedAt(Instant.now());

        assignedUser = User.builder().email("dev@codeops.dev").displayName("Dev User").build();
        assignedUser.setId(UUID.randomUUID());

        task = RemediationTask.builder()
                .job(job)
                .taskNumber(1)
                .title("Fix SQL injection")
                .description("Parameterize query")
                .promptMd("## Fix")
                .promptS3Key("tasks/key.md")
                .findings(new ArrayList<>())
                .priority(Priority.P0)
                .status(TaskStatus.PENDING)
                .build();
        task.setId(taskId);
        task.setCreatedAt(Instant.now());

        setSecurityContext(userId);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // --- createTask ---

    @Test
    void createTask_success() {
        CreateTaskRequest request = new CreateTaskRequest(jobId, 1, "Fix SQL injection",
                "Parameterize query", "## Fix", null, List.of(), Priority.P0);

        when(qaJobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(true);
        when(remediationTaskRepository.save(any(RemediationTask.class))).thenAnswer(inv -> {
            RemediationTask t = inv.getArgument(0);
            t.setId(taskId);
            t.setCreatedAt(Instant.now());
            return t;
        });

        TaskResponse response = remediationTaskService.createTask(request);

        assertNotNull(response);
        assertEquals("Fix SQL injection", response.title());
        assertEquals(TaskStatus.PENDING, response.status());
        assertEquals(Priority.P0, response.priority());
        verify(remediationTaskRepository).save(any(RemediationTask.class));
    }

    @Test
    void createTask_jobNotFound_throws() {
        CreateTaskRequest request = new CreateTaskRequest(jobId, 1, "Title",
                null, null, null, null, Priority.P1);

        when(qaJobRepository.findById(jobId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> remediationTaskService.createTask(request));
    }

    @Test
    void createTask_notTeamMember_throws() {
        CreateTaskRequest request = new CreateTaskRequest(jobId, 1, "Title",
                null, null, null, null, Priority.P1);

        when(qaJobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> remediationTaskService.createTask(request));
    }

    @Test
    void createTask_withFindings_resolvesFindings() {
        UUID findingId1 = UUID.randomUUID();
        UUID findingId2 = UUID.randomUUID();
        Finding f1 = Finding.builder().build();
        f1.setId(findingId1);
        Finding f2 = Finding.builder().build();
        f2.setId(findingId2);

        CreateTaskRequest request = new CreateTaskRequest(jobId, 1, "Title",
                null, null, null, List.of(findingId1, findingId2), Priority.P1);

        when(qaJobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(true);
        when(findingRepository.findAllById(List.of(findingId1, findingId2))).thenReturn(List.of(f1, f2));
        when(remediationTaskRepository.save(any(RemediationTask.class))).thenAnswer(inv -> {
            RemediationTask t = inv.getArgument(0);
            t.setId(taskId);
            t.setCreatedAt(Instant.now());
            return t;
        });

        TaskResponse response = remediationTaskService.createTask(request);

        assertNotNull(response);
        assertEquals(2, response.findingIds().size());
    }

    // --- createTasks ---

    @Test
    void createTasks_emptyList_returnsEmpty() {
        List<TaskResponse> result = remediationTaskService.createTasks(List.of());
        assertTrue(result.isEmpty());
    }

    @Test
    void createTasks_success() {
        CreateTaskRequest req1 = new CreateTaskRequest(jobId, 1, "Task 1", null, null, null, null, Priority.P1);
        CreateTaskRequest req2 = new CreateTaskRequest(jobId, 2, "Task 2", null, null, null, null, Priority.P2);

        when(qaJobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(true);
        when(remediationTaskRepository.saveAll(anyList())).thenAnswer(inv -> {
            List<RemediationTask> tasks = inv.getArgument(0);
            for (int i = 0; i < tasks.size(); i++) {
                tasks.get(i).setId(UUID.randomUUID());
                tasks.get(i).setCreatedAt(Instant.now());
            }
            return tasks;
        });

        List<TaskResponse> responses = remediationTaskService.createTasks(List.of(req1, req2));

        assertEquals(2, responses.size());
        verify(remediationTaskRepository).saveAll(anyList());
    }

    @Test
    void createTasks_differentJobs_throws() {
        UUID otherJobId = UUID.randomUUID();
        CreateTaskRequest req1 = new CreateTaskRequest(jobId, 1, "Task 1", null, null, null, null, Priority.P1);
        CreateTaskRequest req2 = new CreateTaskRequest(otherJobId, 2, "Task 2", null, null, null, null, Priority.P2);

        assertThrows(IllegalArgumentException.class,
                () -> remediationTaskService.createTasks(List.of(req1, req2)));
    }

    // --- getTasksForJob ---

    @Test
    void getTasksForJob_success() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<RemediationTask> page = new PageImpl<>(List.of(task), pageable, 1);

        when(qaJobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(true);
        when(remediationTaskRepository.findByJobId(jobId, pageable)).thenReturn(page);

        PageResponse<TaskResponse> result = remediationTaskService.getTasksForJob(jobId, pageable);

        assertEquals(1, result.content().size());
        assertEquals(1, result.totalElements());
    }

    @Test
    void getTasksForJob_jobNotFound_throws() {
        when(qaJobRepository.findById(jobId)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class,
                () -> remediationTaskService.getTasksForJob(jobId, PageRequest.of(0, 20)));
    }

    // --- getTask ---

    @Test
    void getTask_success() {
        when(remediationTaskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(true);

        TaskResponse response = remediationTaskService.getTask(taskId);

        assertNotNull(response);
        assertEquals(taskId, response.id());
        assertEquals("Fix SQL injection", response.title());
    }

    @Test
    void getTask_notFound_throws() {
        when(remediationTaskRepository.findById(taskId)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> remediationTaskService.getTask(taskId));
    }

    @Test
    void getTask_notTeamMember_throws() {
        when(remediationTaskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> remediationTaskService.getTask(taskId));
    }

    // --- getTasksAssignedToUser ---

    @Test
    void getTasksAssignedToUser_success() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<RemediationTask> page = new PageImpl<>(List.of(task), pageable, 1);

        when(remediationTaskRepository.findByAssignedToId(userId, pageable)).thenReturn(page);

        PageResponse<TaskResponse> result = remediationTaskService.getTasksAssignedToUser(userId, pageable);

        assertEquals(1, result.content().size());
    }

    // --- updateTask ---

    @Test
    void updateTask_statusUpdate_success() {
        UpdateTaskRequest request = new UpdateTaskRequest(TaskStatus.ASSIGNED, null, null);

        when(remediationTaskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(true);
        when(remediationTaskRepository.save(any(RemediationTask.class))).thenReturn(task);

        TaskResponse response = remediationTaskService.updateTask(taskId, request);

        assertNotNull(response);
        assertEquals(TaskStatus.ASSIGNED, task.getStatus());
    }

    @Test
    void updateTask_assignUser_success() {
        UUID assigneeId = assignedUser.getId();
        UpdateTaskRequest request = new UpdateTaskRequest(null, assigneeId, null);

        when(remediationTaskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(true);
        when(userRepository.findById(assigneeId)).thenReturn(Optional.of(assignedUser));
        when(remediationTaskRepository.save(any(RemediationTask.class))).thenReturn(task);

        TaskResponse response = remediationTaskService.updateTask(taskId, request);

        assertNotNull(response);
        assertEquals(assigneeId, response.assignedTo());
        assertEquals("Dev User", response.assignedToName());
    }

    @Test
    void updateTask_assignUser_userNotFound_throws() {
        UUID assigneeId = UUID.randomUUID();
        UpdateTaskRequest request = new UpdateTaskRequest(null, assigneeId, null);

        when(remediationTaskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(true);
        when(userRepository.findById(assigneeId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> remediationTaskService.updateTask(taskId, request));
    }

    @Test
    void updateTask_jiraKey_success() {
        UpdateTaskRequest request = new UpdateTaskRequest(null, null, "PROJ-123");

        when(remediationTaskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(true);
        when(remediationTaskRepository.save(any(RemediationTask.class))).thenReturn(task);

        remediationTaskService.updateTask(taskId, request);

        assertEquals("PROJ-123", task.getJiraKey());
    }

    @Test
    void updateTask_taskNotFound_throws() {
        UpdateTaskRequest request = new UpdateTaskRequest(TaskStatus.COMPLETED, null, null);
        when(remediationTaskRepository.findById(taskId)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> remediationTaskService.updateTask(taskId, request));
    }

    // --- uploadTaskPrompt ---

    @Test
    void uploadTaskPrompt_success() {
        String promptMd = "## Remediation steps";

        String key = remediationTaskService.uploadTaskPrompt(jobId, 1, promptMd);

        assertEquals("tasks/" + jobId + "/task-001.md", key);
        verify(s3StorageService).upload(eq(key), any(byte[].class), eq("text/markdown"));
    }

    @Test
    void uploadTaskPrompt_formatsTaskNumber() {
        String key = remediationTaskService.uploadTaskPrompt(jobId, 42, "content");
        assertTrue(key.contains("task-042.md"));
    }

    // --- mapToResponse with assignedTo ---

    @Test
    void getTask_withAssignedUser_includesAssignedInfo() {
        task.setAssignedTo(assignedUser);

        when(remediationTaskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(true);

        TaskResponse response = remediationTaskService.getTask(taskId);

        assertEquals(assignedUser.getId(), response.assignedTo());
        assertEquals("Dev User", response.assignedToName());
    }

    @Test
    void getTask_withNoAssignedUser_nullAssignedInfo() {
        task.setAssignedTo(null);

        when(remediationTaskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(true);

        TaskResponse response = remediationTaskService.getTask(taskId);

        assertNull(response.assignedTo());
        assertNull(response.assignedToName());
    }

    private void setSecurityContext(UUID userId) {
        var auth = new UsernamePasswordAuthenticationToken(userId, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
