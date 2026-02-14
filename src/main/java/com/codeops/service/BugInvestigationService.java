package com.codeops.service;

import com.codeops.config.AppConstants;
import com.codeops.dto.request.CreateBugInvestigationRequest;
import com.codeops.dto.request.UpdateBugInvestigationRequest;
import com.codeops.dto.response.BugInvestigationResponse;
import com.codeops.entity.BugInvestigation;
import com.codeops.repository.BugInvestigationRepository;
import com.codeops.repository.QaJobRepository;
import com.codeops.repository.TeamMemberRepository;
import com.codeops.security.SecurityUtils;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class BugInvestigationService {

    private final BugInvestigationRepository bugInvestigationRepository;
    private final QaJobRepository qaJobRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final S3StorageService s3StorageService;

    public BugInvestigationResponse createInvestigation(CreateBugInvestigationRequest request) {
        var job = qaJobRepository.findById(request.jobId())
                .orElseThrow(() -> new EntityNotFoundException("Job not found"));
        verifyTeamMembership(job.getProject().getTeam().getId());

        BugInvestigation investigation = BugInvestigation.builder()
                .job(job)
                .jiraKey(request.jiraKey())
                .jiraSummary(request.jiraSummary())
                .jiraDescription(request.jiraDescription())
                .jiraCommentsJson(request.jiraCommentsJson())
                .jiraAttachmentsJson(request.jiraAttachmentsJson())
                .jiraLinkedIssues(request.jiraLinkedIssues())
                .additionalContext(request.additionalContext())
                .rcaPostedToJira(false)
                .fixTasksCreatedInJira(false)
                .build();

        investigation = bugInvestigationRepository.save(investigation);
        return mapToResponse(investigation);
    }

    @Transactional(readOnly = true)
    public BugInvestigationResponse getInvestigation(UUID investigationId) {
        BugInvestigation investigation = bugInvestigationRepository.findById(investigationId)
                .orElseThrow(() -> new EntityNotFoundException("Bug investigation not found"));
        verifyTeamMembership(investigation.getJob().getProject().getTeam().getId());
        return mapToResponse(investigation);
    }

    @Transactional(readOnly = true)
    public BugInvestigationResponse getInvestigationByJob(UUID jobId) {
        BugInvestigation investigation = bugInvestigationRepository.findByJobId(jobId)
                .orElseThrow(() -> new EntityNotFoundException("Bug investigation not found for job"));
        verifyTeamMembership(investigation.getJob().getProject().getTeam().getId());
        return mapToResponse(investigation);
    }

    @Transactional(readOnly = true)
    public BugInvestigationResponse getInvestigationByJiraKey(String jiraKey) {
        BugInvestigation investigation = bugInvestigationRepository.findByJiraKey(jiraKey)
                .orElseThrow(() -> new EntityNotFoundException("Bug investigation not found for Jira key"));
        verifyTeamMembership(investigation.getJob().getProject().getTeam().getId());
        return mapToResponse(investigation);
    }

    public BugInvestigationResponse updateInvestigation(UUID investigationId, UpdateBugInvestigationRequest request) {
        BugInvestigation investigation = bugInvestigationRepository.findById(investigationId)
                .orElseThrow(() -> new EntityNotFoundException("Bug investigation not found"));
        verifyTeamMembership(investigation.getJob().getProject().getTeam().getId());

        if (request.rcaMd() != null) investigation.setRcaMd(request.rcaMd());
        if (request.impactAssessmentMd() != null) investigation.setImpactAssessmentMd(request.impactAssessmentMd());
        if (request.rcaS3Key() != null) investigation.setRcaS3Key(request.rcaS3Key());
        if (request.rcaPostedToJira() != null) investigation.setRcaPostedToJira(request.rcaPostedToJira());
        if (request.fixTasksCreatedInJira() != null) investigation.setFixTasksCreatedInJira(request.fixTasksCreatedInJira());

        investigation = bugInvestigationRepository.save(investigation);
        return mapToResponse(investigation);
    }

    public String uploadRca(UUID jobId, String rcaMd) {
        String key = AppConstants.S3_REPORTS + jobId + "/rca.md";
        s3StorageService.upload(key, rcaMd.getBytes(StandardCharsets.UTF_8), "text/markdown");
        return key;
    }

    private BugInvestigationResponse mapToResponse(BugInvestigation investigation) {
        return new BugInvestigationResponse(
                investigation.getId(),
                investigation.getJob().getId(),
                investigation.getJiraKey(),
                investigation.getJiraSummary(),
                investigation.getJiraDescription(),
                investigation.getAdditionalContext(),
                investigation.getRcaMd(),
                investigation.getImpactAssessmentMd(),
                investigation.getRcaS3Key(),
                investigation.getRcaPostedToJira(),
                investigation.getFixTasksCreatedInJira(),
                investigation.getCreatedAt()
        );
    }

    private void verifyTeamMembership(UUID teamId) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        if (!teamMemberRepository.existsByTeamIdAndUserId(teamId, currentUserId)) {
            throw new AccessDeniedException("Not a member of this team");
        }
    }
}
