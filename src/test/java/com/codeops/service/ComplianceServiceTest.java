package com.codeops.service;

import com.codeops.dto.request.CreateComplianceItemRequest;
import com.codeops.dto.request.CreateSpecificationRequest;
import com.codeops.dto.response.ComplianceItemResponse;
import com.codeops.dto.response.PageResponse;
import com.codeops.dto.response.SpecificationResponse;
import com.codeops.entity.*;
import com.codeops.entity.enums.AgentType;
import com.codeops.entity.enums.ComplianceStatus;
import com.codeops.entity.enums.SpecType;
import com.codeops.repository.ComplianceItemRepository;
import com.codeops.repository.QaJobRepository;
import com.codeops.repository.SpecificationRepository;
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
class ComplianceServiceTest {

    @Mock private ComplianceItemRepository complianceItemRepository;
    @Mock private SpecificationRepository specificationRepository;
    @Mock private QaJobRepository qaJobRepository;
    @Mock private TeamMemberRepository teamMemberRepository;

    @InjectMocks
    private ComplianceService complianceService;

    private UUID userId;
    private UUID teamId;
    private UUID jobId;
    private UUID specId;
    private Team team;
    private Project project;
    private QaJob job;
    private Specification spec;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        teamId = UUID.randomUUID();
        jobId = UUID.randomUUID();
        specId = UUID.randomUUID();

        team = Team.builder().name("Test Team").build();
        team.setId(teamId);

        project = Project.builder().team(team).name("Test Project").build();
        project.setId(UUID.randomUUID());

        job = QaJob.builder().project(project).build();
        job.setId(jobId);
        job.setCreatedAt(Instant.now());

        spec = Specification.builder().job(job).name("API Spec").specType(SpecType.OPENAPI).s3Key("specs/api.yaml").build();
        spec.setId(specId);
        spec.setCreatedAt(Instant.now());

        setSecurityContext(userId);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // --- createSpecification ---

    @Test
    void createSpecification_success() {
        CreateSpecificationRequest request = new CreateSpecificationRequest(
                jobId, "API Spec", SpecType.OPENAPI, "specs/api.yaml");

        when(qaJobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(true);
        when(specificationRepository.save(any(Specification.class))).thenAnswer(inv -> {
            Specification s = inv.getArgument(0);
            s.setId(specId);
            s.setCreatedAt(Instant.now());
            return s;
        });

        SpecificationResponse response = complianceService.createSpecification(request);

        assertNotNull(response);
        assertEquals("API Spec", response.name());
        assertEquals(SpecType.OPENAPI, response.specType());
    }

    @Test
    void createSpecification_jobNotFound_throws() {
        CreateSpecificationRequest request = new CreateSpecificationRequest(
                jobId, "Spec", SpecType.MARKDOWN, "specs/spec.md");

        when(qaJobRepository.findById(jobId)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> complianceService.createSpecification(request));
    }

    @Test
    void createSpecification_notTeamMember_throws() {
        CreateSpecificationRequest request = new CreateSpecificationRequest(
                jobId, "Spec", SpecType.MARKDOWN, "specs/spec.md");

        when(qaJobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> complianceService.createSpecification(request));
    }

    // --- getSpecificationsForJob ---

    @Test
    void getSpecificationsForJob_success() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Specification> page = new PageImpl<>(List.of(spec), pageable, 1);

        when(qaJobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(true);
        when(specificationRepository.findByJobId(jobId, pageable)).thenReturn(page);

        PageResponse<SpecificationResponse> result = complianceService.getSpecificationsForJob(jobId, pageable);

        assertEquals(1, result.content().size());
        assertEquals("API Spec", result.content().get(0).name());
    }

    // --- createComplianceItem ---

    @Test
    void createComplianceItem_success_withSpec() {
        CreateComplianceItemRequest request = new CreateComplianceItemRequest(
                jobId, "All endpoints must return JSON", specId, ComplianceStatus.MET,
                "Verified all endpoints", AgentType.API_CONTRACT, "Good");

        when(qaJobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(true);
        when(specificationRepository.findById(specId)).thenReturn(Optional.of(spec));
        when(complianceItemRepository.save(any(ComplianceItem.class))).thenAnswer(inv -> {
            ComplianceItem item = inv.getArgument(0);
            item.setId(UUID.randomUUID());
            item.setCreatedAt(Instant.now());
            return item;
        });

        ComplianceItemResponse response = complianceService.createComplianceItem(request);

        assertNotNull(response);
        assertEquals("All endpoints must return JSON", response.requirement());
        assertEquals(ComplianceStatus.MET, response.status());
        assertEquals(specId, response.specId());
        assertEquals("API Spec", response.specName());
    }

    @Test
    void createComplianceItem_success_withoutSpec() {
        CreateComplianceItemRequest request = new CreateComplianceItemRequest(
                jobId, "Code must compile", null, ComplianceStatus.MET,
                "Build succeeds", AgentType.BUILD_HEALTH, null);

        when(qaJobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(true);
        when(complianceItemRepository.save(any(ComplianceItem.class))).thenAnswer(inv -> {
            ComplianceItem item = inv.getArgument(0);
            item.setId(UUID.randomUUID());
            item.setCreatedAt(Instant.now());
            return item;
        });

        ComplianceItemResponse response = complianceService.createComplianceItem(request);

        assertNull(response.specId());
        assertNull(response.specName());
    }

    @Test
    void createComplianceItem_specNotFound_throws() {
        UUID missingSpecId = UUID.randomUUID();
        CreateComplianceItemRequest request = new CreateComplianceItemRequest(
                jobId, "Requirement", missingSpecId, ComplianceStatus.MET, null, null, null);

        when(qaJobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(true);
        when(specificationRepository.findById(missingSpecId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> complianceService.createComplianceItem(request));
    }

    // --- createComplianceItems ---

    @Test
    void createComplianceItems_emptyList_returnsEmpty() {
        List<ComplianceItemResponse> result = complianceService.createComplianceItems(List.of());
        assertTrue(result.isEmpty());
    }

    @Test
    void createComplianceItems_success() {
        CreateComplianceItemRequest req1 = new CreateComplianceItemRequest(
                jobId, "Req 1", null, ComplianceStatus.MET, null, null, null);
        CreateComplianceItemRequest req2 = new CreateComplianceItemRequest(
                jobId, "Req 2", null, ComplianceStatus.PARTIAL, null, null, null);

        when(qaJobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(true);
        when(complianceItemRepository.saveAll(anyList())).thenAnswer(inv -> {
            List<ComplianceItem> items = inv.getArgument(0);
            for (ComplianceItem item : items) {
                item.setId(UUID.randomUUID());
                item.setCreatedAt(Instant.now());
            }
            return items;
        });

        List<ComplianceItemResponse> responses = complianceService.createComplianceItems(List.of(req1, req2));

        assertEquals(2, responses.size());
    }

    @Test
    void createComplianceItems_differentJobs_throws() {
        UUID otherJobId = UUID.randomUUID();
        CreateComplianceItemRequest req1 = new CreateComplianceItemRequest(
                jobId, "Req 1", null, ComplianceStatus.MET, null, null, null);
        CreateComplianceItemRequest req2 = new CreateComplianceItemRequest(
                otherJobId, "Req 2", null, ComplianceStatus.MET, null, null, null);

        assertThrows(IllegalArgumentException.class,
                () -> complianceService.createComplianceItems(List.of(req1, req2)));
    }

    // --- getComplianceItemsForJob ---

    @Test
    void getComplianceItemsForJob_success() {
        Pageable pageable = PageRequest.of(0, 20);
        ComplianceItem item = ComplianceItem.builder()
                .job(job).requirement("Req").status(ComplianceStatus.MET).build();
        item.setId(UUID.randomUUID());
        item.setCreatedAt(Instant.now());
        Page<ComplianceItem> page = new PageImpl<>(List.of(item), pageable, 1);

        when(qaJobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(true);
        when(complianceItemRepository.findByJobId(jobId, pageable)).thenReturn(page);

        PageResponse<ComplianceItemResponse> result = complianceService.getComplianceItemsForJob(jobId, pageable);

        assertEquals(1, result.content().size());
    }

    // --- getComplianceItemsByStatus ---

    @Test
    void getComplianceItemsByStatus_success() {
        Pageable pageable = PageRequest.of(0, 20);
        ComplianceItem item = ComplianceItem.builder()
                .job(job).requirement("Req").status(ComplianceStatus.MISSING).build();
        item.setId(UUID.randomUUID());
        item.setCreatedAt(Instant.now());
        Page<ComplianceItem> page = new PageImpl<>(List.of(item), pageable, 1);

        when(qaJobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(true);
        when(complianceItemRepository.findByJobIdAndStatus(jobId, ComplianceStatus.MISSING, pageable)).thenReturn(page);

        PageResponse<ComplianceItemResponse> result =
                complianceService.getComplianceItemsByStatus(jobId, ComplianceStatus.MISSING, pageable);

        assertEquals(1, result.content().size());
        assertEquals(ComplianceStatus.MISSING, result.content().get(0).status());
    }

    // --- getComplianceSummary ---

    @Test
    void getComplianceSummary_success() {
        when(qaJobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(true);
        when(complianceItemRepository.findByJobIdAndStatus(jobId, ComplianceStatus.MET)).thenReturn(
                List.of(mock(ComplianceItem.class), mock(ComplianceItem.class)));
        when(complianceItemRepository.findByJobIdAndStatus(jobId, ComplianceStatus.PARTIAL)).thenReturn(
                List.of(mock(ComplianceItem.class)));
        when(complianceItemRepository.findByJobIdAndStatus(jobId, ComplianceStatus.MISSING)).thenReturn(
                List.of(mock(ComplianceItem.class)));
        when(complianceItemRepository.findByJobIdAndStatus(jobId, ComplianceStatus.NOT_APPLICABLE)).thenReturn(
                List.of());

        Map<String, Object> summary = complianceService.getComplianceSummary(jobId);

        assertEquals(2, summary.get("met"));
        assertEquals(1, summary.get("partial"));
        assertEquals(1, summary.get("missing"));
        assertEquals(0, summary.get("notApplicable"));
        assertEquals(4, summary.get("total"));
        // Score: (2*100 + 1*50) / (4*100) * 100 = 250/400*100 = 62.5 -> rounds to 63
        assertEquals(63L, summary.get("complianceScore"));
    }

    @Test
    void getComplianceSummary_noItems_zeroScore() {
        when(qaJobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(true);
        when(complianceItemRepository.findByJobIdAndStatus(eq(jobId), any())).thenReturn(List.of());

        Map<String, Object> summary = complianceService.getComplianceSummary(jobId);

        assertEquals(0, summary.get("total"));
        assertEquals(0L, summary.get("complianceScore"));
    }

    @Test
    void getComplianceSummary_jobNotFound_throws() {
        when(qaJobRepository.findById(jobId)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> complianceService.getComplianceSummary(jobId));
    }

    @Test
    void getComplianceSummary_allMet_fullScore() {
        when(qaJobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(true);
        when(complianceItemRepository.findByJobIdAndStatus(jobId, ComplianceStatus.MET)).thenReturn(
                List.of(mock(ComplianceItem.class), mock(ComplianceItem.class), mock(ComplianceItem.class)));
        when(complianceItemRepository.findByJobIdAndStatus(jobId, ComplianceStatus.PARTIAL)).thenReturn(List.of());
        when(complianceItemRepository.findByJobIdAndStatus(jobId, ComplianceStatus.MISSING)).thenReturn(List.of());
        when(complianceItemRepository.findByJobIdAndStatus(jobId, ComplianceStatus.NOT_APPLICABLE)).thenReturn(List.of());

        Map<String, Object> summary = complianceService.getComplianceSummary(jobId);

        assertEquals(100L, summary.get("complianceScore"));
    }

    private void setSecurityContext(UUID userId) {
        var auth = new UsernamePasswordAuthenticationToken(userId, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
