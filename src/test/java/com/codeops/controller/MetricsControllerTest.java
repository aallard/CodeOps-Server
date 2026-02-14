package com.codeops.controller;

import com.codeops.dto.response.HealthSnapshotResponse;
import com.codeops.dto.response.ProjectMetricsResponse;
import com.codeops.dto.response.TeamMetricsResponse;
import com.codeops.service.MetricsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MetricsControllerTest {

    @Mock
    private MetricsService metricsService;

    private MetricsController controller;

    private final UUID projectId = UUID.randomUUID();
    private final UUID teamId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        controller = new MetricsController(metricsService);
    }

    private ProjectMetricsResponse buildProjectMetrics() {
        return new ProjectMetricsResponse(projectId, "TestProject", 85, 78,
                20, 150, 3, 12, 8, 5, Instant.now());
    }

    private TeamMetricsResponse buildTeamMetrics() {
        return new TeamMetricsResponse(teamId, 5, 42, 310, 82.5, 1, 7);
    }

    private HealthSnapshotResponse buildSnapshotResponse(UUID id) {
        return new HealthSnapshotResponse(id, projectId, UUID.randomUUID(), 85,
                "{\"CRITICAL\":1,\"HIGH\":3}", 70, 90,
                new BigDecimal("78.5"), Instant.now());
    }

    @Test
    void getProjectMetrics_returnsOkWithBody() {
        ProjectMetricsResponse response = buildProjectMetrics();
        when(metricsService.getProjectMetrics(projectId)).thenReturn(response);

        ResponseEntity<ProjectMetricsResponse> result = controller.getProjectMetrics(projectId);

        assertThat(result.getStatusCode().value()).isEqualTo(200);
        assertThat(result.getBody()).isEqualTo(response);
        assertThat(result.getBody().projectName()).isEqualTo("TestProject");
        assertThat(result.getBody().currentHealthScore()).isEqualTo(85);
        verify(metricsService).getProjectMetrics(projectId);
    }

    @Test
    void getTeamMetrics_returnsOkWithBody() {
        TeamMetricsResponse response = buildTeamMetrics();
        when(metricsService.getTeamMetrics(teamId)).thenReturn(response);

        ResponseEntity<TeamMetricsResponse> result = controller.getTeamMetrics(teamId);

        assertThat(result.getStatusCode().value()).isEqualTo(200);
        assertThat(result.getBody()).isEqualTo(response);
        assertThat(result.getBody().totalProjects()).isEqualTo(5);
        assertThat(result.getBody().averageHealthScore()).isEqualTo(82.5);
        verify(metricsService).getTeamMetrics(teamId);
    }

    @Test
    void getHealthTrend_returnsOkWithList() {
        List<HealthSnapshotResponse> responses = List.of(
                buildSnapshotResponse(UUID.randomUUID()),
                buildSnapshotResponse(UUID.randomUUID()),
                buildSnapshotResponse(UUID.randomUUID()));
        when(metricsService.getHealthTrend(projectId, 30)).thenReturn(responses);

        ResponseEntity<List<HealthSnapshotResponse>> result = controller.getHealthTrend(projectId, 30);

        assertThat(result.getStatusCode().value()).isEqualTo(200);
        assertThat(result.getBody()).hasSize(3);
        verify(metricsService).getHealthTrend(projectId, 30);
    }

    @Test
    void getHealthTrend_withCustomDays_passesCorrectParam() {
        List<HealthSnapshotResponse> responses = List.of(buildSnapshotResponse(UUID.randomUUID()));
        when(metricsService.getHealthTrend(projectId, 7)).thenReturn(responses);

        ResponseEntity<List<HealthSnapshotResponse>> result = controller.getHealthTrend(projectId, 7);

        assertThat(result.getStatusCode().value()).isEqualTo(200);
        assertThat(result.getBody()).hasSize(1);
        verify(metricsService).getHealthTrend(projectId, 7);
    }
}
