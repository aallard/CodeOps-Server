package com.codeops.service;

import com.codeops.dto.response.HealthSnapshotResponse;
import com.codeops.dto.response.ProjectMetricsResponse;
import com.codeops.dto.response.TeamMetricsResponse;
import com.codeops.entity.*;
import com.codeops.entity.enums.*;
import com.codeops.repository.*;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MetricsServiceTest {

    @Mock private ProjectRepository projectRepository;
    @Mock private QaJobRepository qaJobRepository;
    @Mock private FindingRepository findingRepository;
    @Mock private TechDebtItemRepository techDebtItemRepository;
    @Mock private DependencyVulnerabilityRepository vulnerabilityRepository;
    @Mock private DependencyScanRepository dependencyScanRepository;
    @Mock private HealthSnapshotRepository healthSnapshotRepository;
    @Mock private TeamMemberRepository teamMemberRepository;

    @InjectMocks
    private MetricsService metricsService;

    private UUID userId;
    private UUID teamId;
    private UUID projectId;
    private Team team;
    private Project project;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        teamId = UUID.randomUUID();
        projectId = UUID.randomUUID();

        team = Team.builder().name("Test Team").build();
        team.setId(teamId);

        project = Project.builder().team(team).name("Test Project").healthScore(85).build();
        project.setId(projectId);
        project.setCreatedAt(Instant.now());

        setSecurityContext(userId);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // --- getProjectMetrics ---

    @Test
    void getProjectMetrics_success() {
        UUID jobId = UUID.randomUUID();
        UUID scanId = UUID.randomUUID();
        QaJob completedJob = QaJob.builder().project(project).status(JobStatus.COMPLETED).totalFindings(10).build();
        completedJob.setId(jobId);
        completedJob.setCreatedAt(Instant.now());

        DependencyScan scan = DependencyScan.builder().project(project).build();
        scan.setId(scanId);

        HealthSnapshot snap1 = HealthSnapshot.builder().project(project).healthScore(85)
                .capturedAt(Instant.now()).build();
        snap1.setId(UUID.randomUUID());
        HealthSnapshot snap2 = HealthSnapshot.builder().project(project).healthScore(80)
                .capturedAt(Instant.now().minusSeconds(3600)).build();
        snap2.setId(UUID.randomUUID());

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(true);
        when(healthSnapshotRepository.findByProjectIdOrderByCapturedAtDesc(projectId))
                .thenReturn(List.of(snap1, snap2));
        when(qaJobRepository.findByProjectIdOrderByCreatedAtDesc(projectId)).thenReturn(List.of(completedJob));
        when(findingRepository.countByJobIdAndSeverityAndStatus(jobId, Severity.CRITICAL, FindingStatus.OPEN)).thenReturn(2L);
        when(findingRepository.countByJobIdAndSeverityAndStatus(jobId, Severity.HIGH, FindingStatus.OPEN)).thenReturn(5L);
        when(techDebtItemRepository.countByProjectIdAndStatus(projectId, DebtStatus.IDENTIFIED)).thenReturn(3L);
        when(techDebtItemRepository.countByProjectIdAndStatus(projectId, DebtStatus.PLANNED)).thenReturn(2L);
        when(techDebtItemRepository.countByProjectIdAndStatus(projectId, DebtStatus.IN_PROGRESS)).thenReturn(1L);
        when(dependencyScanRepository.findFirstByProjectIdOrderByCreatedAtDesc(projectId))
                .thenReturn(Optional.of(scan));
        when(vulnerabilityRepository.countByScanIdAndStatus(scanId, VulnerabilityStatus.OPEN)).thenReturn(4L);

        ProjectMetricsResponse response = metricsService.getProjectMetrics(projectId);

        assertNotNull(response);
        assertEquals(projectId, response.projectId());
        assertEquals("Test Project", response.projectName());
        assertEquals(85, response.currentHealthScore());
        assertEquals(80, response.previousHealthScore());
        assertEquals(1, response.totalJobs());
        assertEquals(10, response.totalFindings());
        assertEquals(2, response.openCritical());
        assertEquals(5, response.openHigh());
        assertEquals(6, response.techDebtItemCount()); // 3 + 2 + 1
        assertEquals(4, response.openVulnerabilities());
    }

    @Test
    void getProjectMetrics_noCompletedJobs_zeroCriticalHigh() {
        QaJob pendingJob = QaJob.builder().project(project).status(JobStatus.PENDING).totalFindings(0).build();
        pendingJob.setId(UUID.randomUUID());
        pendingJob.setCreatedAt(Instant.now());

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(true);
        when(healthSnapshotRepository.findByProjectIdOrderByCapturedAtDesc(projectId)).thenReturn(List.of());
        when(qaJobRepository.findByProjectIdOrderByCreatedAtDesc(projectId)).thenReturn(List.of(pendingJob));
        when(techDebtItemRepository.countByProjectIdAndStatus(eq(projectId), any())).thenReturn(0L);
        when(dependencyScanRepository.findFirstByProjectIdOrderByCreatedAtDesc(projectId)).thenReturn(Optional.empty());

        ProjectMetricsResponse response = metricsService.getProjectMetrics(projectId);

        assertEquals(0, response.openCritical());
        assertEquals(0, response.openHigh());
        assertNull(response.previousHealthScore());
    }

    @Test
    void getProjectMetrics_projectNotFound_throws() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> metricsService.getProjectMetrics(projectId));
    }

    @Test
    void getProjectMetrics_notTeamMember_throws() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> metricsService.getProjectMetrics(projectId));
    }

    @Test
    void getProjectMetrics_nullTotalFindings_treatsAsZero() {
        QaJob jobWithNull = QaJob.builder().project(project).status(JobStatus.RUNNING).totalFindings(null).build();
        jobWithNull.setId(UUID.randomUUID());
        jobWithNull.setCreatedAt(Instant.now());

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(true);
        when(healthSnapshotRepository.findByProjectIdOrderByCapturedAtDesc(projectId)).thenReturn(List.of());
        when(qaJobRepository.findByProjectIdOrderByCreatedAtDesc(projectId)).thenReturn(List.of(jobWithNull));
        when(techDebtItemRepository.countByProjectIdAndStatus(eq(projectId), any())).thenReturn(0L);
        when(dependencyScanRepository.findFirstByProjectIdOrderByCreatedAtDesc(projectId)).thenReturn(Optional.empty());

        ProjectMetricsResponse response = metricsService.getProjectMetrics(projectId);

        assertEquals(0, response.totalFindings());
    }

    // --- getTeamMetrics ---

    @Test
    void getTeamMetrics_success() {
        UUID jobId = UUID.randomUUID();
        QaJob completedJob = QaJob.builder().project(project).status(JobStatus.COMPLETED).totalFindings(5).build();
        completedJob.setId(jobId);
        completedJob.setCreatedAt(Instant.now());

        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(true);
        when(projectRepository.findByTeamIdAndIsArchivedFalse(teamId)).thenReturn(List.of(project));
        when(qaJobRepository.findByProjectIdOrderByCreatedAtDesc(projectId)).thenReturn(List.of(completedJob));
        when(findingRepository.countByJobIdAndSeverityAndStatus(jobId, Severity.CRITICAL, FindingStatus.OPEN)).thenReturn(1L);

        TeamMetricsResponse response = metricsService.getTeamMetrics(teamId);

        assertNotNull(response);
        assertEquals(teamId, response.teamId());
        assertEquals(1, response.totalProjects());
        assertEquals(1, response.totalJobs());
        assertEquals(5, response.totalFindings());
        assertEquals(85.0, response.averageHealthScore());
        assertEquals(0, response.projectsBelowThreshold());
        assertEquals(1, response.openCriticalFindings());
    }

    @Test
    void getTeamMetrics_projectBelowThreshold() {
        project.setHealthScore(60);

        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(true);
        when(projectRepository.findByTeamIdAndIsArchivedFalse(teamId)).thenReturn(List.of(project));
        when(qaJobRepository.findByProjectIdOrderByCreatedAtDesc(projectId)).thenReturn(List.of());

        TeamMetricsResponse response = metricsService.getTeamMetrics(teamId);

        assertEquals(1, response.projectsBelowThreshold());
    }

    @Test
    void getTeamMetrics_noProjects() {
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(true);
        when(projectRepository.findByTeamIdAndIsArchivedFalse(teamId)).thenReturn(List.of());

        TeamMetricsResponse response = metricsService.getTeamMetrics(teamId);

        assertEquals(0, response.totalProjects());
        assertEquals(0, response.totalJobs());
        assertEquals(0.0, response.averageHealthScore());
    }

    @Test
    void getTeamMetrics_notTeamMember_throws() {
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(false);
        assertThrows(AccessDeniedException.class, () -> metricsService.getTeamMetrics(teamId));
    }

    @Test
    void getTeamMetrics_nullHealthScore_excludedFromAverage() {
        project.setHealthScore(null);
        Project project2 = Project.builder().team(team).name("P2").healthScore(90).build();
        project2.setId(UUID.randomUUID());

        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(true);
        when(projectRepository.findByTeamIdAndIsArchivedFalse(teamId)).thenReturn(List.of(project, project2));
        when(qaJobRepository.findByProjectIdOrderByCreatedAtDesc(any())).thenReturn(List.of());

        TeamMetricsResponse response = metricsService.getTeamMetrics(teamId);

        assertEquals(90.0, response.averageHealthScore());
    }

    // --- getHealthTrend ---

    @Test
    void getHealthTrend_success() {
        HealthSnapshot recent = HealthSnapshot.builder().project(project).healthScore(90)
                .capturedAt(Instant.now().minusSeconds(3600)).build();
        recent.setId(UUID.randomUUID());
        HealthSnapshot old = HealthSnapshot.builder().project(project).healthScore(70)
                .capturedAt(Instant.now().minusSeconds(86400 * 60L)).build();
        old.setId(UUID.randomUUID());

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(true);
        when(healthSnapshotRepository.findByProjectIdOrderByCapturedAtDesc(projectId))
                .thenReturn(List.of(recent, old));

        // 30 days - should only include 'recent', not 'old' (60 days ago)
        List<HealthSnapshotResponse> result = metricsService.getHealthTrend(projectId, 30);

        assertEquals(1, result.size());
        assertEquals(90, result.get(0).healthScore());
    }

    @Test
    void getHealthTrend_projectNotFound_throws() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> metricsService.getHealthTrend(projectId, 30));
    }

    @Test
    void getHealthTrend_notTeamMember_throws() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> metricsService.getHealthTrend(projectId, 30));
    }

    private void setSecurityContext(UUID userId) {
        var auth = new UsernamePasswordAuthenticationToken(userId, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
