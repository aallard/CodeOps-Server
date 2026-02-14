package com.codeops.service;

import com.codeops.dto.request.CreateTaskRequest;
import com.codeops.dto.request.UpdateTaskRequest;
import com.codeops.dto.response.PageResponse;
import com.codeops.dto.response.TaskResponse;
import com.codeops.entity.Finding;
import com.codeops.entity.RemediationTask;
import com.codeops.entity.enums.TaskStatus;
import com.codeops.repository.FindingRepository;
import com.codeops.repository.QaJobRepository;
import com.codeops.repository.RemediationTaskRepository;
import com.codeops.repository.TeamMemberRepository;
import com.codeops.repository.UserRepository;
import com.codeops.security.SecurityUtils;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
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
    private final FindingRepository findingRepository;
    private final S3StorageService s3StorageService;

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
                .findings(resolveFindingIds(request.findingIds()))
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
                        .findings(resolveFindingIds(request.findingIds()))
                        .priority(request.priority())
                        .status(TaskStatus.PENDING)
                        .build())
                .toList();

        tasks = remediationTaskRepository.saveAll(tasks);
        return tasks.stream().map(this::mapToResponse).toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<TaskResponse> getTasksForJob(UUID jobId, Pageable pageable) {
        var job = qaJobRepository.findById(jobId)
                .orElseThrow(() -> new EntityNotFoundException("Job not found"));
        verifyTeamMembership(job.getProject().getTeam().getId());
        Page<RemediationTask> page = remediationTaskRepository.findByJobId(jobId, pageable);
        List<TaskResponse> content = page.getContent().stream()
                .map(this::mapToResponse)
                .toList();
        return new PageResponse<>(content, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages(), page.isLast());
    }

    @Transactional(readOnly = true)
    public TaskResponse getTask(UUID taskId) {
        RemediationTask task = remediationTaskRepository.findById(taskId)
                .orElseThrow(() -> new EntityNotFoundException("Task not found"));
        verifyTeamMembership(task.getJob().getProject().getTeam().getId());
        return mapToResponse(task);
    }

    @Transactional(readOnly = true)
    public PageResponse<TaskResponse> getTasksAssignedToUser(UUID userId, Pageable pageable) {
        Page<RemediationTask> page = remediationTaskRepository.findByAssignedToId(userId, pageable);
        List<TaskResponse> content = page.getContent().stream()
                .map(this::mapToResponse)
                .toList();
        return new PageResponse<>(content, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages(), page.isLast());
    }

    public TaskResponse updateTask(UUID taskId, UpdateTaskRequest request) {
        RemediationTask task = remediationTaskRepository.findById(taskId)
                .orElseThrow(() -> new EntityNotFoundException("Task not found"));
        verifyTeamMembership(task.getJob().getProject().getTeam().getId());

        if (request.status() != null) task.setStatus(request.status());
        if (request.assignedTo() != null) {
            task.setAssignedTo(userRepository.findById(request.assignedTo())
                    .orElseThrow(() -> new EntityNotFoundException("User not found")));
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
        List<UUID> findingIds = task.getFindings().stream()
                .map(Finding::getId)
                .toList();
        return new TaskResponse(
                task.getId(),
                task.getJob().getId(),
                task.getTaskNumber(),
                task.getTitle(),
                task.getDescription(),
                task.getPromptMd(),
                task.getPromptS3Key(),
                findingIds,
                task.getPriority(),
                task.getStatus(),
                task.getAssignedTo() != null ? task.getAssignedTo().getId() : null,
                assignedToName,
                task.getJiraKey(),
                task.getCreatedAt()
        );
    }

    private List<Finding> resolveFindingIds(List<UUID> findingIds) {
        if (findingIds == null || findingIds.isEmpty()) return new ArrayList<>();
        return new ArrayList<>(findingRepository.findAllById(findingIds));
    }

    private void verifyTeamMembership(UUID teamId) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        if (!teamMemberRepository.existsByTeamIdAndUserId(teamId, currentUserId)) {
            throw new AccessDeniedException("Not a member of this team");
        }
    }
}
