package com.codeops.service;

import com.codeops.dto.request.CreateTaskRequest;
import com.codeops.dto.request.UpdateTaskRequest;
import com.codeops.dto.response.TaskResponse;
import com.codeops.entity.RemediationTask;
import com.codeops.entity.enums.TaskStatus;
import com.codeops.repository.QaJobRepository;
import com.codeops.repository.RemediationTaskRepository;
import com.codeops.repository.TeamMemberRepository;
import com.codeops.repository.UserRepository;
import com.codeops.security.SecurityUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class RemediationTaskService {

    private final RemediationTaskRepository remediationTaskRepository;
    private final QaJobRepository qaJobRepository;
    private final UserRepository userRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final S3StorageService s3StorageService;
    private final ObjectMapper objectMapper;

    public TaskResponse createTask(CreateTaskRequest request) {
        var job = qaJobRepository.findById(request.jobId())
                .orElseThrow(() -> new EntityNotFoundException("Job not found"));
        verifyTeamMembership(job.getProject().getTeam().getId());

        RemediationTask task = RemediationTask.builder()
                .job(job)
                .taskNumber(request.taskNumber())
                .title(request.title())
                .description(request.description())
                .promptMd(request.promptMd())
                .promptS3Key(request.promptS3Key())
                .findingIds(serializeFindingIds(request.findingIds()))
                .priority(request.priority())
                .status(TaskStatus.PENDING)
                .build();

        task = remediationTaskRepository.save(task);
        return mapToResponse(task);
    }

    public List<TaskResponse> createTasks(List<CreateTaskRequest> requests) {
        if (requests.isEmpty()) return List.of();

        UUID firstJobId = requests.get(0).jobId();
        boolean allSameJob = requests.stream().allMatch(r -> r.jobId().equals(firstJobId));
        if (!allSameJob) {
            throw new IllegalArgumentException("All tasks must belong to the same job");
        }

        var job = qaJobRepository.findById(firstJobId)
                .orElseThrow(() -> new EntityNotFoundException("Job not found"));
        verifyTeamMembership(job.getProject().getTeam().getId());

        List<RemediationTask> tasks = requests.stream()
                .map(request -> RemediationTask.builder()
                        .job(job)
                        .taskNumber(request.taskNumber())
                        .title(request.title())
                        .description(request.description())
                        .promptMd(request.promptMd())
                        .promptS3Key(request.promptS3Key())
                        .findingIds(serializeFindingIds(request.findingIds()))
                        .priority(request.priority())
                        .status(TaskStatus.PENDING)
                        .build())
                .toList();

        tasks = remediationTaskRepository.saveAll(tasks);
        return tasks.stream().map(this::mapToResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> getTasksForJob(UUID jobId) {
        var job = qaJobRepository.findById(jobId)
                .orElseThrow(() -> new EntityNotFoundException("Job not found"));
        verifyTeamMembership(job.getProject().getTeam().getId());
        return remediationTaskRepository.findByJobIdOrderByTaskNumberAsc(jobId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public TaskResponse getTask(UUID taskId) {
        RemediationTask task = remediationTaskRepository.findById(taskId)
                .orElseThrow(() -> new EntityNotFoundException("Task not found"));
        verifyTeamMembership(task.getJob().getProject().getTeam().getId());
        return mapToResponse(task);
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> getTasksAssignedToUser(UUID userId) {
        return remediationTaskRepository.findByAssignedToId(userId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    public TaskResponse updateTask(UUID taskId, UpdateTaskRequest request) {
        RemediationTask task = remediationTaskRepository.findById(taskId)
                .orElseThrow(() -> new EntityNotFoundException("Task not found"));
        verifyTeamMembership(task.getJob().getProject().getTeam().getId());

        if (request.status() != null) task.setStatus(request.status());
        if (request.assignedTo() != null) {
            task.setAssignedTo(userRepository.getReferenceById(request.assignedTo()));
        }
        if (request.jiraKey() != null) task.setJiraKey(request.jiraKey());

        task = remediationTaskRepository.save(task);
        return mapToResponse(task);
    }

    public String uploadTaskPrompt(UUID jobId, int taskNumber, String promptMd) {
        String key = "tasks/" + jobId + "/task-" + String.format("%03d", taskNumber) + ".md";
        s3StorageService.upload(key, promptMd.getBytes(StandardCharsets.UTF_8), "text/markdown");
        return key;
    }

    private TaskResponse mapToResponse(RemediationTask task) {
        String assignedToName = null;
        if (task.getAssignedTo() != null) {
            assignedToName = task.getAssignedTo().getDisplayName();
        }
        return new TaskResponse(
                task.getId(),
                task.getJob().getId(),
                task.getTaskNumber(),
                task.getTitle(),
                task.getDescription(),
                task.getPromptMd(),
                task.getPromptS3Key(),
                deserializeFindingIds(task.getFindingIds()),
                task.getPriority(),
                task.getStatus(),
                task.getAssignedTo() != null ? task.getAssignedTo().getId() : null,
                assignedToName,
                task.getJiraKey(),
                task.getCreatedAt()
        );
    }

    private String serializeFindingIds(List<UUID> findingIds) {
        if (findingIds == null || findingIds.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(findingIds);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize finding IDs", e);
        }
    }

    private List<UUID> deserializeFindingIds(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<List<UUID>>() {});
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }

    private void verifyTeamMembership(UUID teamId) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        if (!teamMemberRepository.existsByTeamIdAndUserId(teamId, currentUserId)) {
            throw new AccessDeniedException("Not a member of this team");
        }
    }
}
