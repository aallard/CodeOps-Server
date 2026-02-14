package com.codeops.service;

import com.codeops.dto.request.CreateJobRequest;
import com.codeops.dto.request.UpdateJobRequest;
import com.codeops.dto.response.JobResponse;
import com.codeops.dto.response.JobSummaryResponse;
import com.codeops.dto.response.PageResponse;
import com.codeops.entity.*;
import com.codeops.entity.enums.*;
import com.codeops.repository.*;
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
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QaJobServiceTest {

    @Mock private QaJobRepository qaJobRepository;
    @Mock private AgentRunRepository agentRunRepository;
    @Mock private FindingRepository findingRepository;
    @Mock private ProjectRepository projectRepository;
    @Mock private UserRepository userRepository;
    @Mock private TeamMemberRepository teamMemberRepository;
    @Mock private ProjectService projectService;

    @InjectMocks
    private QaJobService qaJobService;

    private UUID userId;
    private UUID teamId;
    private UUID projectId;
    private UUID jobId;
    private Team team;
    private Project project;
    private QaJob job;
    private User user;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        teamId = UUID.randomUUID();
        projectId = UUID.randomUUID();
        jobId = UUID.randomUUID();

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
                .status(JobStatus.PENDING)
                .name("Full Audit")
                .branch("main")
                .configJson("{\"agents\":[\"SECURITY\"]}")
                .jiraTicketKey("PROJ-123")
                .totalFindings(0)
                .criticalCount(0)
                .highCount(0)
                .mediumCount(0)
                .lowCount(0)
                .startedBy(user)
                .build();
        job.setId(jobId);
        job.setCreatedAt(Instant.now());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // --- createJob ---

    @Test
    void createJob_success() {
        CreateJobRequest request = new CreateJobRequest(
                projectId, JobMode.AUDIT, "Full Audit", "main",
                "{\"agents\":[\"SECURITY\"]}", "PROJ-123"
        );

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(true);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(qaJobRepository.save(any(QaJob.class))).thenReturn(job);

        JobResponse response = qaJobService.createJob(request);

        assertNotNull(response);
        assertEquals(jobId, response.id());
        assertEquals(projectId, response.projectId());
        assertEquals("Test Project", response.projectName());
        assertEquals(JobMode.AUDIT, response.mode());
        assertEquals(JobStatus.PENDING, response.status());
        assertEquals("Full Audit", response.name());
        assertEquals("main", response.branch());
        assertEquals("PROJ-123", response.jiraTicketKey());
        assertEquals(userId, response.startedBy());
        assertEquals("Test User", response.startedByName());
        verify(qaJobRepository).save(any(QaJob.class));
    }

    @Test
    void createJob_projectNotFound_throws() {
        CreateJobRequest request = new CreateJobRequest(
                projectId, JobMode.AUDIT, "Job", null, null, null
        );
        when(projectRepository.findById(projectId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> qaJobService.createJob(request));
        verify(qaJobRepository, never()).save(any());
    }

    @Test
    void createJob_notTeamMember_throws() {
        CreateJobRequest request = new CreateJobRequest(
                projectId, JobMode.AUDIT, "Job", null, null, null
        );
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> qaJobService.createJob(request));
        verify(qaJobRepository, never()).save(any());
    }

    @Test
    void createJob_userNotFound_throws() {
        CreateJobRequest request = new CreateJobRequest(
                projectId, JobMode.AUDIT, "Job", null, null, null
        );
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(true);
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> qaJobService.createJob(request));
    }

    // --- getJob ---

    @Test
    void getJob_success() {
        when(qaJobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(true);

        JobResponse response = qaJobService.getJob(jobId);

        assertEquals(jobId, response.id());
        assertEquals("Full Audit", response.name());
        assertEquals(JobMode.AUDIT, response.mode());
    }

    @Test
    void getJob_notFound_throws() {
        when(qaJobRepository.findById(jobId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> qaJobService.getJob(jobId));
    }

    @Test
    void getJob_notTeamMember_throws() {
        when(qaJobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> qaJobService.getJob(jobId));
    }

    // --- getJobsForProject ---

    @Test
    void getJobsForProject_success() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<QaJob> page = new PageImpl<>(List.of(job), pageable, 1);

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(true);
        when(qaJobRepository.findByProjectId(projectId, pageable)).thenReturn(page);

        PageResponse<JobSummaryResponse> response = qaJobService.getJobsForProject(projectId, pageable);

        assertEquals(1, response.content().size());
        assertEquals(1, response.totalElements());
        assertTrue(response.isLast());
        assertEquals("Test Project", response.content().get(0).projectName());
    }

    @Test
    void getJobsForProject_projectNotFound_throws() {
        Pageable pageable = PageRequest.of(0, 20);
        when(projectRepository.findById(projectId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> qaJobService.getJobsForProject(projectId, pageable));
    }

    @Test
    void getJobsForProject_notTeamMember_throws() {
        Pageable pageable = PageRequest.of(0, 20);
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(false);

        assertThrows(AccessDeniedException.class,
                () -> qaJobService.getJobsForProject(projectId, pageable));
    }

    // --- getJobsByUser ---

    @Test
    void getJobsByUser_success() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<QaJob> page = new PageImpl<>(List.of(job), pageable, 1);

        when(qaJobRepository.findByStartedById(userId, pageable)).thenReturn(page);

        PageResponse<JobSummaryResponse> response = qaJobService.getJobsByUser(userId, pageable);

        assertEquals(1, response.content().size());
        assertEquals(jobId, response.content().get(0).id());
    }

    @Test
    void getJobsByUser_empty_returnsEmptyPage() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<QaJob> page = new PageImpl<>(List.of(), pageable, 0);

        when(qaJobRepository.findByStartedById(userId, pageable)).thenReturn(page);

        PageResponse<JobSummaryResponse> response = qaJobService.getJobsByUser(userId, pageable);

        assertTrue(response.content().isEmpty());
        assertEquals(0, response.totalElements());
    }

    // --- updateJob ---

    @Test
    void updateJob_allFields_success() {
        Instant startedAt = Instant.now().minusSeconds(60);
        Instant completedAt = Instant.now();
        UpdateJobRequest request = new UpdateJobRequest(
                JobStatus.COMPLETED, "## Summary\nAll good", JobResult.PASS,
                92, 15, 1, 3, 6, 5, completedAt, startedAt
        );

        when(qaJobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(true);
        when(qaJobRepository.save(any(QaJob.class))).thenReturn(job);

        JobResponse response = qaJobService.updateJob(jobId, request);

        assertNotNull(response);
        assertEquals(JobStatus.COMPLETED, job.getStatus());
        assertEquals("## Summary\nAll good", job.getSummaryMd());
        assertEquals(JobResult.PASS, job.getOverallResult());
        assertEquals(92, job.getHealthScore());
        assertEquals(15, job.getTotalFindings());
        assertEquals(1, job.getCriticalCount());
        assertEquals(3, job.getHighCount());
        assertEquals(6, job.getMediumCount());
        assertEquals(5, job.getLowCount());
        assertEquals(completedAt, job.getCompletedAt());
        assertEquals(startedAt, job.getStartedAt());
        verify(projectService).updateHealthScore(projectId, 92);
    }

    @Test
    void updateJob_nullFields_notUpdated() {
        UpdateJobRequest request = new UpdateJobRequest(
                null, null, null, null, null, null, null, null, null, null, null
        );

        when(qaJobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(true);
        when(qaJobRepository.save(any(QaJob.class))).thenReturn(job);

        qaJobService.updateJob(jobId, request);

        assertEquals(JobStatus.PENDING, job.getStatus());
        assertNull(job.getSummaryMd());
        assertNull(job.getOverallResult());
        assertNull(job.getHealthScore());
        assertEquals(0, job.getTotalFindings());
        verifyNoInteractions(projectService);
    }

    @Test
    void updateJob_completedWithHealthScore_updatesProject() {
        UpdateJobRequest request = new UpdateJobRequest(
                JobStatus.COMPLETED, null, null, 85, null, null, null, null, null, null, null
        );

        when(qaJobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(true);
        when(qaJobRepository.save(any(QaJob.class))).thenReturn(job);

        qaJobService.updateJob(jobId, request);

        verify(projectService).updateHealthScore(projectId, 85);
    }

    @Test
    void updateJob_completedWithoutHealthScore_doesNotUpdateProject() {
        UpdateJobRequest request = new UpdateJobRequest(
                JobStatus.COMPLETED, null, null, null, null, null, null, null, null, null, null
        );

        when(qaJobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(true);
        when(qaJobRepository.save(any(QaJob.class))).thenReturn(job);

        qaJobService.updateJob(jobId, request);

        verifyNoInteractions(projectService);
    }

    @Test
    void updateJob_nonCompletedWithHealthScore_doesNotUpdateProject() {
        UpdateJobRequest request = new UpdateJobRequest(
                JobStatus.RUNNING, null, null, 85, null, null, null, null, null, null, null
        );

        when(qaJobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(true);
        when(qaJobRepository.save(any(QaJob.class))).thenReturn(job);

        qaJobService.updateJob(jobId, request);

        verifyNoInteractions(projectService);
    }

    @Test
    void updateJob_notFound_throws() {
        UpdateJobRequest request = new UpdateJobRequest(
                JobStatus.RUNNING, null, null, null, null, null, null, null, null, null, null
        );
        when(qaJobRepository.findById(jobId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> qaJobService.updateJob(jobId, request));
    }

    @Test
    void updateJob_notTeamMember_throws() {
        UpdateJobRequest request = new UpdateJobRequest(
                JobStatus.RUNNING, null, null, null, null, null, null, null, null, null, null
        );
        when(qaJobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> qaJobService.updateJob(jobId, request));
    }

    // --- deleteJob ---

    @Test
    void deleteJob_asOwner_success() {
        TeamMember ownerMember = TeamMember.builder()
                .team(team)
                .user(user)
                .role(TeamRole.OWNER)
                .joinedAt(Instant.now())
                .build();

        when(qaJobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(teamMemberRepository.findByTeamIdAndUserId(teamId, userId)).thenReturn(Optional.of(ownerMember));

        qaJobService.deleteJob(jobId);

        verify(qaJobRepository).delete(job);
    }

    @Test
    void deleteJob_asAdmin_success() {
        TeamMember adminMember = TeamMember.builder()
                .team(team)
                .user(user)
                .role(TeamRole.ADMIN)
                .joinedAt(Instant.now())
                .build();

        when(qaJobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(teamMemberRepository.findByTeamIdAndUserId(teamId, userId)).thenReturn(Optional.of(adminMember));

        qaJobService.deleteJob(jobId);

        verify(qaJobRepository).delete(job);
    }

    @Test
    void deleteJob_asMember_throws() {
        TeamMember member = TeamMember.builder()
                .team(team)
                .user(user)
                .role(TeamRole.MEMBER)
                .joinedAt(Instant.now())
                .build();

        when(qaJobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(teamMemberRepository.findByTeamIdAndUserId(teamId, userId)).thenReturn(Optional.of(member));

        assertThrows(AccessDeniedException.class, () -> qaJobService.deleteJob(jobId));
        verify(qaJobRepository, never()).delete(any());
    }

    @Test
    void deleteJob_asViewer_throws() {
        TeamMember viewer = TeamMember.builder()
                .team(team)
                .user(user)
                .role(TeamRole.VIEWER)
                .joinedAt(Instant.now())
                .build();

        when(qaJobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(teamMemberRepository.findByTeamIdAndUserId(teamId, userId)).thenReturn(Optional.of(viewer));

        assertThrows(AccessDeniedException.class, () -> qaJobService.deleteJob(jobId));
        verify(qaJobRepository, never()).delete(any());
    }

    @Test
    void deleteJob_notTeamMember_throws() {
        when(qaJobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(teamMemberRepository.findByTeamIdAndUserId(teamId, userId)).thenReturn(Optional.empty());

        assertThrows(AccessDeniedException.class, () -> qaJobService.deleteJob(jobId));
        verify(qaJobRepository, never()).delete(any());
    }

    @Test
    void deleteJob_notFound_throws() {
        when(qaJobRepository.findById(jobId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> qaJobService.deleteJob(jobId));
    }

    // --- mapToJobResponse fields ---

    @Test
    void mapToJobResponse_allFieldsMapped() {
        job.setSummaryMd("summary");
        job.setOverallResult(JobResult.WARN);
        job.setHealthScore(75);
        job.setTotalFindings(20);
        job.setCriticalCount(2);
        job.setHighCount(5);
        job.setMediumCount(8);
        job.setLowCount(5);
        job.setStartedAt(Instant.now().minusSeconds(60));
        job.setCompletedAt(Instant.now());

        when(qaJobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(true);

        JobResponse response = qaJobService.getJob(jobId);

        assertEquals(jobId, response.id());
        assertEquals(projectId, response.projectId());
        assertEquals("Test Project", response.projectName());
        assertEquals(JobMode.AUDIT, response.mode());
        assertEquals(JobStatus.PENDING, response.status());
        assertEquals("Full Audit", response.name());
        assertEquals("main", response.branch());
        assertEquals("{\"agents\":[\"SECURITY\"]}", response.configJson());
        assertEquals("summary", response.summaryMd());
        assertEquals(JobResult.WARN, response.overallResult());
        assertEquals(75, response.healthScore());
        assertEquals(20, response.totalFindings());
        assertEquals(2, response.criticalCount());
        assertEquals(5, response.highCount());
        assertEquals(8, response.mediumCount());
        assertEquals(5, response.lowCount());
        assertEquals("PROJ-123", response.jiraTicketKey());
        assertEquals(userId, response.startedBy());
        assertEquals("Test User", response.startedByName());
        assertNotNull(response.startedAt());
        assertNotNull(response.completedAt());
        assertNotNull(response.createdAt());
    }

    // --- mapToJobSummaryResponse fields ---

    @Test
    void mapToJobSummaryResponse_allFieldsMapped() {
        job.setOverallResult(JobResult.PASS);
        job.setHealthScore(90);
        job.setTotalFindings(5);
        job.setCriticalCount(0);
        job.setCompletedAt(Instant.now());

        Pageable pageable = PageRequest.of(0, 20);
        Page<QaJob> page = new PageImpl<>(List.of(job), pageable, 1);

        when(qaJobRepository.findByStartedById(userId, pageable)).thenReturn(page);

        PageResponse<JobSummaryResponse> response = qaJobService.getJobsByUser(userId, pageable);

        JobSummaryResponse summary = response.content().get(0);
        assertEquals(jobId, summary.id());
        assertEquals("Test Project", summary.projectName());
        assertEquals(JobMode.AUDIT, summary.mode());
        assertEquals(JobStatus.PENDING, summary.status());
        assertEquals("Full Audit", summary.name());
        assertEquals(JobResult.PASS, summary.overallResult());
        assertEquals(90, summary.healthScore());
        assertEquals(5, summary.totalFindings());
        assertEquals(0, summary.criticalCount());
        assertNotNull(summary.completedAt());
        assertNotNull(summary.createdAt());
    }

    private void setSecurityContext(UUID userId) {
        var auth = new UsernamePasswordAuthenticationToken(userId, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
