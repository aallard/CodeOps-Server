package com.codeops.service;

import com.codeops.dto.request.BulkUpdateFindingsRequest;
import com.codeops.dto.request.CreateFindingRequest;
import com.codeops.dto.request.UpdateFindingStatusRequest;
import com.codeops.dto.response.FindingResponse;
import com.codeops.dto.response.PageResponse;
import com.codeops.entity.*;
import com.codeops.entity.enums.*;
import com.codeops.repository.FindingRepository;
import com.codeops.repository.QaJobRepository;
import com.codeops.repository.TeamMemberRepository;
import com.codeops.repository.UserRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FindingServiceTest {

    @Mock private FindingRepository findingRepository;
    @Mock private QaJobRepository qaJobRepository;
    @Mock private UserRepository userRepository;
    @Mock private TeamMemberRepository teamMemberRepository;

    @InjectMocks
    private FindingService findingService;

    private UUID userId;
    private UUID teamId;
    private UUID projectId;
    private UUID jobId;
    private UUID findingId;
    private Team team;
    private Project project;
    private QaJob job;
    private User user;
    private Finding finding;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        teamId = UUID.randomUUID();
        projectId = UUID.randomUUID();
        jobId = UUID.randomUUID();
        findingId = UUID.randomUUID();

        setSecurityContext(userId);

        team = Team.builder().name("Test Team").build();
        team.setId(teamId);
        team.setCreatedAt(Instant.now());

        user = User.builder()
                .email("test@codeops.dev")
                .passwordHash("hash")
                .displayName("Test User")
                .isActive(true)
                .build();
        user.setId(userId);
        user.setCreatedAt(Instant.now());

        project = Project.builder()
                .team(team)
                .name("Test Project")
                .createdBy(user)
                .build();
        project.setId(projectId);
        project.setCreatedAt(Instant.now());

        job = QaJob.builder()
                .project(project)
                .mode(JobMode.AUDIT)
                .status(JobStatus.RUNNING)
                .name("Test Job")
                .startedBy(user)
                .build();
        job.setId(jobId);
        job.setCreatedAt(Instant.now());

        finding = Finding.builder()
                .job(job)
                .agentType(AgentType.SECURITY)
                .severity(Severity.HIGH)
                .title("SQL Injection")
                .description("Potential SQL injection vulnerability")
                .filePath("src/main/java/Foo.java")
                .lineNumber(42)
                .recommendation("Use parameterized queries")
                .evidence("String concatenation in query")
                .effortEstimate(Effort.M)
                .debtCategory(DebtCategory.CODE)
                .status(FindingStatus.OPEN)
                .build();
        finding.setId(findingId);
        finding.setCreatedAt(Instant.now());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // --- createFinding ---

    @Test
    void createFinding_success() {
        CreateFindingRequest request = new CreateFindingRequest(
                jobId, AgentType.SECURITY, Severity.HIGH, "SQL Injection",
                "Potential SQL injection vulnerability", "src/main/java/Foo.java", 42,
                "Use parameterized queries", "String concatenation in query", Effort.M, DebtCategory.CODE
        );

        when(qaJobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(true);
        when(findingRepository.save(any(Finding.class))).thenReturn(finding);

        FindingResponse response = findingService.createFinding(request);

        assertNotNull(response);
        assertEquals(findingId, response.id());
        assertEquals(jobId, response.jobId());
        assertEquals(AgentType.SECURITY, response.agentType());
        assertEquals(Severity.HIGH, response.severity());
        assertEquals("SQL Injection", response.title());
        assertEquals(FindingStatus.OPEN, response.status());
        verify(findingRepository).save(any(Finding.class));
    }

    @Test
    void createFinding_jobNotFound_throws() {
        CreateFindingRequest request = new CreateFindingRequest(
                jobId, AgentType.SECURITY, Severity.HIGH, "Title",
                null, null, null, null, null, null, null
        );

        when(qaJobRepository.findById(jobId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> findingService.createFinding(request));
        verify(findingRepository, never()).save(any());
    }

    @Test
    void createFinding_notTeamMember_throws() {
        CreateFindingRequest request = new CreateFindingRequest(
                jobId, AgentType.SECURITY, Severity.HIGH, "Title",
                null, null, null, null, null, null, null
        );

        when(qaJobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> findingService.createFinding(request));
        verify(findingRepository, never()).save(any());
    }

    // --- createFindings (batch) ---

    @Test
    void createFindings_success() {
        CreateFindingRequest req1 = new CreateFindingRequest(
                jobId, AgentType.SECURITY, Severity.HIGH, "Finding 1",
                null, null, null, null, null, null, null
        );
        CreateFindingRequest req2 = new CreateFindingRequest(
                jobId, AgentType.SECURITY, Severity.MEDIUM, "Finding 2",
                null, null, null, null, null, null, null
        );

        Finding finding2 = Finding.builder()
                .job(job)
                .agentType(AgentType.SECURITY)
                .severity(Severity.MEDIUM)
                .title("Finding 2")
                .status(FindingStatus.OPEN)
                .build();
        finding2.setId(UUID.randomUUID());
        finding2.setCreatedAt(Instant.now());

        when(qaJobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(true);
        when(findingRepository.saveAll(anyList())).thenReturn(List.of(finding, finding2));

        List<FindingResponse> responses = findingService.createFindings(List.of(req1, req2));

        assertEquals(2, responses.size());
        verify(findingRepository).saveAll(anyList());
    }

    @Test
    void createFindings_emptyList_returnsEmpty() {
        List<FindingResponse> responses = findingService.createFindings(List.of());

        assertTrue(responses.isEmpty());
        verifyNoInteractions(qaJobRepository);
        verifyNoInteractions(findingRepository);
    }

    @Test
    void createFindings_differentJobs_throws() {
        UUID otherJobId = UUID.randomUUID();
        CreateFindingRequest req1 = new CreateFindingRequest(
                jobId, AgentType.SECURITY, Severity.HIGH, "Finding 1",
                null, null, null, null, null, null, null
        );
        CreateFindingRequest req2 = new CreateFindingRequest(
                otherJobId, AgentType.SECURITY, Severity.MEDIUM, "Finding 2",
                null, null, null, null, null, null, null
        );

        assertThrows(IllegalArgumentException.class, () -> findingService.createFindings(List.of(req1, req2)));
        verifyNoInteractions(findingRepository);
    }

    @Test
    void createFindings_jobNotFound_throws() {
        CreateFindingRequest req = new CreateFindingRequest(
                jobId, AgentType.SECURITY, Severity.HIGH, "Finding 1",
                null, null, null, null, null, null, null
        );

        when(qaJobRepository.findById(jobId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> findingService.createFindings(List.of(req)));
    }

    // --- getFinding ---

    @Test
    void getFinding_success() {
        when(findingRepository.findById(findingId)).thenReturn(Optional.of(finding));
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(true);

        FindingResponse response = findingService.getFinding(findingId);

        assertEquals(findingId, response.id());
        assertEquals("SQL Injection", response.title());
    }

    @Test
    void getFinding_notFound_throws() {
        when(findingRepository.findById(findingId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> findingService.getFinding(findingId));
    }

    @Test
    void getFinding_notTeamMember_throws() {
        when(findingRepository.findById(findingId)).thenReturn(Optional.of(finding));
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> findingService.getFinding(findingId));
    }

    // --- getFindingsForJob ---

    @Test
    void getFindingsForJob_success() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Finding> page = new PageImpl<>(List.of(finding), pageable, 1);

        when(qaJobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(true);
        when(findingRepository.findByJobId(jobId, pageable)).thenReturn(page);

        PageResponse<FindingResponse> response = findingService.getFindingsForJob(jobId, pageable);

        assertEquals(1, response.content().size());
        assertEquals(1, response.totalElements());
        assertTrue(response.isLast());
    }

    @Test
    void getFindingsForJob_jobNotFound_throws() {
        Pageable pageable = PageRequest.of(0, 20);
        when(qaJobRepository.findById(jobId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> findingService.getFindingsForJob(jobId, pageable));
    }

    // --- getFindingsByJobAndSeverity ---

    @Test
    void getFindingsByJobAndSeverity_success() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Finding> page = new PageImpl<>(List.of(finding), pageable, 1);

        when(qaJobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(true);
        when(findingRepository.findByJobIdAndSeverity(jobId, Severity.HIGH, pageable)).thenReturn(page);

        PageResponse<FindingResponse> response = findingService.getFindingsByJobAndSeverity(jobId, Severity.HIGH, pageable);

        assertEquals(1, response.content().size());
        assertEquals(Severity.HIGH, response.content().get(0).severity());
    }

    @Test
    void getFindingsByJobAndSeverity_jobNotFound_throws() {
        Pageable pageable = PageRequest.of(0, 20);
        when(qaJobRepository.findById(jobId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> findingService.getFindingsByJobAndSeverity(jobId, Severity.HIGH, pageable));
    }

    // --- getFindingsByJobAndAgent ---

    @Test
    void getFindingsByJobAndAgent_success() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Finding> page = new PageImpl<>(List.of(finding), pageable, 1);

        when(qaJobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(true);
        when(findingRepository.findByJobIdAndAgentType(jobId, AgentType.SECURITY, pageable)).thenReturn(page);

        PageResponse<FindingResponse> response = findingService.getFindingsByJobAndAgent(jobId, AgentType.SECURITY, pageable);

        assertEquals(1, response.content().size());
        assertEquals(AgentType.SECURITY, response.content().get(0).agentType());
    }

    @Test
    void getFindingsByJobAndAgent_jobNotFound_throws() {
        Pageable pageable = PageRequest.of(0, 20);
        when(qaJobRepository.findById(jobId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> findingService.getFindingsByJobAndAgent(jobId, AgentType.SECURITY, pageable));
    }

    // --- getFindingsByJobAndStatus ---

    @Test
    void getFindingsByJobAndStatus_success() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Finding> page = new PageImpl<>(List.of(finding), pageable, 1);

        when(qaJobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(true);
        when(findingRepository.findByJobIdAndStatus(jobId, FindingStatus.OPEN, pageable)).thenReturn(page);

        PageResponse<FindingResponse> response = findingService.getFindingsByJobAndStatus(jobId, FindingStatus.OPEN, pageable);

        assertEquals(1, response.content().size());
        assertEquals(FindingStatus.OPEN, response.content().get(0).status());
    }

    @Test
    void getFindingsByJobAndStatus_jobNotFound_throws() {
        Pageable pageable = PageRequest.of(0, 20);
        when(qaJobRepository.findById(jobId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> findingService.getFindingsByJobAndStatus(jobId, FindingStatus.OPEN, pageable));
    }

    // --- updateFindingStatus ---

    @Test
    void updateFindingStatus_success() {
        UpdateFindingStatusRequest request = new UpdateFindingStatusRequest(FindingStatus.ACKNOWLEDGED);

        when(findingRepository.findById(findingId)).thenReturn(Optional.of(finding));
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(true);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(findingRepository.save(any(Finding.class))).thenReturn(finding);

        FindingResponse response = findingService.updateFindingStatus(findingId, request);

        assertNotNull(response);
        assertEquals(FindingStatus.ACKNOWLEDGED, finding.getStatus());
        assertEquals(user, finding.getStatusChangedBy());
        assertNotNull(finding.getStatusChangedAt());
        verify(findingRepository).save(finding);
    }

    @Test
    void updateFindingStatus_findingNotFound_throws() {
        UpdateFindingStatusRequest request = new UpdateFindingStatusRequest(FindingStatus.FIXED);
        when(findingRepository.findById(findingId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> findingService.updateFindingStatus(findingId, request));
    }

    @Test
    void updateFindingStatus_userNotFound_throws() {
        UpdateFindingStatusRequest request = new UpdateFindingStatusRequest(FindingStatus.FIXED);

        when(findingRepository.findById(findingId)).thenReturn(Optional.of(finding));
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(true);
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> findingService.updateFindingStatus(findingId, request));
    }

    @Test
    void updateFindingStatus_notTeamMember_throws() {
        UpdateFindingStatusRequest request = new UpdateFindingStatusRequest(FindingStatus.FIXED);

        when(findingRepository.findById(findingId)).thenReturn(Optional.of(finding));
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(false);

        assertThrows(AccessDeniedException.class,
                () -> findingService.updateFindingStatus(findingId, request));
    }

    // --- bulkUpdateFindingStatus ---

    @Test
    void bulkUpdateFindingStatus_success() {
        Finding finding2 = Finding.builder()
                .job(job)
                .agentType(AgentType.CODE_QUALITY)
                .severity(Severity.MEDIUM)
                .title("Code smell")
                .status(FindingStatus.OPEN)
                .build();
        finding2.setId(UUID.randomUUID());
        finding2.setCreatedAt(Instant.now());

        List<UUID> ids = List.of(findingId, finding2.getId());
        BulkUpdateFindingsRequest request = new BulkUpdateFindingsRequest(ids, FindingStatus.ACKNOWLEDGED);

        when(findingRepository.findAllById(ids)).thenReturn(List.of(finding, finding2));
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(true);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(findingRepository.saveAll(anyList())).thenReturn(List.of(finding, finding2));

        List<FindingResponse> responses = findingService.bulkUpdateFindingStatus(request);

        assertEquals(2, responses.size());
        assertEquals(FindingStatus.ACKNOWLEDGED, finding.getStatus());
        assertEquals(FindingStatus.ACKNOWLEDGED, finding2.getStatus());
        assertNotNull(finding.getStatusChangedBy());
        assertNotNull(finding.getStatusChangedAt());
    }

    @Test
    void bulkUpdateFindingStatus_noFindingsFound_throws() {
        List<UUID> ids = List.of(UUID.randomUUID());
        BulkUpdateFindingsRequest request = new BulkUpdateFindingsRequest(ids, FindingStatus.FIXED);

        when(findingRepository.findAllById(ids)).thenReturn(List.of());

        assertThrows(EntityNotFoundException.class, () -> findingService.bulkUpdateFindingStatus(request));
    }

    @Test
    void bulkUpdateFindingStatus_differentJobs_throws() {
        QaJob otherJob = QaJob.builder()
                .project(project)
                .mode(JobMode.AUDIT)
                .status(JobStatus.RUNNING)
                .name("Other Job")
                .startedBy(user)
                .build();
        otherJob.setId(UUID.randomUUID());
        otherJob.setCreatedAt(Instant.now());

        Finding finding2 = Finding.builder()
                .job(otherJob)
                .agentType(AgentType.CODE_QUALITY)
                .severity(Severity.LOW)
                .title("Other finding")
                .status(FindingStatus.OPEN)
                .build();
        finding2.setId(UUID.randomUUID());
        finding2.setCreatedAt(Instant.now());

        List<UUID> ids = List.of(findingId, finding2.getId());
        BulkUpdateFindingsRequest request = new BulkUpdateFindingsRequest(ids, FindingStatus.FIXED);

        when(findingRepository.findAllById(ids)).thenReturn(List.of(finding, finding2));

        assertThrows(IllegalArgumentException.class, () -> findingService.bulkUpdateFindingStatus(request));
    }

    @Test
    void bulkUpdateFindingStatus_notTeamMember_throws() {
        List<UUID> ids = List.of(findingId);
        BulkUpdateFindingsRequest request = new BulkUpdateFindingsRequest(ids, FindingStatus.FIXED);

        when(findingRepository.findAllById(ids)).thenReturn(List.of(finding));
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> findingService.bulkUpdateFindingStatus(request));
    }

    @Test
    void bulkUpdateFindingStatus_userNotFound_throws() {
        List<UUID> ids = List.of(findingId);
        BulkUpdateFindingsRequest request = new BulkUpdateFindingsRequest(ids, FindingStatus.FIXED);

        when(findingRepository.findAllById(ids)).thenReturn(List.of(finding));
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(true);
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> findingService.bulkUpdateFindingStatus(request));
    }

    // --- countFindingsBySeverity ---

    @Test
    void countFindingsBySeverity_success() {
        when(qaJobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(true);
        when(findingRepository.countByJobIdAndSeverity(jobId, Severity.CRITICAL)).thenReturn(2L);
        when(findingRepository.countByJobIdAndSeverity(jobId, Severity.HIGH)).thenReturn(5L);
        when(findingRepository.countByJobIdAndSeverity(jobId, Severity.MEDIUM)).thenReturn(10L);
        when(findingRepository.countByJobIdAndSeverity(jobId, Severity.LOW)).thenReturn(3L);

        Map<Severity, Long> counts = findingService.countFindingsBySeverity(jobId);

        assertEquals(4, counts.size());
        assertEquals(2L, counts.get(Severity.CRITICAL));
        assertEquals(5L, counts.get(Severity.HIGH));
        assertEquals(10L, counts.get(Severity.MEDIUM));
        assertEquals(3L, counts.get(Severity.LOW));
    }

    @Test
    void countFindingsBySeverity_jobNotFound_throws() {
        when(qaJobRepository.findById(jobId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> findingService.countFindingsBySeverity(jobId));
    }

    @Test
    void countFindingsBySeverity_notTeamMember_throws() {
        when(qaJobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> findingService.countFindingsBySeverity(jobId));
    }

    // --- mapToResponse covers statusChangedBy null ---

    @Test
    void mapToResponse_statusChangedByNull_returnsNullUserId() {
        finding.setStatusChangedBy(null);

        when(findingRepository.findById(findingId)).thenReturn(Optional.of(finding));
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(true);

        FindingResponse response = findingService.getFinding(findingId);

        assertNull(response.statusChangedBy());
    }

    @Test
    void mapToResponse_statusChangedByPresent_returnsUserId() {
        finding.setStatusChangedBy(user);

        when(findingRepository.findById(findingId)).thenReturn(Optional.of(finding));
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(true);

        FindingResponse response = findingService.getFinding(findingId);

        assertEquals(userId, response.statusChangedBy());
    }

    private void setSecurityContext(UUID userId) {
        var auth = new UsernamePasswordAuthenticationToken(userId, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
