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
            openCritical = (int) findingRepository.findByJobIdAndSeverity(latestJobId, Severity.CRITICAL).stream()
                    .filter(f -> f.getStatus() == FindingStatus.OPEN).count();
            openHigh = (int) findingRepository.findByJobIdAndSeverity(latestJobId, Severity.HIGH).stream()
                    .filter(f -> f.getStatus() == FindingStatus.OPEN).count();
        }

        int techDebtItemCount = (int) techDebtItemRepository.countByProjectIdAndStatus(projectId, DebtStatus.IDENTIFIED)
                + (int) techDebtItemRepository.countByProjectIdAndStatus(projectId, DebtStatus.PLANNED)
                + (int) techDebtItemRepository.countByProjectIdAndStatus(projectId, DebtStatus.IN_PROGRESS);

        int openVulnerabilities = 0;
        var latestScan = dependencyScanRepository.findFirstByProjectIdOrderByCreatedAtDesc(projectId);
        if (latestScan.isPresent()) {
            openVulnerabilities = vulnerabilityRepository.findByScanIdAndStatus(latestScan.get().getId(), VulnerabilityStatus.OPEN).size();
        }

        return new ProjectMetricsResponse(
                projectId, project.getName(), currentHealthScore, previousHealthScore,
                totalJobs, totalFindings, openCritical, openHigh,
                techDebtItemCount, openVulnerabilities, project.getLastAuditAt()
        );
    }

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
                openCriticalFindings += (int) findingRepository.findByJobIdAndSeverity(latestCompleted.get().getId(), Severity.CRITICAL).stream()
                        .filter(f -> f.getStatus() == FindingStatus.OPEN).count();
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
