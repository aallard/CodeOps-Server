package com.codeops.controller;

import com.codeops.config.AppConstants;
import com.codeops.dto.request.CreateTaskRequest;
import com.codeops.dto.request.UpdateTaskRequest;
import com.codeops.dto.response.PageResponse;
import com.codeops.dto.response.TaskResponse;
import com.codeops.security.SecurityUtils;
import com.codeops.service.AuditLogService;
import com.codeops.service.RemediationTaskService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for remediation task management operations.
 *
 * <p>Remediation tasks represent actionable work items generated from analysis job
 * findings (e.g., fix a vulnerability, resolve tech debt). Tasks can be assigned
 * to users and tracked through their lifecycle. All endpoints require authentication.</p>
 *
 * <p>Mutating operations (create, batch create, update) record an audit log entry
 * via {@link AuditLogService}.</p>
 *
 * @see RemediationTaskService
 * @see AuditLogService
 */
@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
@Tag(name = "Remediation Tasks")
public class TaskController {

    private final RemediationTaskService remediationTaskService;
    private final AuditLogService auditLogService;

    /**
     * Creates a single remediation task.
     *
     * <p>POST /api/v1/tasks</p>
     *
     * <p>Requires authentication. Logs a TASK_CREATED audit event.</p>
     *
     * @param request the task creation payload including title, description, and assignment details
     * @return the created task with HTTP 201 status
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TaskResponse> createTask(@Valid @RequestBody CreateTaskRequest request) {
        TaskResponse response = remediationTaskService.createTask(request);
        auditLogService.log(SecurityUtils.getCurrentUserId(), null, "TASK_CREATED", "TASK", response.id(), null);
        return ResponseEntity.status(201).body(response);
    }

    /**
     * Creates multiple remediation tasks in a single batch operation.
     *
     * <p>POST /api/v1/tasks/batch</p>
     *
     * <p>Requires authentication. Logs a TASK_CREATED audit event for each
     * successfully created task.</p>
     *
     * @param requests the list of task creation payloads
     * @return the list of created tasks with HTTP 201 status
     */
    @PostMapping("/batch")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<TaskResponse>> createTasks(
            @Valid @RequestBody List<CreateTaskRequest> requests) {
        List<TaskResponse> responses = remediationTaskService.createTasks(requests);
        responses.forEach(r -> auditLogService.log(SecurityUtils.getCurrentUserId(), null, "TASK_CREATED", "TASK", r.id(), null));
        return ResponseEntity.status(201).body(responses);
    }

    /**
     * Retrieves a paginated list of tasks associated with a specific analysis job.
     *
     * <p>GET /api/v1/tasks/job/{jobId}?page={page}&amp;size={size}</p>
     *
     * <p>Requires authentication. Results are sorted by task number ascending.
     * Page size is capped at {@link AppConstants#MAX_PAGE_SIZE}.</p>
     *
     * @param jobId the UUID of the job whose tasks to retrieve
     * @param page  zero-based page index (defaults to 0)
     * @param size  number of items per page (defaults to 20, capped at MAX_PAGE_SIZE)
     * @return a paginated list of tasks for the job
     */
    @GetMapping("/job/{jobId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PageResponse<TaskResponse>> getTasksForJob(
            @PathVariable UUID jobId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, AppConstants.MAX_PAGE_SIZE),
                Sort.by("taskNumber").ascending());
        return ResponseEntity.ok(remediationTaskService.getTasksForJob(jobId, pageable));
    }

    /**
     * Retrieves a single task by its identifier.
     *
     * <p>GET /api/v1/tasks/{taskId}</p>
     *
     * <p>Requires authentication.</p>
     *
     * @param taskId the UUID of the task to retrieve
     * @return the task details
     */
    @GetMapping("/{taskId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TaskResponse> getTask(@PathVariable UUID taskId) {
        return ResponseEntity.ok(remediationTaskService.getTask(taskId));
    }

    /**
     * Retrieves a paginated list of tasks assigned to the currently authenticated user.
     *
     * <p>GET /api/v1/tasks/assigned-to-me?page={page}&amp;size={size}</p>
     *
     * <p>Requires authentication. Results are sorted by creation date descending.
     * Page size is capped at {@link AppConstants#MAX_PAGE_SIZE}.</p>
     *
     * @param page zero-based page index (defaults to 0)
     * @param size number of items per page (defaults to 20, capped at MAX_PAGE_SIZE)
     * @return a paginated list of tasks assigned to the current user
     */
    @GetMapping("/assigned-to-me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PageResponse<TaskResponse>> getAssignedTasks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, AppConstants.MAX_PAGE_SIZE),
                Sort.by("createdAt").descending());
        return ResponseEntity.ok(remediationTaskService.getTasksAssignedToUser(SecurityUtils.getCurrentUserId(), pageable));
    }

    /**
     * Updates an existing remediation task.
     *
     * <p>PUT /api/v1/tasks/{taskId}</p>
     *
     * <p>Requires authentication. Logs a TASK_UPDATED audit event.</p>
     *
     * @param taskId  the UUID of the task to update
     * @param request the update payload with fields to modify (e.g., status, assignee, priority)
     * @return the updated task
     */
    @PutMapping("/{taskId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TaskResponse> updateTask(@PathVariable UUID taskId,
                                                    @Valid @RequestBody UpdateTaskRequest request) {
        TaskResponse response = remediationTaskService.updateTask(taskId, request);
        auditLogService.log(SecurityUtils.getCurrentUserId(), null, "TASK_UPDATED", "TASK", taskId, null);
        return ResponseEntity.ok(response);
    }
}
