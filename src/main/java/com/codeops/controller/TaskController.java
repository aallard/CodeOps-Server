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

@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
@Tag(name = "Remediation Tasks")
public class TaskController {

    private final RemediationTaskService remediationTaskService;
    private final AuditLogService auditLogService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TaskResponse> createTask(@Valid @RequestBody CreateTaskRequest request) {
        TaskResponse response = remediationTaskService.createTask(request);
        auditLogService.log(SecurityUtils.getCurrentUserId(), null, "TASK_CREATED", "TASK", response.id(), null);
        return ResponseEntity.status(201).body(response);
    }

    @PostMapping("/batch")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<TaskResponse>> createTasks(
            @Valid @RequestBody List<CreateTaskRequest> requests) {
        List<TaskResponse> responses = remediationTaskService.createTasks(requests);
        responses.forEach(r -> auditLogService.log(SecurityUtils.getCurrentUserId(), null, "TASK_CREATED", "TASK", r.id(), null));
        return ResponseEntity.status(201).body(responses);
    }

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

    @GetMapping("/{taskId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TaskResponse> getTask(@PathVariable UUID taskId) {
        return ResponseEntity.ok(remediationTaskService.getTask(taskId));
    }

    @GetMapping("/assigned-to-me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PageResponse<TaskResponse>> getAssignedTasks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, AppConstants.MAX_PAGE_SIZE),
                Sort.by("createdAt").descending());
        return ResponseEntity.ok(remediationTaskService.getTasksAssignedToUser(SecurityUtils.getCurrentUserId(), pageable));
    }

    @PutMapping("/{taskId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TaskResponse> updateTask(@PathVariable UUID taskId,
                                                    @Valid @RequestBody UpdateTaskRequest request) {
        TaskResponse response = remediationTaskService.updateTask(taskId, request);
        auditLogService.log(SecurityUtils.getCurrentUserId(), null, "TASK_UPDATED", "TASK", taskId, null);
        return ResponseEntity.ok(response);
    }
}
