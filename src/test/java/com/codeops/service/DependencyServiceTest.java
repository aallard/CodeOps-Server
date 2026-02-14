package com.codeops.service;

import com.codeops.dto.request.CreateDependencyScanRequest;
import com.codeops.dto.request.CreateVulnerabilityRequest;
import com.codeops.dto.response.DependencyScanResponse;
import com.codeops.dto.response.PageResponse;
import com.codeops.dto.response.VulnerabilityResponse;
import com.codeops.entity.*;
import com.codeops.entity.enums.Severity;
import com.codeops.entity.enums.VulnerabilityStatus;
import com.codeops.repository.*;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DependencyServiceTest {

    @Mock private DependencyScanRepository dependencyScanRepository;
    @Mock private DependencyVulnerabilityRepository vulnerabilityRepository;
    @Mock private ProjectRepository projectRepository;
    @Mock private TeamMemberRepository teamMemberRepository;
    @Mock private QaJobRepository qaJobRepository;

    @InjectMocks
    private DependencyService dependencyService;

    private UUID userId;
    private UUID teamId;
    private UUID projectId;
    private UUID scanId;
    private UUID vulnId;
    private UUID jobId;
    private Team team;
    private Project project;
    private QaJob qaJob;
    private DependencyScan scan;
    private DependencyVulnerability vulnerability;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        teamId = UUID.randomUUID();
        projectId = UUID.randomUUID();
        scanId = UUID.randomUUID();
        vulnId = UUID.randomUUID();
        jobId = UUID.randomUUID();

        team = Team.builder().name("Test Team").build();
        team.setId(teamId);

        project = Project.builder().team(team).name("Test Project").build();
        project.setId(projectId);

        qaJob = QaJob.builder().project(project).build();
        qaJob.setId(jobId);

        scan = DependencyScan.builder()
                .project(project)
                .manifestFile("pom.xml")
                .totalDependencies(50)
                .outdatedCount(5)
                .vulnerableCount(2)
                .build();
        scan.setId(scanId);
        scan.setCreatedAt(Instant.now());

        vulnerability = DependencyVulnerability.builder()
                .scan(scan)
                .dependencyName("log4j-core")
                .currentVersion("2.14.0")
                .fixedVersion("2.17.1")
                .cveId("CVE-2021-44228")
                .severity(Severity.CRITICAL)
                .description("Log4Shell RCE vulnerability")
                .status(VulnerabilityStatus.OPEN)
                .build();
        vulnerability.setId(vulnId);
        vulnerability.setCreatedAt(Instant.now());

        setSecurityContext(userId);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // --- createScan ---

    @Test
    void createScan_success() {
        CreateDependencyScanRequest request = new CreateDependencyScanRequest(
                projectId, null, "pom.xml", 50, 5, 2, "{\"data\": []}");

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(true);
        when(dependencyScanRepository.save(any(DependencyScan.class))).thenAnswer(inv -> {
            DependencyScan s = inv.getArgument(0);
            s.setId(scanId);
            s.setCreatedAt(Instant.now());
            return s;
        });

        DependencyScanResponse response = dependencyService.createScan(request);

        assertNotNull(response);
        assertEquals("pom.xml", response.manifestFile());
        assertEquals(50, response.totalDependencies());
        assertEquals(5, response.outdatedCount());
        assertEquals(2, response.vulnerableCount());
        assertNull(response.jobId());
    }

    @Test
    void createScan_withJob_success() {
        CreateDependencyScanRequest request = new CreateDependencyScanRequest(
                projectId, jobId, "pom.xml", 50, 5, 2, null);

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(true);
        when(qaJobRepository.findById(jobId)).thenReturn(Optional.of(qaJob));
        when(dependencyScanRepository.save(any(DependencyScan.class))).thenAnswer(inv -> {
            DependencyScan s = inv.getArgument(0);
            s.setId(scanId);
            s.setCreatedAt(Instant.now());
            return s;
        });

        DependencyScanResponse response = dependencyService.createScan(request);

        assertEquals(jobId, response.jobId());
    }

    @Test
    void createScan_projectNotFound_throws() {
        CreateDependencyScanRequest request = new CreateDependencyScanRequest(
                projectId, null, "pom.xml", 50, 5, 2, null);

        when(projectRepository.findById(projectId)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> dependencyService.createScan(request));
    }

    @Test
    void createScan_notTeamMember_throws() {
        CreateDependencyScanRequest request = new CreateDependencyScanRequest(
                projectId, null, "pom.xml", 50, 5, 2, null);

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> dependencyService.createScan(request));
    }

    // --- getScan ---

    @Test
    void getScan_success() {
        when(dependencyScanRepository.findById(scanId)).thenReturn(Optional.of(scan));
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(true);

        DependencyScanResponse response = dependencyService.getScan(scanId);

        assertNotNull(response);
        assertEquals(scanId, response.id());
    }

    @Test
    void getScan_notFound_throws() {
        when(dependencyScanRepository.findById(scanId)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> dependencyService.getScan(scanId));
    }

    // --- getScansForProject ---

    @Test
    void getScansForProject_success() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<DependencyScan> page = new PageImpl<>(List.of(scan), pageable, 1);

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(true);
        when(dependencyScanRepository.findByProjectId(projectId, pageable)).thenReturn(page);

        PageResponse<DependencyScanResponse> result = dependencyService.getScansForProject(projectId, pageable);

        assertEquals(1, result.content().size());
    }

    // --- getLatestScan ---

    @Test
    void getLatestScan_success() {
        when(dependencyScanRepository.findFirstByProjectIdOrderByCreatedAtDesc(projectId))
                .thenReturn(Optional.of(scan));

        DependencyScanResponse response = dependencyService.getLatestScan(projectId);

        assertNotNull(response);
        assertEquals(scanId, response.id());
    }

    @Test
    void getLatestScan_notFound_throws() {
        when(dependencyScanRepository.findFirstByProjectIdOrderByCreatedAtDesc(projectId))
                .thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> dependencyService.getLatestScan(projectId));
    }

    // --- addVulnerability ---

    @Test
    void addVulnerability_success() {
        CreateVulnerabilityRequest request = new CreateVulnerabilityRequest(
                scanId, "log4j-core", "2.14.0", "2.17.1", "CVE-2021-44228",
                Severity.CRITICAL, "Log4Shell RCE");

        when(dependencyScanRepository.findById(scanId)).thenReturn(Optional.of(scan));
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(true);
        when(vulnerabilityRepository.save(any(DependencyVulnerability.class))).thenAnswer(inv -> {
            DependencyVulnerability v = inv.getArgument(0);
            v.setId(vulnId);
            v.setCreatedAt(Instant.now());
            return v;
        });

        VulnerabilityResponse response = dependencyService.addVulnerability(request);

        assertNotNull(response);
        assertEquals("log4j-core", response.dependencyName());
        assertEquals(Severity.CRITICAL, response.severity());
        assertEquals(VulnerabilityStatus.OPEN, response.status());
    }

    @Test
    void addVulnerability_scanNotFound_throws() {
        CreateVulnerabilityRequest request = new CreateVulnerabilityRequest(
                scanId, "log4j-core", "2.14.0", "2.17.1", "CVE-2021-44228",
                Severity.CRITICAL, "desc");

        when(dependencyScanRepository.findById(scanId)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> dependencyService.addVulnerability(request));
    }

    // --- addVulnerabilities ---

    @Test
    void addVulnerabilities_emptyList_returnsEmpty() {
        List<VulnerabilityResponse> result = dependencyService.addVulnerabilities(List.of());
        assertTrue(result.isEmpty());
    }

    @Test
    void addVulnerabilities_success() {
        CreateVulnerabilityRequest req1 = new CreateVulnerabilityRequest(
                scanId, "log4j-core", "2.14.0", "2.17.1", "CVE-2021-44228", Severity.CRITICAL, "desc1");
        CreateVulnerabilityRequest req2 = new CreateVulnerabilityRequest(
                scanId, "spring-core", "5.3.1", "5.3.18", "CVE-2022-22965", Severity.HIGH, "desc2");

        when(dependencyScanRepository.findById(scanId)).thenReturn(Optional.of(scan));
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(true);
        when(vulnerabilityRepository.saveAll(anyList())).thenAnswer(inv -> {
            List<DependencyVulnerability> vulns = inv.getArgument(0);
            for (DependencyVulnerability v : vulns) {
                v.setId(UUID.randomUUID());
                v.setCreatedAt(Instant.now());
            }
            return vulns;
        });

        List<VulnerabilityResponse> responses = dependencyService.addVulnerabilities(List.of(req1, req2));
        assertEquals(2, responses.size());
    }

    @Test
    void addVulnerabilities_differentScans_throws() {
        UUID otherScanId = UUID.randomUUID();
        CreateVulnerabilityRequest req1 = new CreateVulnerabilityRequest(
                scanId, "dep1", "1.0", "2.0", null, Severity.LOW, null);
        CreateVulnerabilityRequest req2 = new CreateVulnerabilityRequest(
                otherScanId, "dep2", "1.0", "2.0", null, Severity.LOW, null);

        assertThrows(IllegalArgumentException.class,
                () -> dependencyService.addVulnerabilities(List.of(req1, req2)));
    }

    // --- getVulnerabilities ---

    @Test
    void getVulnerabilities_success() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<DependencyVulnerability> page = new PageImpl<>(List.of(vulnerability), pageable, 1);

        when(dependencyScanRepository.findById(scanId)).thenReturn(Optional.of(scan));
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(true);
        when(vulnerabilityRepository.findByScanId(scanId, pageable)).thenReturn(page);

        PageResponse<VulnerabilityResponse> result = dependencyService.getVulnerabilities(scanId, pageable);

        assertEquals(1, result.content().size());
    }

    // --- getVulnerabilitiesBySeverity ---

    @Test
    void getVulnerabilitiesBySeverity_success() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<DependencyVulnerability> page = new PageImpl<>(List.of(vulnerability), pageable, 1);

        when(vulnerabilityRepository.findByScanIdAndSeverity(scanId, Severity.CRITICAL, pageable)).thenReturn(page);

        PageResponse<VulnerabilityResponse> result =
                dependencyService.getVulnerabilitiesBySeverity(scanId, Severity.CRITICAL, pageable);

        assertEquals(1, result.content().size());
        assertEquals(Severity.CRITICAL, result.content().get(0).severity());
    }

    // --- getOpenVulnerabilities ---

    @Test
    void getOpenVulnerabilities_success() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<DependencyVulnerability> page = new PageImpl<>(List.of(vulnerability), pageable, 1);

        when(vulnerabilityRepository.findByScanIdAndStatus(scanId, VulnerabilityStatus.OPEN, pageable)).thenReturn(page);

        PageResponse<VulnerabilityResponse> result = dependencyService.getOpenVulnerabilities(scanId, pageable);

        assertEquals(1, result.content().size());
        assertEquals(VulnerabilityStatus.OPEN, result.content().get(0).status());
    }

    // --- updateVulnerabilityStatus ---

    @Test
    void updateVulnerabilityStatus_success() {
        when(vulnerabilityRepository.findById(vulnId)).thenReturn(Optional.of(vulnerability));
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(true);
        when(vulnerabilityRepository.save(any(DependencyVulnerability.class))).thenReturn(vulnerability);

        VulnerabilityResponse response = dependencyService.updateVulnerabilityStatus(vulnId, VulnerabilityStatus.RESOLVED);

        assertEquals(VulnerabilityStatus.RESOLVED, vulnerability.getStatus());
    }

    @Test
    void updateVulnerabilityStatus_notFound_throws() {
        when(vulnerabilityRepository.findById(vulnId)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class,
                () -> dependencyService.updateVulnerabilityStatus(vulnId, VulnerabilityStatus.RESOLVED));
    }

    @Test
    void updateVulnerabilityStatus_notTeamMember_throws() {
        when(vulnerabilityRepository.findById(vulnId)).thenReturn(Optional.of(vulnerability));
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(false);

        assertThrows(AccessDeniedException.class,
                () -> dependencyService.updateVulnerabilityStatus(vulnId, VulnerabilityStatus.SUPPRESSED));
    }

    // --- mapScanToResponse with null counts ---

    @Test
    void getScan_nullCounts_defaultsToZero() {
        scan.setTotalDependencies(null);
        scan.setOutdatedCount(null);
        scan.setVulnerableCount(null);

        when(dependencyScanRepository.findById(scanId)).thenReturn(Optional.of(scan));
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(true);

        DependencyScanResponse response = dependencyService.getScan(scanId);

        assertEquals(0, response.totalDependencies());
        assertEquals(0, response.outdatedCount());
        assertEquals(0, response.vulnerableCount());
    }

    private void setSecurityContext(UUID userId) {
        var auth = new UsernamePasswordAuthenticationToken(userId, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
