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
import java.util.stream.Collectors;

/**
 * Manages the lifecycle of technical debt items tracked against projects.
 *
 * <p>Technical debt items are categorized by {@link DebtCategory}, prioritized by
 * {@link BusinessImpact}, and tracked through a {@link DebtStatus} lifecycle. Items
 * can be linked to the QA job that first detected them and to the job that resolved them.</p>
 *
 * <p>All operations verify that the calling user is a member of the team that owns
 * the associated project. Deletion requires OWNER or ADMIN role.</p>
 *
 * @see TechDebtController
 * @see TechDebtItem
 * @see TechDebtItemRepository
 */
@Service
@RequiredArgsConstructor
@Transactional
public class TechDebtService {

    private static final Logger log = LoggerFactory.getLogger(TechDebtService.class);

    private final TechDebtItemRepository techDebtItemRepository;
    private final ProjectRepository projectRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final QaJobRepository qaJobRepository;

    /**
     * Creates a single technical debt item for a project.
     *
     * <p>The item is persisted with an initial status of {@link DebtStatus#IDENTIFIED}.
     * If a {@code firstDetectedJobId} is provided, it is resolved and linked to the item.</p>
     *
     * @param request the creation request containing project ID, category, title, description,
     *                file path, effort estimate, business impact, and optional first detected job ID
     * @return the created tech debt item as a response DTO
     * @throws EntityNotFoundException if the project or referenced job is not found
     * @throws AccessDeniedException if the current user is not a member of the project's team
     */
    public TechDebtItemResponse createTechDebtItem(CreateTechDebtItemRequest request) {
        log.debug("createTechDebtItem called with projectId={}, category={}", request.projectId(), request.category());
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
        log.info("Created tech debt item id={} for projectId={}, category={}, impact={}", item.getId(), request.projectId(), request.category(), request.businessImpact());
        return mapToResponse(item);
    }

    /**
     * Creates multiple technical debt items in bulk for a single project.
     *
     * <p>All requests must reference the same project ID. Verifies team membership once
     * for the shared project, then persists all items with an initial status of
     * {@link DebtStatus#IDENTIFIED}.</p>
     *
     * @param requests the list of creation requests; all must share the same project ID
     * @return the list of created tech debt items as response DTOs, or an empty list if input is empty
     * @throws IllegalArgumentException if the requests reference different project IDs
     * @throws EntityNotFoundException if the project or any referenced job is not found
     * @throws AccessDeniedException if the current user is not a member of the project's team
     */
    public List<TechDebtItemResponse> createTechDebtItems(List<CreateTechDebtItemRequest> requests) {
        log.debug("createTechDebtItems called with count={}", requests.size());
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
        log.info("Created {} tech debt items for projectId={}", items.size(), firstProjectId);
        return items.stream().map(this::mapToResponse).toList();
    }

    /**
     * Retrieves a single technical debt item by its ID.
     *
     * @param itemId the ID of the tech debt item to retrieve
     * @return the tech debt item as a response DTO
     * @throws EntityNotFoundException if the item is not found
     * @throws AccessDeniedException if the current user is not a member of the item's project team
     */
    @Transactional(readOnly = true)
    public TechDebtItemResponse getTechDebtItem(UUID itemId) {
        log.debug("getTechDebtItem called with itemId={}", itemId);
        TechDebtItem item = techDebtItemRepository.findById(itemId)
                .orElseThrow(() -> new EntityNotFoundException("Tech debt item not found"));
        verifyTeamMembership(item.getProject().getTeam().getId());
        return mapToResponse(item);
    }

    /**
     * Retrieves a paginated list of all technical debt items for a project.
     *
     * @param projectId the ID of the project whose tech debt items to retrieve
     * @param pageable pagination and sorting parameters
     * @return a paginated response containing the project's tech debt items
     * @throws EntityNotFoundException if the project is not found
     * @throws AccessDeniedException if the current user is not a member of the project's team
     */
    @Transactional(readOnly = true)
    public PageResponse<TechDebtItemResponse> getTechDebtForProject(UUID projectId, Pageable pageable) {
        log.debug("getTechDebtForProject called with projectId={}", projectId);
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

    /**
     * Retrieves a paginated list of technical debt items for a project filtered by status.
     *
     * @param projectId the ID of the project whose tech debt items to retrieve
     * @param status the debt status to filter by
     * @param pageable pagination and sorting parameters
     * @return a paginated response containing the matching tech debt items
     * @throws EntityNotFoundException if the project is not found
     * @throws AccessDeniedException if the current user is not a member of the project's team
     */
    @Transactional(readOnly = true)
    public PageResponse<TechDebtItemResponse> getTechDebtByStatus(UUID projectId, DebtStatus status, Pageable pageable) {
        log.debug("getTechDebtByStatus called with projectId={}, status={}", projectId, status);
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

    /**
     * Retrieves a paginated list of technical debt items for a project filtered by category.
     *
     * @param projectId the ID of the project whose tech debt items to retrieve
     * @param category the debt category to filter by
     * @param pageable pagination and sorting parameters
     * @return a paginated response containing the matching tech debt items
     * @throws EntityNotFoundException if the project is not found
     * @throws AccessDeniedException if the current user is not a member of the project's team
     */
    @Transactional(readOnly = true)
    public PageResponse<TechDebtItemResponse> getTechDebtByCategory(UUID projectId, DebtCategory category, Pageable pageable) {
        log.debug("getTechDebtByCategory called with projectId={}, category={}", projectId, category);
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

    /**
     * Updates the status of a technical debt item and optionally links a resolved job.
     *
     * <p>If a {@code resolvedJobId} is provided, the referenced job is resolved and
     * linked as the job that resolved this debt item.</p>
     *
     * @param itemId the ID of the tech debt item to update
     * @param request the update request containing the new status and optional resolved job ID
     * @return the updated tech debt item as a response DTO
     * @throws EntityNotFoundException if the item or referenced job is not found
     * @throws AccessDeniedException if the current user is not a member of the item's project team
     */
    public TechDebtItemResponse updateTechDebtStatus(UUID itemId, UpdateTechDebtStatusRequest request) {
        log.debug("updateTechDebtStatus called with itemId={}, newStatus={}", itemId, request.status());
        TechDebtItem item = techDebtItemRepository.findById(itemId)
                .orElseThrow(() -> new EntityNotFoundException("Tech debt item not found"));
        verifyTeamMembership(item.getProject().getTeam().getId());

        DebtStatus oldStatus = item.getStatus();
        item.setStatus(request.status());
        if (request.resolvedJobId() != null) {
            item.setResolvedJob(qaJobRepository.findById(request.resolvedJobId()).orElseThrow(() -> new EntityNotFoundException("Job not found")));
        }

        item = techDebtItemRepository.save(item);
        log.info("Updated tech debt item id={} status from {} to {}", itemId, oldStatus, request.status());
        return mapToResponse(item);
    }

    /**
     * Permanently deletes a technical debt item.
     *
     * <p>Requires the calling user to have OWNER or ADMIN role on the item's project team.</p>
     *
     * @param itemId the ID of the tech debt item to delete
     * @throws EntityNotFoundException if the item is not found
     * @throws AccessDeniedException if the current user does not have OWNER or ADMIN role
     */
    public void deleteTechDebtItem(UUID itemId) {
        log.debug("deleteTechDebtItem called with itemId={}", itemId);
        TechDebtItem item = techDebtItemRepository.findById(itemId)
                .orElseThrow(() -> new EntityNotFoundException("Tech debt item not found"));
        verifyTeamAdmin(item.getProject().getTeam().getId());
        techDebtItemRepository.delete(item);
        log.info("Deleted tech debt item id={}", itemId);
    }

    /**
     * Generates an aggregate summary of technical debt for a project.
     *
     * <p>Returns a map containing:</p>
     * <ul>
     *   <li>{@code total} - total number of tech debt items</li>
     *   <li>{@code open} - count of items not in RESOLVED status</li>
     *   <li>{@code critical} - count of items with CRITICAL business impact</li>
     *   <li>{@code byCategory} - breakdown of item counts by {@link DebtCategory}</li>
     *   <li>{@code byStatus} - breakdown of item counts by {@link DebtStatus}</li>
     * </ul>
     *
     * @param projectId the ID of the project to summarize
     * @return a map containing the debt summary statistics
     * @throws EntityNotFoundException if the project is not found
     * @throws AccessDeniedException if the current user is not a member of the project's team
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getDebtSummary(UUID projectId) {
        log.debug("getDebtSummary called with projectId={}", projectId);
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
