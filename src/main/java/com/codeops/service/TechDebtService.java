package com.codeops.service;

import com.codeops.dto.request.CreateTechDebtItemRequest;
import com.codeops.dto.request.UpdateTechDebtStatusRequest;
import com.codeops.dto.response.PageResponse;
import com.codeops.dto.response.TechDebtItemResponse;
import com.codeops.entity.TechDebtItem;
import com.codeops.entity.TeamMember;
import com.codeops.entity.enums.BusinessImpact;
import com.codeops.entity.enums.DebtStatus;
import com.codeops.entity.enums.TeamRole;
import com.codeops.repository.ProjectRepository;
import com.codeops.repository.QaJobRepository;
import com.codeops.repository.TechDebtItemRepository;
import com.codeops.repository.TeamMemberRepository;
import com.codeops.entity.enums.DebtCategory;
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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class TechDebtService {

    private final TechDebtItemRepository techDebtItemRepository;
    private final ProjectRepository projectRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final QaJobRepository qaJobRepository;

    public TechDebtItemResponse createTechDebtItem(CreateTechDebtItemRequest request) {
        var project = projectRepository.findById(request.projectId())
                .orElseThrow(() -> new EntityNotFoundException("Project not found"));
        verifyTeamMembership(project.getTeam().getId());

        TechDebtItem item = TechDebtItem.builder()
                .project(project)
                .category(request.category())
                .title(request.title())
                .description(request.description())
                .filePath(request.filePath())
                .effortEstimate(request.effortEstimate())
                .businessImpact(request.businessImpact())
                .status(DebtStatus.IDENTIFIED)
                .firstDetectedJob(request.firstDetectedJobId() != null ? qaJobRepository.findById(request.firstDetectedJobId()).orElseThrow(() -> new EntityNotFoundException("Job not found")) : null)
                .build();

        item = techDebtItemRepository.save(item);
        return mapToResponse(item);
    }

    public List<TechDebtItemResponse> createTechDebtItems(List<CreateTechDebtItemRequest> requests) {
        if (requests.isEmpty()) return List.of();

        UUID firstProjectId = requests.get(0).projectId();
        boolean allSameProject = requests.stream().allMatch(r -> r.projectId().equals(firstProjectId));
        if (!allSameProject) {
            throw new IllegalArgumentException("All tech debt items must belong to the same project");
        }

        var project = projectRepository.findById(firstProjectId)
                .orElseThrow(() -> new EntityNotFoundException("Project not found"));
        verifyTeamMembership(project.getTeam().getId());

        List<TechDebtItem> items = requests.stream()
                .map(request -> TechDebtItem.builder()
                        .project(project)
                        .category(request.category())
                        .title(request.title())
                        .description(request.description())
                        .filePath(request.filePath())
                        .effortEstimate(request.effortEstimate())
                        .businessImpact(request.businessImpact())
                        .status(DebtStatus.IDENTIFIED)
                        .firstDetectedJob(request.firstDetectedJobId() != null ? qaJobRepository.findById(request.firstDetectedJobId()).orElseThrow(() -> new EntityNotFoundException("Job not found")) : null)
                        .build())
                .toList();

        items = techDebtItemRepository.saveAll(items);
        return items.stream().map(this::mapToResponse).toList();
    }

    @Transactional(readOnly = true)
    public TechDebtItemResponse getTechDebtItem(UUID itemId) {
        TechDebtItem item = techDebtItemRepository.findById(itemId)
                .orElseThrow(() -> new EntityNotFoundException("Tech debt item not found"));
        verifyTeamMembership(item.getProject().getTeam().getId());
        return mapToResponse(item);
    }

    @Transactional(readOnly = true)
    public PageResponse<TechDebtItemResponse> getTechDebtForProject(UUID projectId, Pageable pageable) {
        var project = projectRepository.findById(projectId)
                .orElseThrow(() -> new EntityNotFoundException("Project not found"));
        verifyTeamMembership(project.getTeam().getId());
        Page<TechDebtItem> page = techDebtItemRepository.findByProjectId(projectId, pageable);
        List<TechDebtItemResponse> content = page.getContent().stream()
                .map(this::mapToResponse)
                .toList();
        return new PageResponse<>(content, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages(), page.isLast());
    }

    @Transactional(readOnly = true)
    public PageResponse<TechDebtItemResponse> getTechDebtByStatus(UUID projectId, DebtStatus status, Pageable pageable) {
        var project = projectRepository.findById(projectId)
                .orElseThrow(() -> new EntityNotFoundException("Project not found"));
        verifyTeamMembership(project.getTeam().getId());
        Page<TechDebtItem> page = techDebtItemRepository.findByProjectIdAndStatus(projectId, status, pageable);
        List<TechDebtItemResponse> content = page.getContent().stream()
                .map(this::mapToResponse)
                .toList();
        return new PageResponse<>(content, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages(), page.isLast());
    }

    @Transactional(readOnly = true)
    public PageResponse<TechDebtItemResponse> getTechDebtByCategory(UUID projectId, DebtCategory category, Pageable pageable) {
        var project = projectRepository.findById(projectId)
                .orElseThrow(() -> new EntityNotFoundException("Project not found"));
        verifyTeamMembership(project.getTeam().getId());
        Page<TechDebtItem> page = techDebtItemRepository.findByProjectIdAndCategory(projectId, category, pageable);
        List<TechDebtItemResponse> content = page.getContent().stream()
                .map(this::mapToResponse)
                .toList();
        return new PageResponse<>(content, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages(), page.isLast());
    }

    public TechDebtItemResponse updateTechDebtStatus(UUID itemId, UpdateTechDebtStatusRequest request) {
        TechDebtItem item = techDebtItemRepository.findById(itemId)
                .orElseThrow(() -> new EntityNotFoundException("Tech debt item not found"));
        verifyTeamMembership(item.getProject().getTeam().getId());

        item.setStatus(request.status());
        if (request.resolvedJobId() != null) {
            item.setResolvedJob(qaJobRepository.findById(request.resolvedJobId()).orElseThrow(() -> new EntityNotFoundException("Job not found")));
        }

        item = techDebtItemRepository.save(item);
        return mapToResponse(item);
    }

    public void deleteTechDebtItem(UUID itemId) {
        TechDebtItem item = techDebtItemRepository.findById(itemId)
                .orElseThrow(() -> new EntityNotFoundException("Tech debt item not found"));
        verifyTeamAdmin(item.getProject().getTeam().getId());
        techDebtItemRepository.delete(item);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getDebtSummary(UUID projectId) {
        var project = projectRepository.findById(projectId)
                .orElseThrow(() -> new EntityNotFoundException("Project not found"));
        verifyTeamMembership(project.getTeam().getId());

        List<TechDebtItem> items = techDebtItemRepository.findByProjectId(projectId);
        Map<DebtCategory, Long> byCategory = items.stream()
                .collect(Collectors.groupingBy(TechDebtItem::getCategory, Collectors.counting()));
        Map<DebtStatus, Long> byStatus = items.stream()
                .collect(Collectors.groupingBy(TechDebtItem::getStatus, Collectors.counting()));
        long total = items.size();
        long open = items.stream().filter(i -> i.getStatus() != DebtStatus.RESOLVED).count();
        long critical = items.stream().filter(i -> i.getBusinessImpact() == BusinessImpact.CRITICAL).count();

        return Map.of("total", total, "open", open, "critical", critical, "byCategory", byCategory, "byStatus", byStatus);
    }

    private TechDebtItemResponse mapToResponse(TechDebtItem item) {
        return new TechDebtItemResponse(
                item.getId(),
                item.getProject().getId(),
                item.getCategory(),
                item.getTitle(),
                item.getDescription(),
                item.getFilePath(),
                item.getEffortEstimate(),
                item.getBusinessImpact(),
                item.getStatus(),
                item.getFirstDetectedJob() != null ? item.getFirstDetectedJob().getId() : null,
                item.getResolvedJob() != null ? item.getResolvedJob().getId() : null,
                item.getCreatedAt(),
                item.getUpdatedAt()
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
