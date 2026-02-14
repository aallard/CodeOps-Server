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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ComplianceService {

    private final ComplianceItemRepository complianceItemRepository;
    private final SpecificationRepository specificationRepository;
    private final QaJobRepository qaJobRepository;
    private final TeamMemberRepository teamMemberRepository;

    public SpecificationResponse createSpecification(CreateSpecificationRequest request) {
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
        return mapSpecToResponse(spec);
    }

    @Transactional(readOnly = true)
    public PageResponse<SpecificationResponse> getSpecificationsForJob(UUID jobId, Pageable pageable) {
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

    public ComplianceItemResponse createComplianceItem(CreateComplianceItemRequest request) {
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
        return mapItemToResponse(item);
    }

    public List<ComplianceItemResponse> createComplianceItems(List<CreateComplianceItemRequest> requests) {
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
        return items.stream().map(this::mapItemToResponse).toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<ComplianceItemResponse> getComplianceItemsForJob(UUID jobId, Pageable pageable) {
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

    @Transactional(readOnly = true)
    public PageResponse<ComplianceItemResponse> getComplianceItemsByStatus(UUID jobId, ComplianceStatus status, Pageable pageable) {
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

    @Transactional(readOnly = true)
    public Map<String, Object> getComplianceSummary(UUID jobId) {
        var job = qaJobRepository.findById(jobId)
                .orElseThrow(() -> new EntityNotFoundException("Job not found"));
        verifyTeamMembership(job.getProject().getTeam().getId());

        int met = complianceItemRepository.findByJobIdAndStatus(jobId, ComplianceStatus.MET).size();
        int partial = complianceItemRepository.findByJobIdAndStatus(jobId, ComplianceStatus.PARTIAL).size();
        int missing = complianceItemRepository.findByJobIdAndStatus(jobId, ComplianceStatus.MISSING).size();
        int notApplicable = complianceItemRepository.findByJobIdAndStatus(jobId, ComplianceStatus.NOT_APPLICABLE).size();
        int total = met + partial + missing + notApplicable;
        double score = total > 0 ? ((double) (met * 100 + partial * 50) / (total * 100)) * 100 : 0;

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
