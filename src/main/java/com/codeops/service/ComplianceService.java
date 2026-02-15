package com.codeops.service;

import com.codeops.dto.request.CreateComplianceItemRequest;
import com.codeops.dto.request.CreateSpecificationRequest;
import com.codeops.dto.response.ComplianceItemResponse;
import com.codeops.dto.response.PageResponse;
import com.codeops.dto.response.SpecificationResponse;
import com.codeops.entity.ComplianceItem;
import com.codeops.entity.Specification;
import com.codeops.entity.enums.ComplianceStatus;
import com.codeops.repository.ComplianceItemRepository;
import com.codeops.repository.QaJobRepository;
import com.codeops.repository.SpecificationRepository;
import com.codeops.repository.TeamMemberRepository;
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

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Manages compliance specifications and compliance items for QA jobs.
 *
 * <p>Specifications define the reference documents (e.g., standards, guidelines) against which
 * compliance is measured. Compliance items track individual requirements with statuses of
 * {@link ComplianceStatus#MET}, {@link ComplianceStatus#PARTIAL}, {@link ComplianceStatus#MISSING},
 * or {@link ComplianceStatus#NOT_APPLICABLE}. A compliance score is computed as a weighted
 * percentage where MET counts fully and PARTIAL counts at 50%.</p>
 *
 * @see ComplianceController
 * @see ComplianceItemRepository
 * @see SpecificationRepository
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ComplianceService {

    private static final Logger log = LoggerFactory.getLogger(ComplianceService.class);

    private final ComplianceItemRepository complianceItemRepository;
    private final SpecificationRepository specificationRepository;
    private final QaJobRepository qaJobRepository;
    private final TeamMemberRepository teamMemberRepository;

    /**
     * Creates a new specification document associated with a QA job.
     *
     * @param request the creation request containing job ID, name, spec type, and S3 key
     * @return the newly created specification as a response DTO
     * @throws EntityNotFoundException if the referenced job does not exist
     * @throws AccessDeniedException if the current user is not a member of the job's team
     */
    public SpecificationResponse createSpecification(CreateSpecificationRequest request) {
        log.debug("createSpecification called with jobId={}", request.jobId());
        var job = qaJobRepository.findById(request.jobId())
                .orElseThrow(() -> new EntityNotFoundException("Job not found"));
        verifyTeamMembership(job.getProject().getTeam().getId());

        Specification spec = Specification.builder()
                .job(job)
                .name(request.name())
                .specType(request.specType())
                .s3Key(request.s3Key())
                .build();

        spec = specificationRepository.save(spec);
        log.info("Created specification id={} for jobId={}", spec.getId(), request.jobId());
        return mapSpecToResponse(spec);
    }

    /**
     * Retrieves a paginated list of specifications associated with a QA job.
     *
     * @param jobId    the UUID of the QA job to retrieve specifications for
     * @param pageable the pagination and sorting parameters
     * @return a paginated response containing specification DTOs
     * @throws EntityNotFoundException if the referenced job does not exist
     * @throws AccessDeniedException if the current user is not a member of the job's team
     */
    @Transactional(readOnly = true)
    public PageResponse<SpecificationResponse> getSpecificationsForJob(UUID jobId, Pageable pageable) {
        log.debug("getSpecificationsForJob called with jobId={}", jobId);
        var job = qaJobRepository.findById(jobId)
                .orElseThrow(() -> new EntityNotFoundException("Job not found"));
        verifyTeamMembership(job.getProject().getTeam().getId());
        Page<Specification> page = specificationRepository.findByJobId(jobId, pageable);
        List<SpecificationResponse> content = page.getContent().stream()
                .map(this::mapSpecToResponse)
                .toList();
        return new PageResponse<>(content, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages(), page.isLast());
    }

    /**
     * Creates a single compliance item for a QA job, optionally linked to a specification.
     *
     * @param request the creation request containing job ID, requirement text, status, evidence, and optional spec ID
     * @return the newly created compliance item as a response DTO
     * @throws EntityNotFoundException if the referenced job or specification does not exist
     * @throws AccessDeniedException if the current user is not a member of the job's team
     */
    public ComplianceItemResponse createComplianceItem(CreateComplianceItemRequest request) {
        log.debug("createComplianceItem called with jobId={}, status={}", request.jobId(), request.status());
        var job = qaJobRepository.findById(request.jobId())
                .orElseThrow(() -> new EntityNotFoundException("Job not found"));
        verifyTeamMembership(job.getProject().getTeam().getId());

        ComplianceItem item = ComplianceItem.builder()
                .job(job)
                .requirement(request.requirement())
                .spec(request.specId() != null ? specificationRepository.findById(request.specId()).orElseThrow(() -> new EntityNotFoundException("Specification not found")) : null)
                .status(request.status())
                .evidence(request.evidence())
                .agentType(request.agentType())
                .notes(request.notes())
                .build();

        item = complianceItemRepository.save(item);
        log.info("Created compliance item id={} for jobId={} with status={}", item.getId(), request.jobId(), request.status());
        return mapItemToResponse(item);
    }

    /**
     * Creates multiple compliance items in a single batch. All items must belong to the same QA job.
     *
     * @param requests the list of creation requests; must all reference the same job ID
     * @return a list of the newly created compliance items as response DTOs, or an empty list if input is empty
     * @throws IllegalArgumentException if the requests reference different job IDs
     * @throws EntityNotFoundException if the referenced job or any specification does not exist
     * @throws AccessDeniedException if the current user is not a member of the job's team
     */
    public List<ComplianceItemResponse> createComplianceItems(List<CreateComplianceItemRequest> requests) {
        log.debug("createComplianceItems called with count={}", requests.size());
        if (requests.isEmpty()) return List.of();

        UUID firstJobId = requests.get(0).jobId();
        boolean allSameJob = requests.stream().allMatch(r -> r.jobId().equals(firstJobId));
        if (!allSameJob) {
            throw new IllegalArgumentException("All compliance items must belong to the same job");
        }

        var job = qaJobRepository.findById(firstJobId)
                .orElseThrow(() -> new EntityNotFoundException("Job not found"));
        verifyTeamMembership(job.getProject().getTeam().getId());

        List<ComplianceItem> items = requests.stream()
                .map(request -> ComplianceItem.builder()
                        .job(job)
                        .requirement(request.requirement())
                        .spec(request.specId() != null ? specificationRepository.findById(request.specId()).orElseThrow(() -> new EntityNotFoundException("Specification not found")) : null)
                        .status(request.status())
                        .evidence(request.evidence())
                        .agentType(request.agentType())
                        .notes(request.notes())
                        .build())
                .toList();

        items = complianceItemRepository.saveAll(items);
        log.info("Created {} compliance items for jobId={}", items.size(), firstJobId);
        return items.stream().map(this::mapItemToResponse).toList();
    }

    /**
     * Retrieves a paginated list of all compliance items for a QA job.
     *
     * @param jobId    the UUID of the QA job to retrieve compliance items for
     * @param pageable the pagination and sorting parameters
     * @return a paginated response containing compliance item DTOs
     * @throws EntityNotFoundException if the referenced job does not exist
     * @throws AccessDeniedException if the current user is not a member of the job's team
     */
    @Transactional(readOnly = true)
    public PageResponse<ComplianceItemResponse> getComplianceItemsForJob(UUID jobId, Pageable pageable) {
        log.debug("getComplianceItemsForJob called with jobId={}", jobId);
        var job = qaJobRepository.findById(jobId)
                .orElseThrow(() -> new EntityNotFoundException("Job not found"));
        verifyTeamMembership(job.getProject().getTeam().getId());
        Page<ComplianceItem> page = complianceItemRepository.findByJobId(jobId, pageable);
        List<ComplianceItemResponse> content = page.getContent().stream()
                .map(this::mapItemToResponse)
                .toList();
        return new PageResponse<>(content, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages(), page.isLast());
    }

    /**
     * Retrieves a paginated list of compliance items for a QA job filtered by compliance status.
     *
     * @param jobId    the UUID of the QA job to retrieve compliance items for
     * @param status   the compliance status to filter by (e.g., MET, PARTIAL, MISSING)
     * @param pageable the pagination and sorting parameters
     * @return a paginated response containing compliance item DTOs matching the given status
     * @throws EntityNotFoundException if the referenced job does not exist
     * @throws AccessDeniedException if the current user is not a member of the job's team
     */
    @Transactional(readOnly = true)
    public PageResponse<ComplianceItemResponse> getComplianceItemsByStatus(UUID jobId, ComplianceStatus status, Pageable pageable) {
        log.debug("getComplianceItemsByStatus called with jobId={}, status={}", jobId, status);
        var job = qaJobRepository.findById(jobId)
                .orElseThrow(() -> new EntityNotFoundException("Job not found"));
        verifyTeamMembership(job.getProject().getTeam().getId());
        Page<ComplianceItem> page = complianceItemRepository.findByJobIdAndStatus(jobId, status, pageable);
        List<ComplianceItemResponse> content = page.getContent().stream()
                .map(this::mapItemToResponse)
                .toList();
        return new PageResponse<>(content, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages(), page.isLast());
    }

    /**
     * Computes a compliance summary for a QA job including counts by status and a weighted score.
     *
     * <p>The compliance score is calculated as: {@code ((met * 100 + partial * 50) / (total * 100)) * 100},
     * where MET requirements contribute fully and PARTIAL requirements contribute 50%.
     * Returns a map with keys: {@code met}, {@code partial}, {@code missing},
     * {@code notApplicable}, {@code total}, and {@code complianceScore}.</p>
     *
     * @param jobId the UUID of the QA job to compute the compliance summary for
     * @return a map of summary statistic names to their numeric values
     * @throws EntityNotFoundException if the referenced job does not exist
     * @throws AccessDeniedException if the current user is not a member of the job's team
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getComplianceSummary(UUID jobId) {
        log.debug("getComplianceSummary called with jobId={}", jobId);
        var job = qaJobRepository.findById(jobId)
                .orElseThrow(() -> new EntityNotFoundException("Job not found"));
        verifyTeamMembership(job.getProject().getTeam().getId());

        int met = complianceItemRepository.findByJobIdAndStatus(jobId, ComplianceStatus.MET).size();
        int partial = complianceItemRepository.findByJobIdAndStatus(jobId, ComplianceStatus.PARTIAL).size();
        int missing = complianceItemRepository.findByJobIdAndStatus(jobId, ComplianceStatus.MISSING).size();
        int notApplicable = complianceItemRepository.findByJobIdAndStatus(jobId, ComplianceStatus.NOT_APPLICABLE).size();
        int total = met + partial + missing + notApplicable;
        double score = total > 0 ? ((double) (met * 100 + partial * 50) / (total * 100)) * 100 : 0;

        log.info("Compliance summary for jobId={}: total={}, met={}, partial={}, missing={}, score={}", jobId, total, met, partial, missing, Math.round(score));
        return Map.of(
                "met", met,
                "partial", partial,
                "missing", missing,
                "notApplicable", notApplicable,
                "total", total,
                "complianceScore", Math.round(score)
        );
    }

    private SpecificationResponse mapSpecToResponse(Specification spec) {
        return new SpecificationResponse(
                spec.getId(),
                spec.getJob().getId(),
                spec.getName(),
                spec.getSpecType(),
                spec.getS3Key(),
                spec.getCreatedAt()
        );
    }

    private ComplianceItemResponse mapItemToResponse(ComplianceItem item) {
        String specName = null;
        UUID specId = null;
        if (item.getSpec() != null) {
            specId = item.getSpec().getId();
            specName = item.getSpec().getName();
        }
        return new ComplianceItemResponse(
                item.getId(),
                item.getJob().getId(),
                item.getRequirement(),
                specId,
                specName,
                item.getStatus(),
                item.getEvidence(),
                item.getAgentType(),
                item.getNotes(),
                item.getCreatedAt()
        );
    }

    private void verifyTeamMembership(UUID teamId) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        if (!teamMemberRepository.existsByTeamIdAndUserId(teamId, currentUserId)) {
            throw new AccessDeniedException("Not a member of this team");
        }
    }
}
