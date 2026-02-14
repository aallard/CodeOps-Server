package com.codeops.controller;

import com.codeops.dto.response.HealthSnapshotResponse;
import com.codeops.dto.response.ProjectMetricsResponse;
import com.codeops.dto.response.TeamMetricsResponse;
import com.codeops.service.MetricsService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/metrics")
@RequiredArgsConstructor
@Tag(name = "Metrics")
public class MetricsController {

    private final MetricsService metricsService;

    @GetMapping("/project/{projectId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProjectMetricsResponse> getProjectMetrics(@PathVariable UUID projectId) {
        return ResponseEntity.ok(metricsService.getProjectMetrics(projectId));
    }

    @GetMapping("/team/{teamId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TeamMetricsResponse> getTeamMetrics(@PathVariable UUID teamId) {
        return ResponseEntity.ok(metricsService.getTeamMetrics(teamId));
    }

    @GetMapping("/project/{projectId}/trend")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<HealthSnapshotResponse>> getHealthTrend(@PathVariable UUID projectId,
                                                                        @RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok(metricsService.getHealthTrend(projectId, days));
    }
}
