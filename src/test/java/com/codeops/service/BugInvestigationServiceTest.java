package com.codeops.service;

import com.codeops.dto.request.CreateBugInvestigationRequest;
import com.codeops.dto.request.UpdateBugInvestigationRequest;
import com.codeops.dto.response.BugInvestigationResponse;
import com.codeops.entity.*;
import com.codeops.repository.BugInvestigationRepository;
import com.codeops.repository.QaJobRepository;
import com.codeops.repository.TeamMemberRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BugInvestigationServiceTest {

    @Mock private BugInvestigationRepository bugInvestigationRepository;
    @Mock private QaJobRepository qaJobRepository;
    @Mock private TeamMemberRepository teamMemberRepository;
    @Mock private S3StorageService s3StorageService;

    @InjectMocks
    private BugInvestigationService bugInvestigationService;

    private UUID userId;
    private UUID teamId;
    private UUID jobId;
    private UUID investigationId;
    private Team team;
    private Project project;
    private QaJob job;
    private BugInvestigation investigation;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        teamId = UUID.randomUUID();
        jobId = UUID.randomUUID();
        investigationId = UUID.randomUUID();

        team = Team.builder().name("Test Team").build();
        team.setId(teamId);

        project = Project.builder().team(team).name("Test Project").build();
        project.setId(UUID.randomUUID());

        job = QaJob.builder().project(project).build();
        job.setId(jobId);
        job.setCreatedAt(Instant.now());

        investigation = BugInvestigation.builder()
                .job(job)
                .jiraKey("BUG-123")
                .jiraSummary("Null pointer in login")
                .jiraDescription("NPE when user logs in with empty email")
                .rcaPostedToJira(false)
                .fixTasksCreatedInJira(false)
                .build();
        investigation.setId(investigationId);
        investigation.setCreatedAt(Instant.now());

        setSecurityContext(userId);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // --- createInvestigation ---

    @Test
    void createInvestigation_success() {
        CreateBugInvestigationRequest request = new CreateBugInvestigationRequest(
                jobId, "BUG-123", "Null pointer in login", "NPE when user logs in",
                null, null, null, "Additional context");

        when(qaJobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(true);
        when(bugInvestigationRepository.save(any(BugInvestigation.class))).thenAnswer(inv -> {
            BugInvestigation bi = inv.getArgument(0);
            bi.setId(investigationId);
            bi.setCreatedAt(Instant.now());
            return bi;
        });

        BugInvestigationResponse response = bugInvestigationService.createInvestigation(request);

        assertNotNull(response);
        assertEquals("BUG-123", response.jiraKey());
        assertEquals("Null pointer in login", response.jiraSummary());
        assertFalse(response.rcaPostedToJira());
        assertFalse(response.fixTasksCreatedInJira());
        verify(bugInvestigationRepository).save(any(BugInvestigation.class));
    }

    @Test
    void createInvestigation_jobNotFound_throws() {
        CreateBugInvestigationRequest request = new CreateBugInvestigationRequest(
                jobId, "BUG-1", null, null, null, null, null, null);

        when(qaJobRepository.findById(jobId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> bugInvestigationService.createInvestigation(request));
    }

    @Test
    void createInvestigation_notTeamMember_throws() {
        CreateBugInvestigationRequest request = new CreateBugInvestigationRequest(
                jobId, "BUG-1", null, null, null, null, null, null);

        when(qaJobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> bugInvestigationService.createInvestigation(request));
    }

    // --- getInvestigation ---

    @Test
    void getInvestigation_success() {
        when(bugInvestigationRepository.findById(investigationId)).thenReturn(Optional.of(investigation));
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(true);

        BugInvestigationResponse response = bugInvestigationService.getInvestigation(investigationId);

        assertNotNull(response);
        assertEquals(investigationId, response.id());
        assertEquals("BUG-123", response.jiraKey());
    }

    @Test
    void getInvestigation_notFound_throws() {
        when(bugInvestigationRepository.findById(investigationId)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> bugInvestigationService.getInvestigation(investigationId));
    }

    @Test
    void getInvestigation_notTeamMember_throws() {
        when(bugInvestigationRepository.findById(investigationId)).thenReturn(Optional.of(investigation));
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> bugInvestigationService.getInvestigation(investigationId));
    }

    // --- getInvestigationByJob ---

    @Test
    void getInvestigationByJob_success() {
        when(bugInvestigationRepository.findByJobId(jobId)).thenReturn(Optional.of(investigation));
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(true);

        BugInvestigationResponse response = bugInvestigationService.getInvestigationByJob(jobId);

        assertNotNull(response);
        assertEquals(jobId, response.jobId());
    }

    @Test
    void getInvestigationByJob_notFound_throws() {
        when(bugInvestigationRepository.findByJobId(jobId)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> bugInvestigationService.getInvestigationByJob(jobId));
    }

    // --- getInvestigationByJiraKey ---

    @Test
    void getInvestigationByJiraKey_success() {
        when(bugInvestigationRepository.findByJiraKey("BUG-123")).thenReturn(Optional.of(investigation));
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(true);

        BugInvestigationResponse response = bugInvestigationService.getInvestigationByJiraKey("BUG-123");

        assertNotNull(response);
        assertEquals("BUG-123", response.jiraKey());
    }

    @Test
    void getInvestigationByJiraKey_notFound_throws() {
        when(bugInvestigationRepository.findByJiraKey("NONE-999")).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class,
                () -> bugInvestigationService.getInvestigationByJiraKey("NONE-999"));
    }

    // --- updateInvestigation ---

    @Test
    void updateInvestigation_rcaMd_success() {
        UpdateBugInvestigationRequest request = new UpdateBugInvestigationRequest(
                "## Root Cause Analysis", null, null, null, null);

        when(bugInvestigationRepository.findById(investigationId)).thenReturn(Optional.of(investigation));
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(true);
        when(bugInvestigationRepository.save(any(BugInvestigation.class))).thenReturn(investigation);

        bugInvestigationService.updateInvestigation(investigationId, request);

        assertEquals("## Root Cause Analysis", investigation.getRcaMd());
    }

    @Test
    void updateInvestigation_allFields_success() {
        UpdateBugInvestigationRequest request = new UpdateBugInvestigationRequest(
                "## RCA", "## Impact", "s3://rca.md", true, true);

        when(bugInvestigationRepository.findById(investigationId)).thenReturn(Optional.of(investigation));
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(true);
        when(bugInvestigationRepository.save(any(BugInvestigation.class))).thenReturn(investigation);

        BugInvestigationResponse response = bugInvestigationService.updateInvestigation(investigationId, request);

        assertEquals("## RCA", investigation.getRcaMd());
        assertEquals("## Impact", investigation.getImpactAssessmentMd());
        assertEquals("s3://rca.md", investigation.getRcaS3Key());
        assertTrue(investigation.getRcaPostedToJira());
        assertTrue(investigation.getFixTasksCreatedInJira());
    }

    @Test
    void updateInvestigation_nullFields_noChange() {
        investigation.setRcaMd("existing");
        UpdateBugInvestigationRequest request = new UpdateBugInvestigationRequest(
                null, null, null, null, null);

        when(bugInvestigationRepository.findById(investigationId)).thenReturn(Optional.of(investigation));
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(true);
        when(bugInvestigationRepository.save(any(BugInvestigation.class))).thenReturn(investigation);

        bugInvestigationService.updateInvestigation(investigationId, request);

        assertEquals("existing", investigation.getRcaMd());
    }

    @Test
    void updateInvestigation_notFound_throws() {
        UpdateBugInvestigationRequest request = new UpdateBugInvestigationRequest(
                "content", null, null, null, null);

        when(bugInvestigationRepository.findById(investigationId)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class,
                () -> bugInvestigationService.updateInvestigation(investigationId, request));
    }

    @Test
    void updateInvestigation_notTeamMember_throws() {
        UpdateBugInvestigationRequest request = new UpdateBugInvestigationRequest(
                "content", null, null, null, null);

        when(bugInvestigationRepository.findById(investigationId)).thenReturn(Optional.of(investigation));
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(false);

        assertThrows(AccessDeniedException.class,
                () -> bugInvestigationService.updateInvestigation(investigationId, request));
    }

    // --- uploadRca ---

    @Test
    void uploadRca_success() {
        String rcaMd = "## Root Cause Analysis\nMemory leak identified.";

        String key = bugInvestigationService.uploadRca(jobId, rcaMd);

        assertEquals("reports/" + jobId + "/rca.md", key);
        verify(s3StorageService).upload(eq(key), any(byte[].class), eq("text/markdown"));
    }

    private void setSecurityContext(UUID userId) {
        var auth = new UsernamePasswordAuthenticationToken(userId, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
