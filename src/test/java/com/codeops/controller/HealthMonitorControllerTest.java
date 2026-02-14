package com.codeops.controller;

import com.codeops.dto.request.CreateHealthScheduleRequest;
import com.codeops.dto.request.CreateHealthSnapshotRequest;
import com.codeops.dto.response.HealthScheduleResponse;
import com.codeops.dto.response.HealthSnapshotResponse;
import com.codeops.dto.response.PageResponse;
import com.codeops.entity.enums.AgentType;
import com.codeops.entity.enums.ScheduleType;
import com.codeops.service.AuditLogService;
import com.codeops.service.HealthMonitorService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HealthMonitorControllerTest {

    @Mock
    private HealthMonitorService healthMonitorService;

    @Mock
    private AuditLogService auditLogService;

    private HealthMonitorController controller;

    private final UUID userId = UUID.randomUUID();
    private final UUID projectId = UUID.randomUUID();
    private final UUID scheduleId = UUID.randomUUID();
    private final UUID snapshotId = UUID.randomUUID();
    private final UUID jobId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        controller = new HealthMonitorController(healthMonitorService, auditLogService);
        setSecurityContext(userId);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void setSecurityContext(UUID userId) {
        var auth = new UsernamePasswordAuthenticationToken(userId, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private HealthScheduleResponse buildScheduleResponse(UUID id) {
        return new HealthScheduleResponse(id, projectId, ScheduleType.WEEKLY, "0 0 * * 1",
                List.of(AgentType.SECURITY, AgentType.CODE_QUALITY), true,
                Instant.now(), Instant.now().plusSeconds(604800), Instant.now());
    }

    private HealthSnapshotResponse buildSnapshotResponse(UUID id) {
        return new HealthSnapshotResponse(id, projectId, jobId, 85,
                "{\"CRITICAL\":1,\"HIGH\":3}", 70, 90,
                new BigDecimal("78.5"), Instant.now());
    }

    @Test
    void createSchedule_returnsCreatedWithBody() {
        CreateHealthScheduleRequest request = new CreateHealthScheduleRequest(projectId,
                ScheduleType.WEEKLY, "0 0 * * 1",
                List.of(AgentType.SECURITY, AgentType.CODE_QUALITY));
        HealthScheduleResponse response = buildScheduleResponse(scheduleId);
        when(healthMonitorService.createSchedule(request)).thenReturn(response);

        ResponseEntity<HealthScheduleResponse> result = controller.createSchedule(request);

        assertThat(result.getStatusCode().value()).isEqualTo(201);
        assertThat(result.getBody()).isEqualTo(response);
        verify(healthMonitorService).createSchedule(request);
        verify(auditLogService).log(userId, null, "HEALTH_SCHEDULE_CREATED", "HEALTH_SCHEDULE", scheduleId, null);
    }

    @Test
    void getSchedulesForProject_returnsOkWithList() {
        List<HealthScheduleResponse> responses = List.of(buildScheduleResponse(scheduleId));
        when(healthMonitorService.getSchedulesForProject(projectId)).thenReturn(responses);

        ResponseEntity<List<HealthScheduleResponse>> result = controller.getSchedulesForProject(projectId);

        assertThat(result.getStatusCode().value()).isEqualTo(200);
        assertThat(result.getBody()).hasSize(1);
        verify(healthMonitorService).getSchedulesForProject(projectId);
    }

    @Test
    void updateSchedule_returnsOkWithBody() {
        HealthScheduleResponse response = buildScheduleResponse(scheduleId);
        when(healthMonitorService.updateSchedule(scheduleId, false)).thenReturn(response);

        ResponseEntity<HealthScheduleResponse> result = controller.updateSchedule(scheduleId, false);

        assertThat(result.getStatusCode().value()).isEqualTo(200);
        assertThat(result.getBody()).isEqualTo(response);
        verify(healthMonitorService).updateSchedule(scheduleId, false);
    }

    @Test
    void deleteSchedule_returnsNoContent() {
        ResponseEntity<Void> result = controller.deleteSchedule(scheduleId);

        assertThat(result.getStatusCode().value()).isEqualTo(204);
        assertThat(result.getBody()).isNull();
        verify(healthMonitorService).deleteSchedule(scheduleId);
        verify(auditLogService).log(userId, null, "HEALTH_SCHEDULE_DELETED", "HEALTH_SCHEDULE", scheduleId, null);
    }

    @Test
    void createSnapshot_returnsCreatedWithBody() {
        CreateHealthSnapshotRequest request = new CreateHealthSnapshotRequest(projectId, jobId, 85,
                "{\"CRITICAL\":1,\"HIGH\":3}", 70, 90, new BigDecimal("78.5"));
        HealthSnapshotResponse response = buildSnapshotResponse(snapshotId);
        when(healthMonitorService.createSnapshot(request)).thenReturn(response);

        ResponseEntity<HealthSnapshotResponse> result = controller.createSnapshot(request);

        assertThat(result.getStatusCode().value()).isEqualTo(201);
        assertThat(result.getBody()).isEqualTo(response);
        verify(healthMonitorService).createSnapshot(request);
    }

    @Test
    void getSnapshots_returnsOkWithPage() {
        PageResponse<HealthSnapshotResponse> page = new PageResponse<>(
                List.of(buildSnapshotResponse(snapshotId)), 0, 20, 1, 1, true);
        when(healthMonitorService.getSnapshots(eq(projectId), any())).thenReturn(page);

        ResponseEntity<PageResponse<HealthSnapshotResponse>> result = controller.getSnapshots(projectId, 0, 20);

        assertThat(result.getStatusCode().value()).isEqualTo(200);
        assertThat(result.getBody().content()).hasSize(1);
        verify(healthMonitorService).getSnapshots(eq(projectId), any());
    }

    @Test
    void getLatestSnapshot_returnsOkWithBody() {
        HealthSnapshotResponse response = buildSnapshotResponse(snapshotId);
        when(healthMonitorService.getLatestSnapshot(projectId)).thenReturn(response);

        ResponseEntity<HealthSnapshotResponse> result = controller.getLatestSnapshot(projectId);

        assertThat(result.getStatusCode().value()).isEqualTo(200);
        assertThat(result.getBody()).isEqualTo(response);
        verify(healthMonitorService).getLatestSnapshot(projectId);
    }

    @Test
    void getHealthTrend_returnsOkWithList() {
        List<HealthSnapshotResponse> responses = List.of(
                buildSnapshotResponse(snapshotId),
                buildSnapshotResponse(UUID.randomUUID()));
        when(healthMonitorService.getHealthTrend(projectId, 30)).thenReturn(responses);

        ResponseEntity<List<HealthSnapshotResponse>> result = controller.getHealthTrend(projectId, 30);

        assertThat(result.getStatusCode().value()).isEqualTo(200);
        assertThat(result.getBody()).hasSize(2);
        verify(healthMonitorService).getHealthTrend(projectId, 30);
    }
}
