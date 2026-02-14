package com.codeops.service;

import com.codeops.dto.response.HealthSnapshotResponse;
import com.codeops.dto.response.ProjectMetricsResponse;
import com.codeops.dto.response.TeamMetricsResponse;
import com.codeops.entity.HealthSnapshot;
import com.codeops.entity.Project;
import com.codeops.entity.QaJob;
import com.codeops.entity.enums.*;
import com.codeops.repository.*;
import com.codeops.security.SecurityUtils;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * Provides aggregated quality metrics at the project and team levels.
 *
 * <p>Computes metrics by combining data from QA jobs, findings, tech debt items,
 * dependency scans, vulnerability records, and health snapshots. All read operations
 * verify team membership before returning data.</p>
 *
 * @see MetricsController
 * @see ProjectMetricsResponse
 * @see TeamMetricsResponse
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MetricsService {

    private final ProjectRepository projectRepository;
    private final QaJobRepository qaJobRepository;
    private final FindingRepository findingRepository;
    private final TechDebtItemRepository techDebtItemRepository;
    private final DependencyVulnerabilityRepository vulnerabilityRepository;
    private final DependencyScanRepository dependencyScanRepository;
    private final HealthSnapshotRepository healthSnapshotRepository;
    private final TeamMemberRepository teamMemberRepository;

    /**
     * Computes aggregated quality metrics for a single project.
     *
     * <p>Metrics include the current and previous health scores, total QA jobs run,
     * total findings across all jobs, open critical and high findings from the latest
     * completed job, active tech debt item count, and open vulnerability count from
     * the latest dependency scan.</p>
     *
     * @param projectId the ID of the project whose metrics to compute
     * @return the aggregated project metrics as a response DTO
     * @throws EntityNotFoundException if the project is not found
     * @throws AccessDeniedException if the current user is not a member of the project's team
     */
    public ProjectMetricsResponse getProjectMetrics(UUID projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new EntityNotFoundException("Project not found"));
        verifyTeamMembership(project.getTeam().getId());

        Integer currentHealthScore = project.getHealthScore();
        Integer previousHealthScore = null;
        List<HealthSnapshot> snapshots = healthSnapshotRepository.findByProjectIdOrderByCapturedAtDesc(projectId);
        if (snapshots.size() > 1) {
            previousHealthScore = snapshots.get(1).getHealthScore();
        }

        List<QaJob> jobs = qaJobRepository.findByProjectIdOrderByCreatedAtDesc(projectId);
        int totalJobs = jobs.size();
        int totalFindings = jobs.stream()
                .mapToInt(j -> j.getTotalFindings() != null ? j.getTotalFindings() : 0).sum();

        int openCritical = 0;
        int openHigh = 0;
        var latestCompleted = jobs.stream()
                .filter(j -> j.getStatus() == JobStatus.COMPLETED)
                .findFirst();
        if (latestCompleted.isPresent()) {
            UUID latestJobId = latestCompleted.get().getId();
            openCritical = (int) findingRepository.countByJobIdAndSeverityAndStatus(latestJobId, Severity.CRITICAL, FindingStatus.OPEN);
            openHigh = (int) findingRepository.countByJobIdAndSeverityAndStatus(latestJobId, Severity.HIGH, FindingStatus.OPEN);
        }

        int techDebtItemCount = (int) techDebtItemRepository.countByProjectIdAndStatus(projectId, DebtStatus.IDENTIFIED)
                + (int) techDebtItemRepository.countByProjectIdAndStatus(projectId, DebtStatus.PLANNED)
                + (int) techDebtItemRepository.countByProjectIdAndStatus(projectId, DebtStatus.IN_PROGRESS);

        int openVulnerabilities = 0;
        var latestScan = dependencyScanRepository.findFirstByProjectIdOrderByCreatedAtDesc(projectId);
        if (latestScan.isPresent()) {
            openVulnerabilities = (int) vulnerabilityRepository.countByScanIdAndStatus(latestScan.get().getId(), VulnerabilityStatus.OPEN);
        }

        return new ProjectMetricsResponse(
                projectId, project.getName(), currentHealthScore, previousHealthScore,
                totalJobs, totalFindings, openCritical, openHigh,
                techDebtItemCount, openVulnerabilities, project.getLastAuditAt()
        );
    }

    /**
     * Computes aggregated quality metrics across all non-archived projects in a team.
     *
     * <p>Metrics include total project count, total QA jobs, total findings, average
     * health score, count of projects below the health threshold (score &lt; 70), and
     * open critical findings across all projects.</p>
     *
     * @param teamId the ID of the team whose metrics to compute
     * @return the aggregated team metrics as a response DTO
     * @throws AccessDeniedException if the current user is not a member of the team
     */
    public TeamMetricsResponse getTeamMetrics(UUID teamId) {
        verifyTeamMembership(teamId);

        List<Project> projects = projectRepository.findByTeamIdAndIsArchivedFalse(teamId);
        int totalProjects = projects.size();
        int totalJobs = 0;
        int totalFindings = 0;
        int openCriticalFindings = 0;

        for (Project project : projects) {
            List<QaJob> jobs = qaJobRepository.findByProjectIdOrderByCreatedAtDesc(project.getId());
            totalJobs += jobs.size();
            totalFindings += jobs.stream()
                    .mapToInt(j -> j.getTotalFindings() != null ? j.getTotalFindings() : 0).sum();
            var latestCompleted = jobs.stream()
                    .filter(j -> j.getStatus() == JobStatus.COMPLETED)
                    .findFirst();
            if (latestCompleted.isPresent()) {
                openCriticalFindings += (int) findingRepository.countByJobIdAndSeverityAndStatus(latestCompleted.get().getId(), Severity.CRITICAL, FindingStatus.OPEN);
            }
        }

        double averageHealthScore = projects.stream()
                .filter(p -> p.getHealthScore() != null)
                .mapToInt(Project::getHealthScore)
                .average().orElse(0.0);

        int projectsBelowThreshold = (int) projects.stream()
                .filter(p -> p.getHealthScore() != null && p.getHealthScore() < 70).count();

        return new TeamMetricsResponse(
                teamId, totalProjects, totalJobs, totalFindings,
                averageHealthScore, projectsBelowThreshold, openCriticalFindings
        );
    }

    /**
     * Retrieves the health trend for a project over a specified number of days.
     *
     * <p>Returns health snapshots captured within the given time window, sorted
     * in ascending chronological order (oldest first) for trend visualization.</p>
     *
     * @param projectId the ID of the project whose health trend to retrieve
     * @param days the number of days to look back from the current instant
     * @return a list of health snapshot response DTOs ordered from oldest to newest
     * @throws EntityNotFoundException if the project is not found
     * @throws AccessDeniedException if the current user is not a member of the project's team
     */
    public List<HealthSnapshotResponse> getHealthTrend(UUID projectId, int days) {
        var project = projectRepository.findById(projectId)
                .orElseThrow(() -> new EntityNotFoundException("Project not found"));
        verifyTeamMembership(project.getTeam().getId());

        Instant cutoff = Instant.now().minus(days, ChronoUnit.DAYS);
        return healthSnapshotRepository.findByProjectIdOrderByCapturedAtDesc(projectId).stream()
                .filter(s -> s.getCapturedAt().isAfter(cutoff))
                .sorted((a, b) -> a.getCapturedAt().compareTo(b.getCapturedAt()))
                .map(this::mapSnapshotToResponse)
                .toList();
    }

    private HealthSnapshotResponse mapSnapshotToResponse(HealthSnapshot snapshot) {
        return new HealthSnapshotResponse(
                snapshot.getId(),
                snapshot.getProject().getId(),
                snapshot.getJob() != null ? snapshot.getJob().getId() : null,
                snapshot.getHealthScore(),
                snapshot.getFindingsBySeverity(),
                snapshot.getTechDebtScore(),
                snapshot.getDependencyScore(),
                snapshot.getTestCoveragePercent(),
                snapshot.getCapturedAt()
        );
    }

    private void verifyTeamMembership(UUID teamId) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        if (!teamMemberRepository.existsByTeamIdAndUserId(teamId, currentUserId)) {
            throw new AccessDeniedException("Not a member of this team");
        }
    }
}
