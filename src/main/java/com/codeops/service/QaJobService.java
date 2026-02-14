package com.codeops.service;

import com.codeops.dto.request.CreateJobRequest;
import com.codeops.dto.request.UpdateJobRequest;
import com.codeops.dto.response.JobResponse;
import com.codeops.dto.response.JobSummaryResponse;
import com.codeops.dto.response.PageResponse;
import com.codeops.entity.QaJob;
import com.codeops.entity.TeamMember;
import com.codeops.entity.enums.JobStatus;
import com.codeops.entity.enums.TeamRole;
import com.codeops.repository.*;
import com.codeops.security.SecurityUtils;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class QaJobService {

    private final QaJobRepository qaJobRepository;
    private final AgentRunRepository agentRunRepository;
    private final FindingRepository findingRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final ProjectService projectService;

    public JobResponse createJob(CreateJobRequest request) {
        var project = projectRepository.findById(request.projectId())
                .orElseThrow(() -> new EntityNotFoundException("Project not found"));
        verifyTeamMembership(project.getTeam().getId());

        QaJob job = QaJob.builder()
                .project(project)
                .mode(request.mode())
                .status(JobStatus.PENDING)
                .name(request.name())
                .branch(request.branch())
                .configJson(request.configJson())
                .jiraTicketKey(request.jiraTicketKey())
                .totalFindings(0)
                .criticalCount(0)
                .highCount(0)
                .mediumCount(0)
                .lowCount(0)
                .startedBy(userRepository.getReferenceById(SecurityUtils.getCurrentUserId()))
                .build();

        job = qaJobRepository.save(job);
        return mapToJobResponse(job);
    }

    @Transactional(readOnly = true)
    public JobResponse getJob(UUID jobId) {
        QaJob job = qaJobRepository.findById(jobId)
                .orElseThrow(() -> new EntityNotFoundException("Job not found"));
        verifyTeamMembership(job.getProject().getTeam().getId());
        return mapToJobResponse(job);
    }

    @Transactional(readOnly = true)
    public PageResponse<JobSummaryResponse> getJobsForProject(UUID projectId, Pageable pageable) {
        var project = projectRepository.findById(projectId)
                .orElseThrow(() -> new EntityNotFoundException("Project not found"));
        verifyTeamMembership(project.getTeam().getId());

        Page<QaJob> page = qaJobRepository.findByProjectId(projectId, pageable);
        List<JobSummaryResponse> content = page.getContent().stream()
                .map(this::mapToJobSummaryResponse)
                .toList();
        return new PageResponse<>(content, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages(), page.isLast());
    }

    @Transactional(readOnly = true)
    public List<JobSummaryResponse> getJobsByUser(UUID userId) {
        return qaJobRepository.findByStartedById(userId).stream()
                .map(this::mapToJobSummaryResponse)
                .toList();
    }

    public JobResponse updateJob(UUID jobId, UpdateJobRequest request) {
        QaJob job = qaJobRepository.findById(jobId)
                .orElseThrow(() -> new EntityNotFoundException("Job not found"));
        verifyTeamMembership(job.getProject().getTeam().getId());

        if (request.status() != null) job.setStatus(request.status());
        if (request.summaryMd() != null) job.setSummaryMd(request.summaryMd());
        if (request.overallResult() != null) job.setOverallResult(request.overallResult());
        if (request.healthScore() != null) job.setHealthScore(request.healthScore());
        if (request.totalFindings() != null) job.setTotalFindings(request.totalFindings());
        if (request.criticalCount() != null) job.setCriticalCount(request.criticalCount());
        if (request.highCount() != null) job.setHighCount(request.highCount());
        if (request.mediumCount() != null) job.setMediumCount(request.mediumCount());
        if (request.lowCount() != null) job.setLowCount(request.lowCount());
        if (request.completedAt() != null) job.setCompletedAt(request.completedAt());
        if (request.startedAt() != null) job.setStartedAt(request.startedAt());

        if (request.status() == JobStatus.COMPLETED && request.healthScore() != null) {
            projectService.updateHealthScore(job.getProject().getId(), request.healthScore());
        }

        job = qaJobRepository.save(job);
        return mapToJobResponse(job);
    }

    public void deleteJob(UUID jobId) {
        QaJob job = qaJobRepository.findById(jobId)
                .orElseThrow(() -> new EntityNotFoundException("Job not found"));
        verifyTeamAdmin(job.getProject().getTeam().getId());
        qaJobRepository.delete(job);
    }

    private JobResponse mapToJobResponse(QaJob job) {
        return new JobResponse(
                job.getId(),
                job.getProject().getId(),
                job.getProject().getName(),
                job.getMode(),
                job.getStatus(),
                job.getName(),
                job.getBranch(),
                job.getConfigJson(),
                job.getSummaryMd(),
                job.getOverallResult(),
                job.getHealthScore(),
                job.getTotalFindings(),
                job.getCriticalCount(),
                job.getHighCount(),
                job.getMediumCount(),
                job.getLowCount(),
                job.getJiraTicketKey(),
                job.getStartedBy().getId(),
                job.getStartedBy().getDisplayName(),
                job.getStartedAt(),
                job.getCompletedAt(),
                job.getCreatedAt()
        );
    }

    private JobSummaryResponse mapToJobSummaryResponse(QaJob job) {
        return new JobSummaryResponse(
                job.getId(),
                job.getProject().getName(),
                job.getMode(),
                job.getStatus(),
                job.getName(),
                job.getOverallResult(),
                job.getHealthScore(),
                job.getTotalFindings(),
                job.getCriticalCount(),
                job.getCompletedAt(),
                job.getCreatedAt()
        );
    }

    private void verifyTeamMembership(UUID teamId) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        if (!teamMemberRepository.existsByTeamIdAndUserId(teamId, currentUserId)) {
            throw new AccessDeniedException("Not a member of this team");
        }
    }

    private void verifyTeamAdmin(UUID teamId) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        TeamMember member = teamMemberRepository.findByTeamIdAndUserId(teamId, currentUserId)
                .orElseThrow(() -> new AccessDeniedException("Not a member of this team"));
        if (member.getRole() != TeamRole.OWNER && member.getRole() != TeamRole.ADMIN) {
            throw new AccessDeniedException("Requires OWNER or ADMIN role");
        }
    }
}
