package com.codeops.controller;

import com.codeops.dto.request.CreateTechDebtItemRequest;
import com.codeops.dto.request.UpdateTechDebtStatusRequest;
import com.codeops.dto.response.PageResponse;
import com.codeops.dto.response.TechDebtItemResponse;
import com.codeops.entity.enums.*;
import com.codeops.service.AuditLogService;
import com.codeops.service.TechDebtService;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TechDebtControllerTest {

    @Mock
    private TechDebtService techDebtService;

    @Mock
    private AuditLogService auditLogService;

    private TechDebtController controller;

    private final UUID userId = UUID.randomUUID();
    private final UUID itemId = UUID.randomUUID();
    private final UUID projectId = UUID.randomUUID();
    private final UUID jobId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        controller = new TechDebtController(techDebtService, auditLogService);
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

    private TechDebtItemResponse buildTechDebtResponse(UUID id) {
        return new TechDebtItemResponse(id, projectId, DebtCategory.ARCHITECTURE, "Circular dependency",
                "Module A depends on Module B which depends on A", "/src/moduleA/Service.java",
                Effort.L, BusinessImpact.HIGH, DebtStatus.IDENTIFIED, jobId, null,
                Instant.now(), Instant.now());
    }

    @Test
    void createTechDebtItem_returnsCreatedWithBody() {
        CreateTechDebtItemRequest request = new CreateTechDebtItemRequest(projectId, DebtCategory.ARCHITECTURE,
                "Circular dependency", "Module A depends on Module B which depends on A",
                "/src/moduleA/Service.java", Effort.L, BusinessImpact.HIGH, jobId);
        TechDebtItemResponse response = buildTechDebtResponse(itemId);
        when(techDebtService.createTechDebtItem(request)).thenReturn(response);

        ResponseEntity<TechDebtItemResponse> result = controller.createTechDebtItem(request);

        assertThat(result.getStatusCode().value()).isEqualTo(201);
        assertThat(result.getBody()).isEqualTo(response);
        verify(techDebtService).createTechDebtItem(request);
    }

    @Test
    void createTechDebtItems_batch_returnsCreatedWithList() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        CreateTechDebtItemRequest req1 = new CreateTechDebtItemRequest(projectId, DebtCategory.CODE,
                "Debt 1", null, null, Effort.S, BusinessImpact.LOW, null);
        CreateTechDebtItemRequest req2 = new CreateTechDebtItemRequest(projectId, DebtCategory.TEST,
                "Debt 2", null, null, Effort.M, BusinessImpact.MEDIUM, null);
        List<CreateTechDebtItemRequest> requests = List.of(req1, req2);
        List<TechDebtItemResponse> responses = List.of(buildTechDebtResponse(id1), buildTechDebtResponse(id2));
        when(techDebtService.createTechDebtItems(requests)).thenReturn(responses);

        ResponseEntity<List<TechDebtItemResponse>> result = controller.createTechDebtItems(requests);

        assertThat(result.getStatusCode().value()).isEqualTo(201);
        assertThat(result.getBody()).hasSize(2);
        verify(techDebtService).createTechDebtItems(requests);
    }

    @Test
    void getTechDebtItem_returnsOkWithBody() {
        TechDebtItemResponse response = buildTechDebtResponse(itemId);
        when(techDebtService.getTechDebtItem(itemId)).thenReturn(response);

        ResponseEntity<TechDebtItemResponse> result = controller.getTechDebtItem(itemId);

        assertThat(result.getStatusCode().value()).isEqualTo(200);
        assertThat(result.getBody()).isEqualTo(response);
        verify(techDebtService).getTechDebtItem(itemId);
    }

    @Test
    void getTechDebtForProject_returnsOkWithPage() {
        PageResponse<TechDebtItemResponse> page = new PageResponse<>(
                List.of(buildTechDebtResponse(itemId)), 0, 20, 1, 1, true);
        when(techDebtService.getTechDebtForProject(eq(projectId), any())).thenReturn(page);

        ResponseEntity<PageResponse<TechDebtItemResponse>> result =
                controller.getTechDebtForProject(projectId, 0, 20);

        assertThat(result.getStatusCode().value()).isEqualTo(200);
        assertThat(result.getBody().content()).hasSize(1);
        verify(techDebtService).getTechDebtForProject(eq(projectId), any());
    }

    @Test
    void getTechDebtByStatus_returnsOkWithPage() {
        PageResponse<TechDebtItemResponse> page = new PageResponse<>(List.of(), 0, 20, 0, 0, true);
        when(techDebtService.getTechDebtByStatus(eq(projectId), eq(DebtStatus.IDENTIFIED), any()))
                .thenReturn(page);

        ResponseEntity<PageResponse<TechDebtItemResponse>> result =
                controller.getTechDebtByStatus(projectId, DebtStatus.IDENTIFIED, 0, 20);

        assertThat(result.getStatusCode().value()).isEqualTo(200);
        verify(techDebtService).getTechDebtByStatus(eq(projectId), eq(DebtStatus.IDENTIFIED), any());
    }

    @Test
    void getTechDebtByCategory_returnsOkWithPage() {
        PageResponse<TechDebtItemResponse> page = new PageResponse<>(List.of(), 0, 20, 0, 0, true);
        when(techDebtService.getTechDebtByCategory(eq(projectId), eq(DebtCategory.CODE), any()))
                .thenReturn(page);

        ResponseEntity<PageResponse<TechDebtItemResponse>> result =
                controller.getTechDebtByCategory(projectId, DebtCategory.CODE, 0, 20);

        assertThat(result.getStatusCode().value()).isEqualTo(200);
        verify(techDebtService).getTechDebtByCategory(eq(projectId), eq(DebtCategory.CODE), any());
    }

    @Test
    void updateTechDebtStatus_returnsOkWithBody() {
        UpdateTechDebtStatusRequest request = new UpdateTechDebtStatusRequest(DebtStatus.RESOLVED, jobId);
        TechDebtItemResponse response = buildTechDebtResponse(itemId);
        when(techDebtService.updateTechDebtStatus(itemId, request)).thenReturn(response);

        ResponseEntity<TechDebtItemResponse> result = controller.updateTechDebtStatus(itemId, request);

        assertThat(result.getStatusCode().value()).isEqualTo(200);
        assertThat(result.getBody()).isEqualTo(response);
        verify(techDebtService).updateTechDebtStatus(itemId, request);
        verify(auditLogService).log(userId, null, "TECH_DEBT_STATUS_UPDATED", "TECH_DEBT", itemId, null);
    }

    @Test
    void deleteTechDebtItem_returnsNoContent() {
        ResponseEntity<Void> result = controller.deleteTechDebtItem(itemId);

        assertThat(result.getStatusCode().value()).isEqualTo(204);
        assertThat(result.getBody()).isNull();
        verify(techDebtService).deleteTechDebtItem(itemId);
        verify(auditLogService).log(userId, null, "TECH_DEBT_DELETED", "TECH_DEBT", itemId, null);
    }

    @Test
    void getDebtSummary_returnsOkWithMap() {
        Map<String, Object> summary = Map.of("totalItems", 15, "resolvedCount", 5);
        when(techDebtService.getDebtSummary(projectId)).thenReturn(summary);

        ResponseEntity<Map<String, Object>> result = controller.getDebtSummary(projectId);

        assertThat(result.getStatusCode().value()).isEqualTo(200);
        assertThat(result.getBody()).containsEntry("totalItems", 15);
        verify(techDebtService).getDebtSummary(projectId);
    }
}
