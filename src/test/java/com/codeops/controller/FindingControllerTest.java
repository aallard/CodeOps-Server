package com.codeops.controller;

import com.codeops.dto.request.BulkUpdateFindingsRequest;
import com.codeops.dto.request.CreateFindingRequest;
import com.codeops.dto.request.UpdateFindingStatusRequest;
import com.codeops.dto.response.FindingResponse;
import com.codeops.dto.response.PageResponse;
import com.codeops.entity.enums.*;
import com.codeops.service.AuditLogService;
import com.codeops.service.FindingService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FindingControllerTest {

    @Mock
    private FindingService findingService;

    @Mock
    private AuditLogService auditLogService;

    private FindingController controller;

    private final UUID userId = UUID.randomUUID();
    private final UUID findingId = UUID.randomUUID();
    private final UUID jobId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        controller = new FindingController(findingService, auditLogService);
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

    private FindingResponse buildFindingResponse(UUID id) {
        return new FindingResponse(id, jobId, AgentType.SECURITY, Severity.HIGH, "SQL Injection found",
                "Description", "/src/Main.java", 42, "Use parameterized queries",
                "evidence text", Effort.M, DebtCategory.CODE, FindingStatus.OPEN,
                null, null, Instant.now());
    }

    @Test
    void createFinding_returnsCreatedWithBody() {
        CreateFindingRequest request = new CreateFindingRequest(jobId, AgentType.SECURITY, Severity.HIGH,
                "SQL Injection found", "Description", "/src/Main.java", 42,
                "Use parameterized queries", "evidence text", Effort.M, DebtCategory.CODE);
        FindingResponse response = buildFindingResponse(findingId);
        when(findingService.createFinding(request)).thenReturn(response);

        ResponseEntity<FindingResponse> result = controller.createFinding(request);

        assertThat(result.getStatusCode().value()).isEqualTo(201);
        assertThat(result.getBody()).isEqualTo(response);
        verify(findingService).createFinding(request);
        verify(auditLogService).log(userId, null, "FINDING_CREATED", "FINDING", findingId, null);
    }

    @Test
    void createFindings_batch_returnsCreatedWithList() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        CreateFindingRequest req1 = new CreateFindingRequest(jobId, AgentType.SECURITY, Severity.HIGH,
                "Finding 1", null, null, null, null, null, null, null);
        CreateFindingRequest req2 = new CreateFindingRequest(jobId, AgentType.CODE_QUALITY, Severity.MEDIUM,
                "Finding 2", null, null, null, null, null, null, null);
        List<CreateFindingRequest> requests = List.of(req1, req2);
        List<FindingResponse> responses = List.of(buildFindingResponse(id1), buildFindingResponse(id2));
        when(findingService.createFindings(requests)).thenReturn(responses);

        ResponseEntity<List<FindingResponse>> result = controller.createFindings(requests);

        assertThat(result.getStatusCode().value()).isEqualTo(201);
        assertThat(result.getBody()).hasSize(2);
        verify(findingService).createFindings(requests);
        verify(auditLogService, times(2)).log(eq(userId), isNull(), eq("FINDING_CREATED"),
                eq("FINDING"), any(UUID.class), isNull());
    }

    @Test
    void getFinding_returnsOkWithBody() {
        FindingResponse response = buildFindingResponse(findingId);
        when(findingService.getFinding(findingId)).thenReturn(response);

        ResponseEntity<FindingResponse> result = controller.getFinding(findingId);

        assertThat(result.getStatusCode().value()).isEqualTo(200);
        assertThat(result.getBody()).isEqualTo(response);
        verify(findingService).getFinding(findingId);
    }

    @Test
    void getFindingsForJob_returnsOkWithPage() {
        PageResponse<FindingResponse> page = new PageResponse<>(
                List.of(buildFindingResponse(findingId)), 0, 20, 1, 1, true);
        when(findingService.getFindingsForJob(eq(jobId), any())).thenReturn(page);

        ResponseEntity<PageResponse<FindingResponse>> result = controller.getFindingsForJob(jobId, 0, 20);

        assertThat(result.getStatusCode().value()).isEqualTo(200);
        assertThat(result.getBody().content()).hasSize(1);
        verify(findingService).getFindingsForJob(eq(jobId), any());
    }

    @Test
    void getFindingsBySeverity_returnsOkWithPage() {
        PageResponse<FindingResponse> page = new PageResponse<>(List.of(), 0, 20, 0, 0, true);
        when(findingService.getFindingsByJobAndSeverity(eq(jobId), eq(Severity.CRITICAL), any())).thenReturn(page);

        ResponseEntity<PageResponse<FindingResponse>> result = controller.getFindingsBySeverity(jobId, Severity.CRITICAL, 0, 20);

        assertThat(result.getStatusCode().value()).isEqualTo(200);
        verify(findingService).getFindingsByJobAndSeverity(eq(jobId), eq(Severity.CRITICAL), any());
    }

    @Test
    void getFindingsByAgent_returnsOkWithPage() {
        PageResponse<FindingResponse> page = new PageResponse<>(List.of(), 0, 20, 0, 0, true);
        when(findingService.getFindingsByJobAndAgent(eq(jobId), eq(AgentType.SECURITY), any())).thenReturn(page);

        ResponseEntity<PageResponse<FindingResponse>> result = controller.getFindingsByAgent(jobId, AgentType.SECURITY, 0, 20);

        assertThat(result.getStatusCode().value()).isEqualTo(200);
        verify(findingService).getFindingsByJobAndAgent(eq(jobId), eq(AgentType.SECURITY), any());
    }

    @Test
    void getFindingsByStatus_returnsOkWithPage() {
        PageResponse<FindingResponse> page = new PageResponse<>(List.of(), 0, 20, 0, 0, true);
        when(findingService.getFindingsByJobAndStatus(eq(jobId), eq(FindingStatus.OPEN), any())).thenReturn(page);

        ResponseEntity<PageResponse<FindingResponse>> result = controller.getFindingsByStatus(jobId, FindingStatus.OPEN, 0, 20);

        assertThat(result.getStatusCode().value()).isEqualTo(200);
        verify(findingService).getFindingsByJobAndStatus(eq(jobId), eq(FindingStatus.OPEN), any());
    }

    @Test
    void getSeverityCounts_returnsOkWithMap() {
        Map<Severity, Long> counts = Map.of(Severity.CRITICAL, 3L, Severity.HIGH, 7L);
        when(findingService.countFindingsBySeverity(jobId)).thenReturn(counts);

        ResponseEntity<Map<Severity, Long>> result = controller.getSeverityCounts(jobId);

        assertThat(result.getStatusCode().value()).isEqualTo(200);
        assertThat(result.getBody()).containsEntry(Severity.CRITICAL, 3L);
        verify(findingService).countFindingsBySeverity(jobId);
    }

    @Test
    void updateFindingStatus_returnsOkWithBody() {
        UpdateFindingStatusRequest request = new UpdateFindingStatusRequest(FindingStatus.ACKNOWLEDGED);
        FindingResponse response = buildFindingResponse(findingId);
        when(findingService.updateFindingStatus(findingId, request)).thenReturn(response);

        ResponseEntity<FindingResponse> result = controller.updateFindingStatus(findingId, request);

        assertThat(result.getStatusCode().value()).isEqualTo(200);
        assertThat(result.getBody()).isEqualTo(response);
        verify(findingService).updateFindingStatus(findingId, request);
        verify(auditLogService).log(userId, null, "FINDING_STATUS_UPDATED", "FINDING", findingId, "ACKNOWLEDGED");
    }

    @Test
    void bulkUpdateStatus_returnsOkWithList() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        BulkUpdateFindingsRequest request = new BulkUpdateFindingsRequest(List.of(id1, id2), FindingStatus.FALSE_POSITIVE);
        List<FindingResponse> responses = List.of(buildFindingResponse(id1), buildFindingResponse(id2));
        when(findingService.bulkUpdateFindingStatus(request)).thenReturn(responses);

        ResponseEntity<List<FindingResponse>> result = controller.bulkUpdateStatus(request);

        assertThat(result.getStatusCode().value()).isEqualTo(200);
        assertThat(result.getBody()).hasSize(2);
        verify(findingService).bulkUpdateFindingStatus(request);
        verify(auditLogService, times(2)).log(eq(userId), isNull(), eq("FINDING_STATUS_UPDATED"),
                eq("FINDING"), any(UUID.class), eq("FALSE_POSITIVE"));
    }
}
