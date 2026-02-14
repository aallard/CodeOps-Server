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

@Service
@RequiredArgsConstructor
@Transactional
public class FindingService {

    private final FindingRepository findingRepository;
    private final QaJobRepository qaJobRepository;
    private final UserRepository userRepository;
    private final TeamMemberRepository teamMemberRepository;

    public FindingResponse createFinding(CreateFindingRequest request) {
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
        return mapToResponse(finding);
    }

    public List<FindingResponse> createFindings(List<CreateFindingRequest> requests) {
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
        return findings.stream().map(this::mapToResponse).toList();
    }

    @Transactional(readOnly = true)
    public FindingResponse getFinding(UUID findingId) {
        Finding finding = findingRepository.findById(findingId)
                .orElseThrow(() -> new EntityNotFoundException("Finding not found"));
        verifyTeamMembership(finding.getJob().getProject().getTeam().getId());
        return mapToResponse(finding);
    }

    @Transactional(readOnly = true)
    public PageResponse<FindingResponse> getFindingsForJob(UUID jobId, Pageable pageable) {
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

    @Transactional(readOnly = true)
    public List<FindingResponse> getFindingsByJobAndSeverity(UUID jobId, Severity severity) {
        var job = qaJobRepository.findById(jobId)
                .orElseThrow(() -> new EntityNotFoundException("Job not found"));
        verifyTeamMembership(job.getProject().getTeam().getId());
        return findingRepository.findByJobIdAndSeverity(jobId, severity).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FindingResponse> getFindingsByJobAndAgent(UUID jobId, AgentType agentType) {
        var job = qaJobRepository.findById(jobId)
                .orElseThrow(() -> new EntityNotFoundException("Job not found"));
        verifyTeamMembership(job.getProject().getTeam().getId());
        return findingRepository.findByJobIdAndAgentType(jobId, agentType).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FindingResponse> getFindingsByJobAndStatus(UUID jobId, FindingStatus status) {
        var job = qaJobRepository.findById(jobId)
                .orElseThrow(() -> new EntityNotFoundException("Job not found"));
        verifyTeamMembership(job.getProject().getTeam().getId());
        return findingRepository.findByJobIdAndStatus(jobId, status).stream()
                .map(this::mapToResponse)
                .toList();
    }

    public FindingResponse updateFindingStatus(UUID findingId, UpdateFindingStatusRequest request) {
        Finding finding = findingRepository.findById(findingId)
                .orElseThrow(() -> new EntityNotFoundException("Finding not found"));
        verifyTeamMembership(finding.getJob().getProject().getTeam().getId());

        finding.setStatus(request.status());
        finding.setStatusChangedBy(userRepository.getReferenceById(SecurityUtils.getCurrentUserId()));
        finding.setStatusChangedAt(Instant.now());

        finding = findingRepository.save(finding);
        return mapToResponse(finding);
    }

    public List<FindingResponse> bulkUpdateFindingStatus(BulkUpdateFindingsRequest request) {
        List<Finding> findings = findingRepository.findAllById(request.findingIds());
        if (findings.isEmpty()) {
            throw new EntityNotFoundException("No findings found for the provided IDs");
        }

        UUID firstJobId = findings.get(0).getJob().getId();
        boolean allSameJob = findings.stream().allMatch(f -> f.getJob().getId().equals(firstJobId));
        if (!allSameJob) {
            throw new IllegalArgumentException("All findings must belong to the same job");
        }

        verifyTeamMembership(findings.get(0).getJob().getProject().getTeam().getId());

        var currentUser = userRepository.getReferenceById(SecurityUtils.getCurrentUserId());
        Instant now = Instant.now();
        findings.forEach(finding -> {
            finding.setStatus(request.status());
            finding.setStatusChangedBy(currentUser);
            finding.setStatusChangedAt(now);
        });

        findings = findingRepository.saveAll(findings);
        return findings.stream().map(this::mapToResponse).toList();
    }

    @Transactional(readOnly = true)
    public Map<Severity, Long> countFindingsBySeverity(UUID jobId) {
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
