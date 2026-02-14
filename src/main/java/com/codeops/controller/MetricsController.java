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

/**
 * REST controller for project and team metrics retrieval.
 *
 * <p>Provides endpoints for fetching aggregated project metrics, team metrics,
 * and project health score trends over time. All endpoints require authentication.</p>
 *
 * @see MetricsService
 */
@RestController
@RequestMapping("/api/v1/metrics")
@RequiredArgsConstructor
@Tag(name = "Metrics")
public class MetricsController {

    private final MetricsService metricsService;

    /**
     * Retrieves aggregated metrics for a specific project.
     *
     * <p>GET /api/v1/metrics/project/{projectId}</p>
     *
     * <p>Requires authentication.</p>
     *
     * @param projectId the UUID of the project to retrieve metrics for
     * @return the project metrics including health score and summary statistics
     */
    @GetMapping("/project/{projectId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProjectMetricsResponse> getProjectMetrics(@PathVariable UUID projectId) {
        return ResponseEntity.ok(metricsService.getProjectMetrics(projectId));
    }

    /**
     * Retrieves aggregated metrics for a specific team.
     *
     * <p>GET /api/v1/metrics/team/{teamId}</p>
     *
     * <p>Requires authentication.</p>
     *
     * @param teamId the UUID of the team to retrieve metrics for
     * @return the team metrics including aggregate project health and activity data
     */
    @GetMapping("/team/{teamId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TeamMetricsResponse> getTeamMetrics(@PathVariable UUID teamId) {
        return ResponseEntity.ok(metricsService.getTeamMetrics(teamId));
    }

    /**
     * Retrieves the health score trend for a project over a specified number of days.
     *
     * <p>GET /api/v1/metrics/project/{projectId}/trend?days={days}</p>
     *
     * <p>Requires authentication. Returns a list of health snapshots ordered chronologically,
     * allowing clients to render trend charts.</p>
     *
     * @param projectId the UUID of the project to retrieve the health trend for
     * @param days      the number of days of history to include (defaults to 30)
     * @return a list of health snapshots covering the requested time window
     */
    @GetMapping("/project/{projectId}/trend")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<HealthSnapshotResponse>> getHealthTrend(@PathVariable UUID projectId,
                                                                        @RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok(metricsService.getHealthTrend(projectId, days));
    }
}
