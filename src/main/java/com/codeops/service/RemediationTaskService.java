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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Manages remediation tasks that are created from QA job findings.
 *
 * <p>Remediation tasks represent actionable work items derived from code quality
 * findings. Each task is associated with a QA job and can reference one or more
 * findings. Tasks can be assigned to users and tracked through a status lifecycle.</p>
 *
 * <p>All operations verify that the calling user is a member of the team that owns
 * the associated project before proceeding.</p>
 *
 * @see TaskController
 * @see RemediationTask
 * @see RemediationTaskRepository
 */
@Service
@RequiredArgsConstructor
@Transactional
public class RemediationTaskService {

    private static final Logger log = LoggerFactory.getLogger(RemediationTaskService.class);

    private final RemediationTaskRepository remediationTaskRepository;
    private final QaJobRepository qaJobRepository;
    private final UserRepository userRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final FindingRepository findingRepository;
    private final S3StorageService s3StorageService;

    /**
     * Creates a single remediation task for a QA job.
     *
     * <p>Resolves the associated job, verifies team membership, resolves any
     * referenced finding IDs, and persists the task with an initial status of
     * {@link TaskStatus#PENDING}.</p>
     *
     * @param request the task creation request containing job ID, title, description,
     *                prompt details, finding IDs, and priority
     * @return the created task as a response DTO
     * @throws EntityNotFoundException if the referenced job is not found
     * @throws AccessDeniedException if the current user is not a member of the job's team
     */
    public TaskResponse createTask(CreateTaskRequest request) {
        log.debug("createTask called with jobId={}, title={}", request.jobId(), request.title());
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
        log.info("Created remediation task id={} for jobId={}, status={}", task.getId(), request.jobId(), task.getStatus());
        return mapToResponse(task);
    }

    /**
     * Creates multiple remediation tasks in bulk for a single QA job.
     *
     * <p>All requests must reference the same job ID. Verifies team membership once
     * for the shared job, then persists all tasks with an initial status of
     * {@link TaskStatus#PENDING}.</p>
     *
     * @param requests the list of task creation requests; all must share the same job ID
     * @return the list of created tasks as response DTOs, or an empty list if the input is empty
     * @throws IllegalArgumentException if the requests reference different job IDs
     * @throws EntityNotFoundException if the referenced job is not found
     * @throws AccessDeniedException if the current user is not a member of the job's team
     */
    public List<TaskResponse> createTasks(List<CreateTaskRequest> requests) {
        log.debug("createTasks called with count={}", requests.size());
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
        log.info("Created {} remediation tasks for jobId={}", tasks.size(), firstJobId);
        return tasks.stream().map(this::mapToResponse).toList();
    }

    /**
     * Retrieves a paginated list of remediation tasks for a specific QA job.
     *
     * @param jobId the ID of the QA job whose tasks to retrieve
     * @param pageable pagination and sorting parameters
     * @return a paginated response containing the job's remediation tasks
     * @throws EntityNotFoundException if the job is not found
     * @throws AccessDeniedException if the current user is not a member of the job's team
     */
    @Transactional(readOnly = true)
    public PageResponse<TaskResponse> getTasksForJob(UUID jobId, Pageable pageable) {
        log.debug("getTasksForJob called with jobId={}", jobId);
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

    /**
     * Retrieves a single remediation task by its ID.
     *
     * @param taskId the ID of the task to retrieve
     * @return the task as a response DTO
     * @throws EntityNotFoundException if the task is not found
     * @throws AccessDeniedException if the current user is not a member of the task's team
     */
    @Transactional(readOnly = true)
    public TaskResponse getTask(UUID taskId) {
        log.debug("getTask called with taskId={}", taskId);
        RemediationTask task = remediationTaskRepository.findById(taskId)
                .orElseThrow(() -> new EntityNotFoundException("Task not found"));
        verifyTeamMembership(task.getJob().getProject().getTeam().getId());
        return mapToResponse(task);
    }

    /**
     * Retrieves a paginated list of remediation tasks assigned to a specific user.
     *
     * <p>Does not perform team membership verification since tasks may span
     * multiple teams.</p>
     *
     * @param userId the ID of the user whose assigned tasks to retrieve
     * @param pageable pagination and sorting parameters
     * @return a paginated response containing tasks assigned to the user
     */
    @Transactional(readOnly = true)
    public PageResponse<TaskResponse> getTasksAssignedToUser(UUID userId, Pageable pageable) {
        log.debug("getTasksAssignedToUser called with userId={}", userId);
        Page<RemediationTask> page = remediationTaskRepository.findByAssignedToId(userId, pageable);
        List<TaskResponse> content = page.getContent().stream()
                .map(this::mapToResponse)
                .toList();
        return new PageResponse<>(content, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages(), page.isLast());
    }

    /**
     * Updates a remediation task's status, assignee, or Jira key.
     *
     * <p>Only non-null fields in the request are applied. If an assignee is specified,
     * the referenced user must exist in the system.</p>
     *
     * @param taskId the ID of the task to update
     * @param request the update request containing optional status, assignee, and Jira key
     * @return the updated task as a response DTO
     * @throws EntityNotFoundException if the task or the assigned user is not found
     * @throws AccessDeniedException if the current user is not a member of the task's team
     */
    public TaskResponse updateTask(UUID taskId, UpdateTaskRequest request) {
        log.debug("updateTask called with taskId={}", taskId);
        RemediationTask task = remediationTaskRepository.findById(taskId)
                .orElseThrow(() -> new EntityNotFoundException("Task not found"));
        verifyTeamMembership(task.getJob().getProject().getTeam().getId());

        TaskStatus oldStatus = task.getStatus();
        if (request.status() != null) task.setStatus(request.status());
        if (request.assignedTo() != null) {
            task.setAssignedTo(userRepository.findById(request.assignedTo())
                    .orElseThrow(() -> new EntityNotFoundException("User not found")));
        }
        if (request.jiraKey() != null) task.setJiraKey(request.jiraKey());

        task = remediationTaskRepository.save(task);
        if (request.status() != null && request.status() != oldStatus) {
            log.info("Updated remediation task id={} status from {} to {}", taskId, oldStatus, request.status());
        }
        if (request.assignedTo() != null) {
            log.info("Assigned remediation task id={} to userId={}", taskId, request.assignedTo());
        }
        return mapToResponse(task);
    }

    /**
     * Uploads a task prompt markdown file to S3 (or local storage).
     *
     * <p>The file is stored under the key pattern
     * {@code tasks/{jobId}/task-{taskNumber}.md} with zero-padded task numbers.</p>
     *
     * @param jobId the ID of the QA job the task belongs to
     * @param taskNumber the task number, used in the storage key (zero-padded to 3 digits)
     * @param promptMd the markdown content of the task prompt
     * @return the S3 key (or local storage path) where the prompt was stored
     */
    public String uploadTaskPrompt(UUID jobId, int taskNumber, String promptMd) {
        log.debug("uploadTaskPrompt called with jobId={}, taskNumber={}", jobId, taskNumber);
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
