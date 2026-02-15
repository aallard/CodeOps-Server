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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Manages bug investigation records that link Jira issues to root cause analyses (RCAs).
 *
 * <p>Each bug investigation is associated with a QA job and tracks Jira issue metadata,
 * RCA markdown content, impact assessments, and the status of posting results back to Jira.
 * RCA documents can be uploaded to S3 storage for persistent access.</p>
 *
 * @see BugInvestigationRepository
 * @see BugInvestigation
 * @see S3StorageService
 */
@Service
@RequiredArgsConstructor
@Transactional
public class BugInvestigationService {

    private static final Logger log = LoggerFactory.getLogger(BugInvestigationService.class);

    private final BugInvestigationRepository bugInvestigationRepository;
    private final QaJobRepository qaJobRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final S3StorageService s3StorageService;

    /**
     * Creates a new bug investigation linked to a QA job with the provided Jira issue data.
     *
     * <p>Initializes the investigation with {@code rcaPostedToJira=false} and
     * {@code fixTasksCreatedInJira=false}.</p>
     *
     * @param request the creation request containing job ID, Jira key, summary, description, and related metadata
     * @return the newly created bug investigation as a response DTO
     * @throws EntityNotFoundException if the referenced job does not exist
     * @throws AccessDeniedException if the current user is not a member of the job's team
     */
    public BugInvestigationResponse createInvestigation(CreateBugInvestigationRequest request) {
        log.debug("createInvestigation called with jobId={}, jiraKey={}", request.jobId(), request.jiraKey());
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
        log.info("Created bug investigation id={} for jobId={}, jiraKey={}", investigation.getId(), request.jobId(), request.jiraKey());
        return mapToResponse(investigation);
    }

    /**
     * Retrieves a bug investigation by its unique identifier.
     *
     * @param investigationId the UUID of the bug investigation to retrieve
     * @return the bug investigation as a response DTO
     * @throws EntityNotFoundException if no investigation exists with the given ID
     * @throws AccessDeniedException if the current user is not a member of the associated team
     */
    @Transactional(readOnly = true)
    public BugInvestigationResponse getInvestigation(UUID investigationId) {
        log.debug("getInvestigation called with investigationId={}", investigationId);
        BugInvestigation investigation = bugInvestigationRepository.findById(investigationId)
                .orElseThrow(() -> new EntityNotFoundException("Bug investigation not found"));
        verifyTeamMembership(investigation.getJob().getProject().getTeam().getId());
        return mapToResponse(investigation);
    }

    /**
     * Retrieves a bug investigation by its associated QA job ID.
     *
     * @param jobId the UUID of the QA job linked to the investigation
     * @return the bug investigation as a response DTO
     * @throws EntityNotFoundException if no investigation exists for the given job
     * @throws AccessDeniedException if the current user is not a member of the associated team
     */
    @Transactional(readOnly = true)
    public BugInvestigationResponse getInvestigationByJob(UUID jobId) {
        log.debug("getInvestigationByJob called with jobId={}", jobId);
        BugInvestigation investigation = bugInvestigationRepository.findByJobId(jobId)
                .orElseThrow(() -> new EntityNotFoundException("Bug investigation not found for job"));
        verifyTeamMembership(investigation.getJob().getProject().getTeam().getId());
        return mapToResponse(investigation);
    }

    /**
     * Retrieves a bug investigation by its Jira issue key (e.g., "PROJ-123").
     *
     * @param jiraKey the Jira issue key to look up
     * @return the bug investigation as a response DTO
     * @throws EntityNotFoundException if no investigation exists for the given Jira key
     * @throws AccessDeniedException if the current user is not a member of the associated team
     */
    @Transactional(readOnly = true)
    public BugInvestigationResponse getInvestigationByJiraKey(String jiraKey) {
        log.debug("getInvestigationByJiraKey called with jiraKey={}", jiraKey);
        BugInvestigation investigation = bugInvestigationRepository.findByJiraKey(jiraKey)
                .orElseThrow(() -> new EntityNotFoundException("Bug investigation not found for Jira key"));
        verifyTeamMembership(investigation.getJob().getProject().getTeam().getId());
        return mapToResponse(investigation);
    }

    /**
     * Partially updates a bug investigation with the non-null fields from the request.
     *
     * <p>Supports updating the RCA markdown, impact assessment markdown, RCA S3 key,
     * and Jira posting status flags.</p>
     *
     * @param investigationId the UUID of the bug investigation to update
     * @param request         the update request containing fields to modify (null fields are skipped)
     * @return the updated bug investigation as a response DTO
     * @throws EntityNotFoundException if no investigation exists with the given ID
     * @throws AccessDeniedException if the current user is not a member of the associated team
     */
    public BugInvestigationResponse updateInvestigation(UUID investigationId, UpdateBugInvestigationRequest request) {
        log.debug("updateInvestigation called with investigationId={}", investigationId);
        BugInvestigation investigation = bugInvestigationRepository.findById(investigationId)
                .orElseThrow(() -> new EntityNotFoundException("Bug investigation not found"));
        verifyTeamMembership(investigation.getJob().getProject().getTeam().getId());

        if (request.rcaMd() != null) investigation.setRcaMd(request.rcaMd());
        if (request.impactAssessmentMd() != null) investigation.setImpactAssessmentMd(request.impactAssessmentMd());
        if (request.rcaS3Key() != null) investigation.setRcaS3Key(request.rcaS3Key());
        if (request.rcaPostedToJira() != null) investigation.setRcaPostedToJira(request.rcaPostedToJira());
        if (request.fixTasksCreatedInJira() != null) investigation.setFixTasksCreatedInJira(request.fixTasksCreatedInJira());

        investigation = bugInvestigationRepository.save(investigation);
        log.info("Updated bug investigation id={}", investigationId);
        return mapToResponse(investigation);
    }

    /**
     * Uploads RCA markdown content to S3 storage under the job's report directory.
     *
     * <p>The file is stored at the key pattern {@code reports/{jobId}/rca.md}
     * with content type {@code text/markdown}.</p>
     *
     * @param jobId the UUID of the QA job to associate the RCA with
     * @param rcaMd the root cause analysis content in markdown format
     * @return the S3 storage key where the RCA was uploaded
     */
    public String uploadRca(UUID jobId, String rcaMd) {
        log.debug("uploadRca called with jobId={}", jobId);
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
