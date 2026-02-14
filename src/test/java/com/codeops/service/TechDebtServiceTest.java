package com.codeops.service;

import com.codeops.dto.request.CreateTechDebtItemRequest;
import com.codeops.dto.request.UpdateTechDebtStatusRequest;
import com.codeops.dto.response.PageResponse;
import com.codeops.dto.response.TechDebtItemResponse;
import com.codeops.entity.*;
import com.codeops.entity.enums.*;
import com.codeops.repository.ProjectRepository;
import com.codeops.repository.QaJobRepository;
import com.codeops.repository.TechDebtItemRepository;
import com.codeops.repository.TeamMemberRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TechDebtServiceTest {

    @Mock private TechDebtItemRepository techDebtItemRepository;
    @Mock private ProjectRepository projectRepository;
    @Mock private TeamMemberRepository teamMemberRepository;
    @Mock private QaJobRepository qaJobRepository;

    @InjectMocks
    private TechDebtService techDebtService;

    private UUID userId;
    private UUID teamId;
    private UUID projectId;
    private UUID itemId;
    private UUID jobId;
    private Team team;
    private Project project;
    private QaJob qaJob;
    private TechDebtItem debtItem;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        teamId = UUID.randomUUID();
        projectId = UUID.randomUUID();
        itemId = UUID.randomUUID();
        jobId = UUID.randomUUID();

        team = Team.builder().name("Test Team").build();
        team.setId(teamId);

        project = Project.builder().team(team).name("Test Project").build();
        project.setId(projectId);

        qaJob = QaJob.builder().project(project).build();
        qaJob.setId(jobId);

        debtItem = TechDebtItem.builder()
                .project(project)
                .category(DebtCategory.CODE)
                .title("Duplicated logic in UserService")
                .description("Same validation logic repeated in 3 places")
                .filePath("src/main/java/com/example/UserService.java")
                .effortEstimate(Effort.M)
                .businessImpact(BusinessImpact.MEDIUM)
                .status(DebtStatus.IDENTIFIED)
                .build();
        debtItem.setId(itemId);
        debtItem.setCreatedAt(Instant.now());
        debtItem.setUpdatedAt(Instant.now());

        setSecurityContext(userId);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // --- createTechDebtItem ---

    @Test
    void createTechDebtItem_success() {
        CreateTechDebtItemRequest request = new CreateTechDebtItemRequest(
                projectId, DebtCategory.CODE, "Duplicated logic", "description",
                "src/UserService.java", Effort.M, BusinessImpact.MEDIUM, null);

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(true);
        when(techDebtItemRepository.save(any(TechDebtItem.class))).thenAnswer(inv -> {
            TechDebtItem item = inv.getArgument(0);
            item.setId(itemId);
            item.setCreatedAt(Instant.now());
            item.setUpdatedAt(Instant.now());
            return item;
        });

        TechDebtItemResponse response = techDebtService.createTechDebtItem(request);

        assertNotNull(response);
        assertEquals(DebtCategory.CODE, response.category());
        assertEquals(DebtStatus.IDENTIFIED, response.status());
    }

    @Test
    void createTechDebtItem_withFirstDetectedJob_success() {
        CreateTechDebtItemRequest request = new CreateTechDebtItemRequest(
                projectId, DebtCategory.ARCHITECTURE, "Title", null, null, null, null, jobId);

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(true);
        when(qaJobRepository.findById(jobId)).thenReturn(Optional.of(qaJob));
        when(techDebtItemRepository.save(any(TechDebtItem.class))).thenAnswer(inv -> {
            TechDebtItem item = inv.getArgument(0);
            item.setId(itemId);
            item.setCreatedAt(Instant.now());
            item.setUpdatedAt(Instant.now());
            return item;
        });

        TechDebtItemResponse response = techDebtService.createTechDebtItem(request);

        assertNotNull(response);
        assertEquals(jobId, response.firstDetectedJobId());
    }

    @Test
    void createTechDebtItem_projectNotFound_throws() {
        CreateTechDebtItemRequest request = new CreateTechDebtItemRequest(
                projectId, DebtCategory.CODE, "Title", null, null, null, null, null);

        when(projectRepository.findById(projectId)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> techDebtService.createTechDebtItem(request));
    }

    @Test
    void createTechDebtItem_notTeamMember_throws() {
        CreateTechDebtItemRequest request = new CreateTechDebtItemRequest(
                projectId, DebtCategory.CODE, "Title", null, null, null, null, null);

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> techDebtService.createTechDebtItem(request));
    }

    // --- createTechDebtItems ---

    @Test
    void createTechDebtItems_emptyList_returnsEmpty() {
        List<TechDebtItemResponse> result = techDebtService.createTechDebtItems(List.of());
        assertTrue(result.isEmpty());
    }

    @Test
    void createTechDebtItems_success() {
        CreateTechDebtItemRequest req1 = new CreateTechDebtItemRequest(
                projectId, DebtCategory.CODE, "Item 1", null, null, null, null, null);
        CreateTechDebtItemRequest req2 = new CreateTechDebtItemRequest(
                projectId, DebtCategory.TEST, "Item 2", null, null, null, null, null);

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(true);
        when(techDebtItemRepository.saveAll(anyList())).thenAnswer(inv -> {
            List<TechDebtItem> items = inv.getArgument(0);
            for (TechDebtItem item : items) {
                item.setId(UUID.randomUUID());
                item.setCreatedAt(Instant.now());
                item.setUpdatedAt(Instant.now());
            }
            return items;
        });

        List<TechDebtItemResponse> responses = techDebtService.createTechDebtItems(List.of(req1, req2));
        assertEquals(2, responses.size());
    }

    @Test
    void createTechDebtItems_differentProjects_throws() {
        UUID otherProjectId = UUID.randomUUID();
        CreateTechDebtItemRequest req1 = new CreateTechDebtItemRequest(
                projectId, DebtCategory.CODE, "Item 1", null, null, null, null, null);
        CreateTechDebtItemRequest req2 = new CreateTechDebtItemRequest(
                otherProjectId, DebtCategory.CODE, "Item 2", null, null, null, null, null);

        assertThrows(IllegalArgumentException.class,
                () -> techDebtService.createTechDebtItems(List.of(req1, req2)));
    }

    // --- getTechDebtItem ---

    @Test
    void getTechDebtItem_success() {
        when(techDebtItemRepository.findById(itemId)).thenReturn(Optional.of(debtItem));
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(true);

        TechDebtItemResponse response = techDebtService.getTechDebtItem(itemId);

        assertNotNull(response);
        assertEquals(itemId, response.id());
        assertEquals("Duplicated logic in UserService", response.title());
    }

    @Test
    void getTechDebtItem_notFound_throws() {
        when(techDebtItemRepository.findById(itemId)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> techDebtService.getTechDebtItem(itemId));
    }

    // --- getTechDebtForProject ---

    @Test
    void getTechDebtForProject_success() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<TechDebtItem> page = new PageImpl<>(List.of(debtItem), pageable, 1);

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(true);
        when(techDebtItemRepository.findByProjectId(projectId, pageable)).thenReturn(page);

        PageResponse<TechDebtItemResponse> result = techDebtService.getTechDebtForProject(projectId, pageable);

        assertEquals(1, result.content().size());
    }

    // --- getTechDebtByStatus ---

    @Test
    void getTechDebtByStatus_success() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<TechDebtItem> page = new PageImpl<>(List.of(debtItem), pageable, 1);

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(true);
        when(techDebtItemRepository.findByProjectIdAndStatus(projectId, DebtStatus.IDENTIFIED, pageable)).thenReturn(page);

        PageResponse<TechDebtItemResponse> result =
                techDebtService.getTechDebtByStatus(projectId, DebtStatus.IDENTIFIED, pageable);

        assertEquals(1, result.content().size());
    }

    // --- getTechDebtByCategory ---

    @Test
    void getTechDebtByCategory_success() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<TechDebtItem> page = new PageImpl<>(List.of(debtItem), pageable, 1);

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(true);
        when(techDebtItemRepository.findByProjectIdAndCategory(projectId, DebtCategory.CODE, pageable)).thenReturn(page);

        PageResponse<TechDebtItemResponse> result =
                techDebtService.getTechDebtByCategory(projectId, DebtCategory.CODE, pageable);

        assertEquals(1, result.content().size());
    }

    // --- updateTechDebtStatus ---

    @Test
    void updateTechDebtStatus_success() {
        UpdateTechDebtStatusRequest request = new UpdateTechDebtStatusRequest(DebtStatus.RESOLVED, jobId);

        when(techDebtItemRepository.findById(itemId)).thenReturn(Optional.of(debtItem));
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(true);
        when(qaJobRepository.findById(jobId)).thenReturn(Optional.of(qaJob));
        when(techDebtItemRepository.save(any(TechDebtItem.class))).thenReturn(debtItem);

        TechDebtItemResponse response = techDebtService.updateTechDebtStatus(itemId, request);

        assertEquals(DebtStatus.RESOLVED, debtItem.getStatus());
        assertNotNull(debtItem.getResolvedJob());
    }

    @Test
    void updateTechDebtStatus_withoutResolvedJob_success() {
        UpdateTechDebtStatusRequest request = new UpdateTechDebtStatusRequest(DebtStatus.PLANNED, null);

        when(techDebtItemRepository.findById(itemId)).thenReturn(Optional.of(debtItem));
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(true);
        when(techDebtItemRepository.save(any(TechDebtItem.class))).thenReturn(debtItem);

        techDebtService.updateTechDebtStatus(itemId, request);

        assertEquals(DebtStatus.PLANNED, debtItem.getStatus());
    }

    @Test
    void updateTechDebtStatus_itemNotFound_throws() {
        UpdateTechDebtStatusRequest request = new UpdateTechDebtStatusRequest(DebtStatus.RESOLVED, null);
        when(techDebtItemRepository.findById(itemId)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> techDebtService.updateTechDebtStatus(itemId, request));
    }

    // --- deleteTechDebtItem ---

    @Test
    void deleteTechDebtItem_asOwner_success() {
        TeamMember ownerMember = TeamMember.builder().role(TeamRole.OWNER).build();

        when(techDebtItemRepository.findById(itemId)).thenReturn(Optional.of(debtItem));
        when(teamMemberRepository.findByTeamIdAndUserId(teamId, userId)).thenReturn(Optional.of(ownerMember));

        techDebtService.deleteTechDebtItem(itemId);

        verify(techDebtItemRepository).delete(debtItem);
    }

    @Test
    void deleteTechDebtItem_asAdmin_success() {
        TeamMember adminMember = TeamMember.builder().role(TeamRole.ADMIN).build();

        when(techDebtItemRepository.findById(itemId)).thenReturn(Optional.of(debtItem));
        when(teamMemberRepository.findByTeamIdAndUserId(teamId, userId)).thenReturn(Optional.of(adminMember));

        techDebtService.deleteTechDebtItem(itemId);

        verify(techDebtItemRepository).delete(debtItem);
    }

    @Test
    void deleteTechDebtItem_asMember_throws() {
        TeamMember memberRole = TeamMember.builder().role(TeamRole.MEMBER).build();

        when(techDebtItemRepository.findById(itemId)).thenReturn(Optional.of(debtItem));
        when(teamMemberRepository.findByTeamIdAndUserId(teamId, userId)).thenReturn(Optional.of(memberRole));

        assertThrows(AccessDeniedException.class, () -> techDebtService.deleteTechDebtItem(itemId));
    }

    @Test
    void deleteTechDebtItem_notTeamMember_throws() {
        when(techDebtItemRepository.findById(itemId)).thenReturn(Optional.of(debtItem));
        when(teamMemberRepository.findByTeamIdAndUserId(teamId, userId)).thenReturn(Optional.empty());

        assertThrows(AccessDeniedException.class, () -> techDebtService.deleteTechDebtItem(itemId));
    }

    @Test
    void deleteTechDebtItem_itemNotFound_throws() {
        when(techDebtItemRepository.findById(itemId)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> techDebtService.deleteTechDebtItem(itemId));
    }

    // --- getDebtSummary ---

    @Test
    void getDebtSummary_success() {
        TechDebtItem item1 = TechDebtItem.builder().project(project).category(DebtCategory.CODE)
                .title("t1").status(DebtStatus.IDENTIFIED).businessImpact(BusinessImpact.CRITICAL).build();
        TechDebtItem item2 = TechDebtItem.builder().project(project).category(DebtCategory.CODE)
                .title("t2").status(DebtStatus.RESOLVED).businessImpact(BusinessImpact.LOW).build();
        TechDebtItem item3 = TechDebtItem.builder().project(project).category(DebtCategory.TEST)
                .title("t3").status(DebtStatus.PLANNED).businessImpact(BusinessImpact.MEDIUM).build();

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(true);
        when(techDebtItemRepository.findByProjectId(projectId)).thenReturn(List.of(item1, item2, item3));

        Map<String, Object> summary = techDebtService.getDebtSummary(projectId);

        assertEquals(3L, summary.get("total"));
        assertEquals(2L, summary.get("open")); // IDENTIFIED + PLANNED
        assertEquals(1L, summary.get("critical"));
    }

    @Test
    void getDebtSummary_projectNotFound_throws() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> techDebtService.getDebtSummary(projectId));
    }

    private void setSecurityContext(UUID userId) {
        var auth = new UsernamePasswordAuthenticationToken(userId, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
