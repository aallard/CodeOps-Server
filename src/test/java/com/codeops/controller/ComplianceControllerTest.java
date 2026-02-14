package com.codeops.controller;

import com.codeops.dto.request.CreateComplianceItemRequest;
import com.codeops.dto.request.CreateSpecificationRequest;
import com.codeops.dto.response.ComplianceItemResponse;
import com.codeops.dto.response.PageResponse;
import com.codeops.dto.response.SpecificationResponse;
import com.codeops.entity.enums.AgentType;
import com.codeops.entity.enums.ComplianceStatus;
import com.codeops.entity.enums.SpecType;
import com.codeops.service.AuditLogService;
import com.codeops.service.ComplianceService;
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
class ComplianceControllerTest {

    @Mock
    private ComplianceService complianceService;

    @Mock
    private AuditLogService auditLogService;

    private ComplianceController controller;

    private final UUID userId = UUID.randomUUID();
    private final UUID jobId = UUID.randomUUID();
    private final UUID specId = UUID.randomUUID();
    private final UUID complianceItemId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        controller = new ComplianceController(complianceService, auditLogService);
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

    private SpecificationResponse buildSpecResponse(UUID id) {
        return new SpecificationResponse(id, jobId, "API Spec", SpecType.OPENAPI,
                "specs/api-spec.yaml", Instant.now());
    }

    private ComplianceItemResponse buildComplianceItemResponse(UUID id) {
        return new ComplianceItemResponse(id, jobId, "All endpoints must have auth",
                specId, "API Spec", ComplianceStatus.MET, "Auth filter present",
                AgentType.API_CONTRACT, null, Instant.now());
    }

    @Test
    void createSpecification_returnsCreatedWithBody() {
        CreateSpecificationRequest request = new CreateSpecificationRequest(jobId, "API Spec",
                SpecType.OPENAPI, "specs/api-spec.yaml");
        SpecificationResponse response = buildSpecResponse(specId);
        when(complianceService.createSpecification(request)).thenReturn(response);

        ResponseEntity<SpecificationResponse> result = controller.createSpecification(request);

        assertThat(result.getStatusCode().value()).isEqualTo(201);
        assertThat(result.getBody()).isEqualTo(response);
        verify(complianceService).createSpecification(request);
        verify(auditLogService).log(userId, null, "SPECIFICATION_CREATED", "SPECIFICATION", specId, null);
    }

    @Test
    void getSpecificationsForJob_returnsOkWithPage() {
        PageResponse<SpecificationResponse> page = new PageResponse<>(
                List.of(buildSpecResponse(specId)), 0, 20, 1, 1, true);
        when(complianceService.getSpecificationsForJob(eq(jobId), any())).thenReturn(page);

        ResponseEntity<PageResponse<SpecificationResponse>> result = controller.getSpecificationsForJob(jobId, 0, 20);

        assertThat(result.getStatusCode().value()).isEqualTo(200);
        assertThat(result.getBody().content()).hasSize(1);
        verify(complianceService).getSpecificationsForJob(eq(jobId), any());
    }

    @Test
    void createComplianceItem_returnsCreatedWithBody() {
        CreateComplianceItemRequest request = new CreateComplianceItemRequest(jobId,
                "All endpoints must have auth", specId, ComplianceStatus.MET,
                "Auth filter present", AgentType.API_CONTRACT, null);
        ComplianceItemResponse response = buildComplianceItemResponse(complianceItemId);
        when(complianceService.createComplianceItem(request)).thenReturn(response);

        ResponseEntity<ComplianceItemResponse> result = controller.createComplianceItem(request);

        assertThat(result.getStatusCode().value()).isEqualTo(201);
        assertThat(result.getBody()).isEqualTo(response);
        verify(complianceService).createComplianceItem(request);
        verify(auditLogService).log(userId, null, "COMPLIANCE_ITEM_CREATED", "COMPLIANCE_ITEM", complianceItemId, null);
    }

    @Test
    void createComplianceItems_batch_returnsCreatedWithList() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        CreateComplianceItemRequest req1 = new CreateComplianceItemRequest(jobId, "Req 1", specId,
                ComplianceStatus.MET, "Evidence", AgentType.API_CONTRACT, null);
        CreateComplianceItemRequest req2 = new CreateComplianceItemRequest(jobId, "Req 2", specId,
                ComplianceStatus.MISSING, null, AgentType.API_CONTRACT, "needs fix");
        List<CreateComplianceItemRequest> requests = List.of(req1, req2);
        List<ComplianceItemResponse> responses = List.of(
                buildComplianceItemResponse(id1), buildComplianceItemResponse(id2));
        when(complianceService.createComplianceItems(requests)).thenReturn(responses);

        ResponseEntity<List<ComplianceItemResponse>> result = controller.createComplianceItems(requests);

        assertThat(result.getStatusCode().value()).isEqualTo(201);
        assertThat(result.getBody()).hasSize(2);
        verify(complianceService).createComplianceItems(requests);
        verify(auditLogService, times(2)).log(eq(userId), isNull(), eq("COMPLIANCE_ITEM_CREATED"),
                eq("COMPLIANCE_ITEM"), any(UUID.class), isNull());
    }

    @Test
    void getComplianceItemsForJob_returnsOkWithPage() {
        PageResponse<ComplianceItemResponse> page = new PageResponse<>(
                List.of(buildComplianceItemResponse(complianceItemId)), 0, 20, 1, 1, true);
        when(complianceService.getComplianceItemsForJob(eq(jobId), any())).thenReturn(page);

        ResponseEntity<PageResponse<ComplianceItemResponse>> result = controller.getComplianceItemsForJob(jobId, 0, 20);

        assertThat(result.getStatusCode().value()).isEqualTo(200);
        assertThat(result.getBody().content()).hasSize(1);
        verify(complianceService).getComplianceItemsForJob(eq(jobId), any());
    }

    @Test
    void getComplianceItemsByStatus_returnsOkWithPage() {
        PageResponse<ComplianceItemResponse> page = new PageResponse<>(List.of(), 0, 20, 0, 0, true);
        when(complianceService.getComplianceItemsByStatus(eq(jobId), eq(ComplianceStatus.MISSING), any()))
                .thenReturn(page);

        ResponseEntity<PageResponse<ComplianceItemResponse>> result =
                controller.getComplianceItemsByStatus(jobId, ComplianceStatus.MISSING, 0, 20);

        assertThat(result.getStatusCode().value()).isEqualTo(200);
        verify(complianceService).getComplianceItemsByStatus(eq(jobId), eq(ComplianceStatus.MISSING), any());
    }

    @Test
    void getComplianceSummary_returnsOkWithMap() {
        Map<String, Object> summary = Map.of("totalItems", 10, "metCount", 8, "missingCount", 2);
        when(complianceService.getComplianceSummary(jobId)).thenReturn(summary);

        ResponseEntity<Map<String, Object>> result = controller.getComplianceSummary(jobId);

        assertThat(result.getStatusCode().value()).isEqualTo(200);
        assertThat(result.getBody()).containsEntry("totalItems", 10);
        verify(complianceService).getComplianceSummary(jobId);
    }
}
