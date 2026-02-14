package com.codeops.controller;

import com.codeops.config.AppConstants;
import com.codeops.dto.request.CreateHealthScheduleRequest;
import com.codeops.dto.request.CreateHealthSnapshotRequest;
import com.codeops.dto.response.HealthScheduleResponse;
import com.codeops.dto.response.HealthSnapshotResponse;
import com.codeops.dto.response.PageResponse;
import com.codeops.security.SecurityUtils;
import com.codeops.service.AuditLogService;
import com.codeops.service.HealthMonitorService;
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
@RequestMapping("/api/v1/health-monitor")
@RequiredArgsConstructor
@Tag(name = "Health Monitor")
public class HealthMonitorController {

    private final HealthMonitorService healthMonitorService;
    private final AuditLogService auditLogService;

    @PostMapping("/schedules")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<HealthScheduleResponse> createSchedule(@Valid @RequestBody CreateHealthScheduleRequest request) {
        HealthScheduleResponse response = healthMonitorService.createSchedule(request);
        auditLogService.log(SecurityUtils.getCurrentUserId(), null, "HEALTH_SCHEDULE_CREATED", "HEALTH_SCHEDULE", response.id(), null);
        return ResponseEntity.status(201).body(response);
    }

    @GetMapping("/schedules/project/{projectId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<HealthScheduleResponse>> getSchedulesForProject(@PathVariable UUID projectId) {
        return ResponseEntity.ok(healthMonitorService.getSchedulesForProject(projectId));
    }

    @PutMapping("/schedules/{scheduleId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<HealthScheduleResponse> updateSchedule(@PathVariable UUID scheduleId,
                                                                  @RequestParam boolean active) {
        return ResponseEntity.ok(healthMonitorService.updateSchedule(scheduleId, active));
    }

    @DeleteMapping("/schedules/{scheduleId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteSchedule(@PathVariable UUID scheduleId) {
        healthMonitorService.deleteSchedule(scheduleId);
        auditLogService.log(SecurityUtils.getCurrentUserId(), null, "HEALTH_SCHEDULE_DELETED", "HEALTH_SCHEDULE", scheduleId, null);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/snapshots")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<HealthSnapshotResponse> createSnapshot(@Valid @RequestBody CreateHealthSnapshotRequest request) {
        return ResponseEntity.status(201).body(healthMonitorService.createSnapshot(request));
    }

    @GetMapping("/snapshots/project/{projectId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PageResponse<HealthSnapshotResponse>> getSnapshots(
            @PathVariable UUID projectId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, AppConstants.MAX_PAGE_SIZE),
                Sort.by("capturedAt").descending());
        return ResponseEntity.ok(healthMonitorService.getSnapshots(projectId, pageable));
    }

    @GetMapping("/snapshots/project/{projectId}/latest")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<HealthSnapshotResponse> getLatestSnapshot(@PathVariable UUID projectId) {
        return ResponseEntity.ok(healthMonitorService.getLatestSnapshot(projectId));
    }

    @GetMapping("/snapshots/project/{projectId}/trend")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<HealthSnapshotResponse>> getHealthTrend(@PathVariable UUID projectId,
                                                                        @RequestParam(defaultValue = "30") int limit) {
        return ResponseEntity.ok(healthMonitorService.getHealthTrend(projectId, limit));
    }
}
