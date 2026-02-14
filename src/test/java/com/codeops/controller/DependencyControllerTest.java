package com.codeops.controller;

import com.codeops.dto.request.CreateDependencyScanRequest;
import com.codeops.dto.request.CreateVulnerabilityRequest;
import com.codeops.dto.response.DependencyScanResponse;
import com.codeops.dto.response.PageResponse;
import com.codeops.dto.response.VulnerabilityResponse;
import com.codeops.entity.enums.Severity;
import com.codeops.entity.enums.VulnerabilityStatus;
import com.codeops.service.AuditLogService;
import com.codeops.service.DependencyService;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DependencyControllerTest {

    @Mock
    private DependencyService dependencyService;

    @Mock
    private AuditLogService auditLogService;

    private DependencyController controller;

    private final UUID userId = UUID.randomUUID();
    private final UUID scanId = UUID.randomUUID();
    private final UUID projectId = UUID.randomUUID();
    private final UUID jobId = UUID.randomUUID();
    private final UUID vulnerabilityId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        controller = new DependencyController(dependencyService, auditLogService);
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

    private DependencyScanResponse buildScanResponse(UUID id) {
        return new DependencyScanResponse(id, projectId, jobId, "pom.xml",
                50, 5, 3, Instant.now());
    }

    private VulnerabilityResponse buildVulnerabilityResponse(UUID id) {
        return new VulnerabilityResponse(id, scanId, "log4j-core", "2.14.1", "2.17.1",
                "CVE-2021-44228", Severity.CRITICAL, "Remote code execution via JNDI",
                VulnerabilityStatus.OPEN, Instant.now());
    }

    @Test
    void createScan_returnsCreatedWithBody() {
        CreateDependencyScanRequest request = new CreateDependencyScanRequest(projectId, jobId,
                "pom.xml", 50, 5, 3, null);
        DependencyScanResponse response = buildScanResponse(scanId);
        when(dependencyService.createScan(request)).thenReturn(response);

        ResponseEntity<DependencyScanResponse> result = controller.createScan(request);

        assertThat(result.getStatusCode().value()).isEqualTo(201);
        assertThat(result.getBody()).isEqualTo(response);
        verify(dependencyService).createScan(request);
        verify(auditLogService).log(userId, null, "DEPENDENCY_SCAN_CREATED", "DEPENDENCY_SCAN", scanId, null);
    }

    @Test
    void getScan_returnsOkWithBody() {
        DependencyScanResponse response = buildScanResponse(scanId);
        when(dependencyService.getScan(scanId)).thenReturn(response);

        ResponseEntity<DependencyScanResponse> result = controller.getScan(scanId);

        assertThat(result.getStatusCode().value()).isEqualTo(200);
        assertThat(result.getBody()).isEqualTo(response);
        verify(dependencyService).getScan(scanId);
    }

    @Test
    void getScansForProject_returnsOkWithPage() {
        PageResponse<DependencyScanResponse> page = new PageResponse<>(
                List.of(buildScanResponse(scanId)), 0, 20, 1, 1, true);
        when(dependencyService.getScansForProject(eq(projectId), any())).thenReturn(page);

        ResponseEntity<PageResponse<DependencyScanResponse>> result =
                controller.getScansForProject(projectId, 0, 20);

        assertThat(result.getStatusCode().value()).isEqualTo(200);
        assertThat(result.getBody().content()).hasSize(1);
        verify(dependencyService).getScansForProject(eq(projectId), any());
    }

    @Test
    void getLatestScan_returnsOkWithBody() {
        DependencyScanResponse response = buildScanResponse(scanId);
        when(dependencyService.getLatestScan(projectId)).thenReturn(response);

        ResponseEntity<DependencyScanResponse> result = controller.getLatestScan(projectId);

        assertThat(result.getStatusCode().value()).isEqualTo(200);
        assertThat(result.getBody()).isEqualTo(response);
        verify(dependencyService).getLatestScan(projectId);
    }

    @Test
    void addVulnerability_returnsCreatedWithBody() {
        CreateVulnerabilityRequest request = new CreateVulnerabilityRequest(scanId, "log4j-core",
                "2.14.1", "2.17.1", "CVE-2021-44228", Severity.CRITICAL,
                "Remote code execution via JNDI");
        VulnerabilityResponse response = buildVulnerabilityResponse(vulnerabilityId);
        when(dependencyService.addVulnerability(request)).thenReturn(response);

        ResponseEntity<VulnerabilityResponse> result = controller.addVulnerability(request);

        assertThat(result.getStatusCode().value()).isEqualTo(201);
        assertThat(result.getBody()).isEqualTo(response);
        verify(dependencyService).addVulnerability(request);
        verify(auditLogService).log(userId, null, "VULNERABILITY_ADDED", "VULNERABILITY", vulnerabilityId, null);
    }

    @Test
    void addVulnerabilities_batch_returnsCreatedWithList() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        CreateVulnerabilityRequest req1 = new CreateVulnerabilityRequest(scanId, "log4j-core",
                "2.14.1", "2.17.1", "CVE-2021-44228", Severity.CRITICAL, "RCE");
        CreateVulnerabilityRequest req2 = new CreateVulnerabilityRequest(scanId, "jackson-databind",
                "2.12.0", "2.12.7", "CVE-2022-42003", Severity.HIGH, "Deserialization");
        List<CreateVulnerabilityRequest> requests = List.of(req1, req2);
        List<VulnerabilityResponse> responses = List.of(
                buildVulnerabilityResponse(id1), buildVulnerabilityResponse(id2));
        when(dependencyService.addVulnerabilities(requests)).thenReturn(responses);

        ResponseEntity<List<VulnerabilityResponse>> result = controller.addVulnerabilities(requests);

        assertThat(result.getStatusCode().value()).isEqualTo(201);
        assertThat(result.getBody()).hasSize(2);
        verify(dependencyService).addVulnerabilities(requests);
        verify(auditLogService, times(2)).log(eq(userId), isNull(), eq("VULNERABILITY_ADDED"),
                eq("VULNERABILITY"), any(UUID.class), isNull());
    }

    @Test
    void getVulnerabilities_returnsOkWithPage() {
        PageResponse<VulnerabilityResponse> page = new PageResponse<>(
                List.of(buildVulnerabilityResponse(vulnerabilityId)), 0, 20, 1, 1, true);
        when(dependencyService.getVulnerabilities(eq(scanId), any())).thenReturn(page);

        ResponseEntity<PageResponse<VulnerabilityResponse>> result =
                controller.getVulnerabilities(scanId, 0, 20);

        assertThat(result.getStatusCode().value()).isEqualTo(200);
        assertThat(result.getBody().content()).hasSize(1);
        verify(dependencyService).getVulnerabilities(eq(scanId), any());
    }

    @Test
    void getVulnerabilitiesBySeverity_returnsOkWithPage() {
        PageResponse<VulnerabilityResponse> page = new PageResponse<>(List.of(), 0, 20, 0, 0, true);
        when(dependencyService.getVulnerabilitiesBySeverity(eq(scanId), eq(Severity.CRITICAL), any()))
                .thenReturn(page);

        ResponseEntity<PageResponse<VulnerabilityResponse>> result =
                controller.getVulnerabilitiesBySeverity(scanId, Severity.CRITICAL, 0, 20);

        assertThat(result.getStatusCode().value()).isEqualTo(200);
        verify(dependencyService).getVulnerabilitiesBySeverity(eq(scanId), eq(Severity.CRITICAL), any());
    }

    @Test
    void getOpenVulnerabilities_returnsOkWithPage() {
        PageResponse<VulnerabilityResponse> page = new PageResponse<>(List.of(), 0, 20, 0, 0, true);
        when(dependencyService.getOpenVulnerabilities(eq(scanId), any())).thenReturn(page);

        ResponseEntity<PageResponse<VulnerabilityResponse>> result =
                controller.getOpenVulnerabilities(scanId, 0, 20);

        assertThat(result.getStatusCode().value()).isEqualTo(200);
        verify(dependencyService).getOpenVulnerabilities(eq(scanId), any());
    }

    @Test
    void updateVulnerabilityStatus_returnsOkWithBody() {
        VulnerabilityResponse response = buildVulnerabilityResponse(vulnerabilityId);
        when(dependencyService.updateVulnerabilityStatus(vulnerabilityId, VulnerabilityStatus.RESOLVED))
                .thenReturn(response);

        ResponseEntity<VulnerabilityResponse> result =
                controller.updateVulnerabilityStatus(vulnerabilityId, VulnerabilityStatus.RESOLVED);

        assertThat(result.getStatusCode().value()).isEqualTo(200);
        assertThat(result.getBody()).isEqualTo(response);
        verify(dependencyService).updateVulnerabilityStatus(vulnerabilityId, VulnerabilityStatus.RESOLVED);
        verify(auditLogService).log(userId, null, "VULNERABILITY_STATUS_UPDATED", "VULNERABILITY",
                vulnerabilityId, "RESOLVED");
    }
}
