package com.codeops.service;

import com.codeops.dto.request.BulkUpdateFindingsRequest;
import com.codeops.dto.request.CreateFindingRequest;
import com.codeops.dto.request.UpdateFindingStatusRequest;
import com.codeops.dto.response.FindingResponse;
import com.codeops.dto.response.PageResponse;
import com.codeops.entity.Finding;
import com.codeops.entity.enums.AgentType;
import com.codeops.entity.enums.FindingStatus;
import com.codeops.entity.enums.Severity;
import com.codeops.repository.FindingRepository;
import com.codeops.repository.QaJobRepository;
import com.codeops.repository.TeamMemberRepository;
import com.codeops.repository.UserRepository;
import com.codeops.security.SecurityUtils;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Manages QA findings (code issues, vulnerabilities, tech debt) discovered by analysis agents during QA jobs.
 *
 * <p>Findings are created by agents with severity levels and can be filtered by job, severity,
 * agent type, or status. Status updates record the user who changed the status and a timestamp.
 * Bulk status updates are supported for batch triage operations. All operations verify team
 * membership through the job's project association.</p>
 *
 * @see FindingController
 * @see FindingRepository
 * @see Finding
 */
@Service
@RequiredArgsConstructor
@Transactional
public class FindingService {

    private static final Logger log = LoggerFactory.getLogger(FindingService.class);

    private final FindingRepository findingRepository;
    private final QaJobRepository qaJobRepository;
    private final UserRepository userRepository;
    private final TeamMemberRepository teamMemberRepository;

    /**
     * Creates a single finding for a QA job with initial status {@link FindingStatus#OPEN}.
     *
     * @param request the creation request containing job ID, agent type, severity, title, description,
     *                file path, line number, recommendation, evidence, effort estimate, and debt category
     * @return the newly created finding as a response DTO
     * @throws EntityNotFoundException if the referenced job does not exist
     * @throws AccessDeniedException if the current user is not a member of the job's team
     */
    public FindingResponse createFinding(CreateFindingRequest request) {
        log.debug("createFinding called with jobId={}, severity={}", request.jobId(), request.severity());
        var job = qaJobRepository.findById(request.jobId())
                .orElseThrow(() -> new EntityNotFoundException("Job not found"));
        verifyTeamMembership(job.getProject().getTeam().getId());

        Finding finding = Finding.builder()
                .job(job)
                .agentType(request.agentType())
                .severity(request.severity())
                .title(request.title())
                .description(request.description())
                .filePath(request.filePath())
                .lineNumber(request.lineNumber())
                .recommendation(request.recommendation())
                .evidence(request.evidence())
                .effortEstimate(request.effortEstimate())
                .debtCategory(request.debtCategory())
                .status(FindingStatus.OPEN)
                .build();

        finding = findingRepository.save(finding);
        log.info("Finding created: findingId={}, jobId={}, severity={}, agentType={}", finding.getId(), request.jobId(), request.severity(), request.agentType());
        return mapToResponse(finding);
    }

    /**
     * Creates multiple findings in a single batch. All findings must belong to the same QA job.
     *
     * <p>Each finding is initialized with {@link FindingStatus#OPEN} status.</p>
     *
     * @param requests the list of creation requests; must all reference the same job ID
     * @return a list of the newly created findings as response DTOs, or an empty list if input is empty
     * @throws IllegalArgumentException if the requests reference different job IDs
     * @throws EntityNotFoundException if the referenced job does not exist
     * @throws AccessDeniedException if the current user is not a member of the job's team
     */
    public List<FindingResponse> createFindings(List<CreateFindingRequest> requests) {
        log.debug("createFindings called with count={}", requests.size());
        if (requests.isEmpty()) return List.of();

        UUID firstJobId = requests.get(0).jobId();
        boolean allSameJob = requests.stream().allMatch(r -> r.jobId().equals(firstJobId));
        if (!allSameJob) {
            throw new IllegalArgumentException("All findings must belong to the same job");
        }

        var job = qaJobRepository.findById(firstJobId)
                .orElseThrow(() -> new EntityNotFoundException("Job not found"));
        verifyTeamMembership(job.getProject().getTeam().getId());

        List<Finding> findings = requests.stream()
                .map(request -> Finding.builder()
                        .job(job)
                        .agentType(request.agentType())
                        .severity(request.severity())
                        .title(request.title())
                        .description(request.description())
                        .filePath(request.filePath())
                        .lineNumber(request.lineNumber())
                        .recommendation(request.recommendation())
                        .evidence(request.evidence())
                        .effortEstimate(request.effortEstimate())
                        .debtCategory(request.debtCategory())
                        .status(FindingStatus.OPEN)
                        .build())
                .toList();

        findings = findingRepository.saveAll(findings);
        log.info("Bulk findings created: count={}, jobId={}", findings.size(), firstJobId);
        return findings.stream().map(this::mapToResponse).toList();
    }

    /**
     * Retrieves a single finding by its unique identifier.
     *
     * @param findingId the UUID of the finding to retrieve
     * @return the finding as a response DTO
     * @throws EntityNotFoundException if no finding exists with the given ID
     * @throws AccessDeniedException if the current user is not a member of the associated team
     */
    @Transactional(readOnly = true)
    public FindingResponse getFinding(UUID findingId) {
        log.debug("getFinding called with findingId={}", findingId);
        Finding finding = findingRepository.findById(findingId)
                .orElseThrow(() -> new EntityNotFoundException("Finding not found"));
        verifyTeamMembership(finding.getJob().getProject().getTeam().getId());
        return mapToResponse(finding);
    }

    /**
     * Retrieves a paginated list of all findings for a QA job.
     *
     * @param jobId    the UUID of the QA job to retrieve findings for
     * @param pageable the pagination and sorting parameters
     * @return a paginated response containing finding DTOs
     * @throws EntityNotFoundException if the referenced job does not exist
     * @throws AccessDeniedException if the current user is not a member of the job's team
     */
    @Transactional(readOnly = true)
    public PageResponse<FindingResponse> getFindingsForJob(UUID jobId, Pageable pageable) {
        log.debug("getFindingsForJob called with jobId={}", jobId);
        var job = qaJobRepository.findById(jobId)
                .orElseThrow(() -> new EntityNotFoundException("Job not found"));
        verifyTeamMembership(job.getProject().getTeam().getId());

        Page<Finding> page = findingRepository.findByJobId(jobId, pageable);
        List<FindingResponse> content = page.getContent().stream()
                .map(this::mapToResponse)
                .toList();
        return new PageResponse<>(content, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages(), page.isLast());
    }

    /**
     * Retrieves a paginated list of findings for a QA job filtered by severity level.
     *
     * @param jobId    the UUID of the QA job to retrieve findings for
     * @param severity the severity level to filter by (e.g., CRITICAL, HIGH, MEDIUM, LOW)
     * @param pageable the pagination and sorting parameters
     * @return a paginated response containing finding DTOs matching the given severity
     * @throws EntityNotFoundException if the referenced job does not exist
     * @throws AccessDeniedException if the current user is not a member of the job's team
     */
    @Transactional(readOnly = true)
    public PageResponse<FindingResponse> getFindingsByJobAndSeverity(UUID jobId, Severity severity, Pageable pageable) {
        log.debug("getFindingsByJobAndSeverity called with jobId={}, severity={}", jobId, severity);
        var job = qaJobRepository.findById(jobId)
                .orElseThrow(() -> new EntityNotFoundException("Job not found"));
        verifyTeamMembership(job.getProject().getTeam().getId());
        Page<Finding> page = findingRepository.findByJobIdAndSeverity(jobId, severity, pageable);
        List<FindingResponse> content = page.getContent().stream()
                .map(this::mapToResponse)
                .toList();
        return new PageResponse<>(content, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages(), page.isLast());
    }

    /**
     * Retrieves a paginated list of findings for a QA job filtered by the agent type that produced them.
     *
     * @param jobId     the UUID of the QA job to retrieve findings for
     * @param agentType the type of agent to filter by
     * @param pageable  the pagination and sorting parameters
     * @return a paginated response containing finding DTOs produced by the given agent type
     * @throws EntityNotFoundException if the referenced job does not exist
     * @throws AccessDeniedException if the current user is not a member of the job's team
     */
    @Transactional(readOnly = true)
    public PageResponse<FindingResponse> getFindingsByJobAndAgent(UUID jobId, AgentType agentType, Pageable pageable) {
        log.debug("getFindingsByJobAndAgent called with jobId={}, agentType={}", jobId, agentType);
        var job = qaJobRepository.findById(jobId)
                .orElseThrow(() -> new EntityNotFoundException("Job not found"));
        verifyTeamMembership(job.getProject().getTeam().getId());
        Page<Finding> page = findingRepository.findByJobIdAndAgentType(jobId, agentType, pageable);
        List<FindingResponse> content = page.getContent().stream()
                .map(this::mapToResponse)
                .toList();
        return new PageResponse<>(content, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages(), page.isLast());
    }

    /**
     * Retrieves a paginated list of findings for a QA job filtered by finding status.
     *
     * @param jobId    the UUID of the QA job to retrieve findings for
     * @param status   the finding status to filter by (e.g., OPEN, RESOLVED, WONT_FIX)
     * @param pageable the pagination and sorting parameters
     * @return a paginated response containing finding DTOs matching the given status
     * @throws EntityNotFoundException if the referenced job does not exist
     * @throws AccessDeniedException if the current user is not a member of the job's team
     */
    @Transactional(readOnly = true)
    public PageResponse<FindingResponse> getFindingsByJobAndStatus(UUID jobId, FindingStatus status, Pageable pageable) {
        log.debug("getFindingsByJobAndStatus called with jobId={}, status={}", jobId, status);
        var job = qaJobRepository.findById(jobId)
                .orElseThrow(() -> new EntityNotFoundException("Job not found"));
        verifyTeamMembership(job.getProject().getTeam().getId());
        Page<Finding> page = findingRepository.findByJobIdAndStatus(jobId, status, pageable);
        List<FindingResponse> content = page.getContent().stream()
                .map(this::mapToResponse)
                .toList();
        return new PageResponse<>(content, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages(), page.isLast());
    }

    /**
     * Updates the status of a single finding and records who made the change and when.
     *
     * <p>Side effects: sets {@code statusChangedBy} to the current user and
     * {@code statusChangedAt} to the current timestamp.</p>
     *
     * @param findingId the UUID of the finding to update
     * @param request   the status update request containing the new status
     * @return the updated finding as a response DTO
     * @throws EntityNotFoundException if the finding or current user does not exist
     * @throws AccessDeniedException if the current user is not a member of the associated team
     */
    public FindingResponse updateFindingStatus(UUID findingId, UpdateFindingStatusRequest request) {
        log.debug("updateFindingStatus called with findingId={}, newStatus={}", findingId, request.status());
        Finding finding = findingRepository.findById(findingId)
                .orElseThrow(() -> new EntityNotFoundException("Finding not found"));
        verifyTeamMembership(finding.getJob().getProject().getTeam().getId());

        FindingStatus previousStatus = finding.getStatus();
        finding.setStatus(request.status());
        finding.setStatusChangedBy(userRepository.findById(SecurityUtils.getCurrentUserId())
                .orElseThrow(() -> new EntityNotFoundException("User not found")));
        finding.setStatusChangedAt(Instant.now());

        finding = findingRepository.save(finding);
        log.info("Finding status updated: findingId={}, {} -> {}", findingId, previousStatus, request.status());
        return mapToResponse(finding);
    }

    /**
     * Updates the status of multiple findings in a single batch operation.
     *
     * <p>All findings must belong to the same QA job. Each finding's {@code statusChangedBy}
     * is set to the current user and {@code statusChangedAt} to the current timestamp.</p>
     *
     * @param request the bulk update request containing a list of finding IDs and the new status
     * @return a list of the updated findings as response DTOs
     * @throws EntityNotFoundException if no findings are found for the provided IDs or the current user does not exist
     * @throws IllegalArgumentException if the findings belong to different jobs
     * @throws AccessDeniedException if the current user is not a member of the associated team
     */
    public List<FindingResponse> bulkUpdateFindingStatus(BulkUpdateFindingsRequest request) {
        log.debug("bulkUpdateFindingStatus called with findingCount={}, newStatus={}", request.findingIds().size(), request.status());
        List<Finding> findings = findingRepository.findAllById(request.findingIds());
        if (findings.isEmpty()) {
            log.warn("Bulk update found no findings for provided IDs");
            throw new EntityNotFoundException("No findings found for the provided IDs");
        }

        UUID firstJobId = findings.get(0).getJob().getId();
        boolean allSameJob = findings.stream().allMatch(f -> f.getJob().getId().equals(firstJobId));
        if (!allSameJob) {
            throw new IllegalArgumentException("All findings must belong to the same job");
        }

        verifyTeamMembership(findings.get(0).getJob().getProject().getTeam().getId());

        var currentUser = userRepository.findById(SecurityUtils.getCurrentUserId())
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        Instant now = Instant.now();
        findings.forEach(finding -> {
            finding.setStatus(request.status());
            finding.setStatusChangedBy(currentUser);
            finding.setStatusChangedAt(now);
        });

        findings = findingRepository.saveAll(findings);
        log.info("Bulk finding status update: count={}, jobId={}, newStatus={}", findings.size(), firstJobId, request.status());
        return findings.stream().map(this::mapToResponse).toList();
    }

    /**
     * Counts findings for a QA job grouped by severity level.
     *
     * <p>Returns a map with an entry for every {@link Severity} enum value, where the value
     * is the count of findings at that severity level (zero if none exist).</p>
     *
     * @param jobId the UUID of the QA job to count findings for
     * @return an {@link EnumMap} mapping each severity level to its finding count
     * @throws EntityNotFoundException if the referenced job does not exist
     * @throws AccessDeniedException if the current user is not a member of the job's team
     */
    @Transactional(readOnly = true)
    public Map<Severity, Long> countFindingsBySeverity(UUID jobId) {
        log.debug("countFindingsBySeverity called with jobId={}", jobId);
        var job = qaJobRepository.findById(jobId)
                .orElseThrow(() -> new EntityNotFoundException("Job not found"));
        verifyTeamMembership(job.getProject().getTeam().getId());

        Map<Severity, Long> counts = new EnumMap<>(Severity.class);
        for (Severity severity : Severity.values()) {
            counts.put(severity, findingRepository.countByJobIdAndSeverity(jobId, severity));
        }
        return counts;
    }

    private FindingResponse mapToResponse(Finding finding) {
        return new FindingResponse(
                finding.getId(),
                finding.getJob().getId(),
                finding.getAgentType(),
                finding.getSeverity(),
                finding.getTitle(),
                finding.getDescription(),
                finding.getFilePath(),
                finding.getLineNumber(),
                finding.getRecommendation(),
                finding.getEvidence(),
                finding.getEffortEstimate(),
                finding.getDebtCategory(),
                finding.getStatus(),
                finding.getStatusChangedBy() != null ? finding.getStatusChangedBy().getId() : null,
                finding.getStatusChangedAt(),
                finding.getCreatedAt()
        );
    }

    private void verifyTeamMembership(UUID teamId) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        if (!teamMemberRepository.existsByTeamIdAndUserId(teamId, currentUserId)) {
            throw new AccessDeniedException("Not a member of this team");
        }
    }
}
