package com.codeops.controller;

import com.codeops.dto.request.CreateTaskRequest;
import com.codeops.dto.request.UpdateTaskRequest;
import com.codeops.dto.response.TaskResponse;
import com.codeops.security.SecurityUtils;
import com.codeops.service.RemediationTaskService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TaskResponse> createTask(@Valid @RequestBody CreateTaskRequest request) {
        return ResponseEntity.status(201).body(remediationTaskService.createTask(request));
    }

    @PostMapping("/batch")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<TaskResponse>> createTasks(
            @Valid @RequestBody List<CreateTaskRequest> requests) {
        return ResponseEntity.status(201).body(remediationTaskService.createTasks(requests));
    }

    @GetMapping("/job/{jobId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<TaskResponse>> getTasksForJob(@PathVariable UUID jobId) {
        return ResponseEntity.ok(remediationTaskService.getTasksForJob(jobId));
    }

    @GetMapping("/{taskId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TaskResponse> getTask(@PathVariable UUID taskId) {
        return ResponseEntity.ok(remediationTaskService.getTask(taskId));
    }

    @GetMapping("/assigned-to-me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<TaskResponse>> getAssignedTasks() {
        return ResponseEntity.ok(remediationTaskService.getTasksAssignedToUser(SecurityUtils.getCurrentUserId()));
    }

    @PutMapping("/{taskId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TaskResponse> updateTask(@PathVariable UUID taskId,
                                                    @Valid @RequestBody UpdateTaskRequest request) {
        return ResponseEntity.ok(remediationTaskService.updateTask(taskId, request));
    }
}
