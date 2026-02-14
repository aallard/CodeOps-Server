package com.codeops.service;

import com.codeops.dto.request.CreateHealthScheduleRequest;
import com.codeops.dto.request.CreateHealthSnapshotRequest;
import com.codeops.dto.response.HealthScheduleResponse;
import com.codeops.dto.response.HealthSnapshotResponse;
import com.codeops.dto.response.PageResponse;
import com.codeops.entity.*;
import com.codeops.entity.enums.AgentType;
import com.codeops.entity.enums.ScheduleType;
import com.codeops.entity.enums.TeamRole;
import com.codeops.repository.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HealthMonitorServiceTest {

    @Mock private HealthScheduleRepository healthScheduleRepository;
    @Mock private HealthSnapshotRepository healthSnapshotRepository;
    @Mock private ProjectRepository projectRepository;
    @Mock private TeamMemberRepository teamMemberRepository;
    @Mock private UserRepository userRepository;
    @Mock private QaJobRepository qaJobRepository;
    @Mock private ObjectMapper objectMapper;

    @InjectMocks
    private HealthMonitorService healthMonitorService;

    private UUID userId;
    private UUID teamId;
    private UUID projectId;
    private UUID scheduleId;
    private UUID snapshotId;
    private UUID jobId;
    private Team team;
    private Project project;
    private User user;
    private QaJob qaJob;
    private HealthSchedule schedule;
    private HealthSnapshot snapshot;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        teamId = UUID.randomUUID();
        projectId = UUID.randomUUID();
        scheduleId = UUID.randomUUID();
        snapshotId = UUID.randomUUID();
        jobId = UUID.randomUUID();

        team = Team.builder().name("Test Team").build();
        team.setId(teamId);

        project = Project.builder().team(team).name("Test Project").build();
        project.setId(projectId);

        user = User.builder().email("test@codeops.dev").displayName("Test User").build();
        user.setId(userId);

        qaJob = QaJob.builder().project(project).build();
        qaJob.setId(jobId);

        schedule = HealthSchedule.builder()
                .project(project)
                .scheduleType(ScheduleType.DAILY)
                .cronExpression("0 0 * * *")
                .agentTypes("[\"SECURITY\",\"CODE_QUALITY\"]")
                .isActive(true)
                .createdBy(user)
                .nextRunAt(Instant.now().plusSeconds(86400))
                .build();
        schedule.setId(scheduleId);
        schedule.setCreatedAt(Instant.now());

        snapshot = HealthSnapshot.builder()
                .project(project)
                .healthScore(85)
                .findingsBySeverity("{\"CRITICAL\":0,\"HIGH\":2}")
                .techDebtScore(70)
                .dependencyScore(90)
                .testCoveragePercent(new BigDecimal("78.50"))
                .capturedAt(Instant.now())
                .build();
        snapshot.setId(snapshotId);

        setSecurityContext(userId);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // --- createSchedule ---

    @Test
    void createSchedule_daily_success() throws JsonProcessingException {
        CreateHealthScheduleRequest request = new CreateHealthScheduleRequest(
                projectId, ScheduleType.DAILY, "0 0 * * *", List.of(AgentType.SECURITY, AgentType.CODE_QUALITY));

        TeamMember adminMember = TeamMember.builder().role(TeamRole.ADMIN).build();

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(teamMemberRepository.findByTeamIdAndUserId(teamId, userId)).thenReturn(Optional.of(adminMember));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(objectMapper.writeValueAsString(anyList())).thenReturn("[\"SECURITY\",\"CODE_QUALITY\"]");
        when(objectMapper.readValue(anyString(), any(com.fasterxml.jackson.core.type.TypeReference.class)))
                .thenReturn(List.of(AgentType.SECURITY, AgentType.CODE_QUALITY));
        when(healthScheduleRepository.save(any(HealthSchedule.class))).thenAnswer(inv -> {
            HealthSchedule s = inv.getArgument(0);
            s.setId(scheduleId);
            s.setCreatedAt(Instant.now());
            return s;
        });

        HealthScheduleResponse response = healthMonitorService.createSchedule(request);

        assertNotNull(response);
        assertEquals(ScheduleType.DAILY, response.scheduleType());
        assertTrue(response.isActive());
    }

    @Test
    void createSchedule_notAdmin_throws() {
        CreateHealthScheduleRequest request = new CreateHealthScheduleRequest(
                projectId, ScheduleType.DAILY, null, List.of(AgentType.SECURITY));

        TeamMember memberRole = TeamMember.builder().role(TeamRole.MEMBER).build();

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(teamMemberRepository.findByTeamIdAndUserId(teamId, userId)).thenReturn(Optional.of(memberRole));

        assertThrows(AccessDeniedException.class, () -> healthMonitorService.createSchedule(request));
    }

    @Test
    void createSchedule_projectNotFound_throws() {
        CreateHealthScheduleRequest request = new CreateHealthScheduleRequest(
                projectId, ScheduleType.DAILY, null, List.of(AgentType.SECURITY));

        when(projectRepository.findById(projectId)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> healthMonitorService.createSchedule(request));
    }

    // --- getSchedulesForProject ---

    @Test
    void getSchedulesForProject_success() throws JsonProcessingException {
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(true);
        when(healthScheduleRepository.findByProjectId(projectId)).thenReturn(List.of(schedule));
        when(objectMapper.readValue(anyString(), any(com.fasterxml.jackson.core.type.TypeReference.class)))
                .thenReturn(List.of(AgentType.SECURITY, AgentType.CODE_QUALITY));

        List<HealthScheduleResponse> responses = healthMonitorService.getSchedulesForProject(projectId);

        assertEquals(1, responses.size());
    }

    @Test
    void getSchedulesForProject_projectNotFound_throws() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> healthMonitorService.getSchedulesForProject(projectId));
    }

    // --- getActiveSchedules ---

    @Test
    void getActiveSchedules_filtersToUserTeams() throws JsonProcessingException {
        TeamMember membership = TeamMember.builder().role(TeamRole.MEMBER).build();
        membership.setId(UUID.randomUUID());
        Team memberTeam = Team.builder().name("My Team").build();
        memberTeam.setId(teamId);
        membership.setTeam(memberTeam);

        when(teamMemberRepository.findByUserId(userId)).thenReturn(List.of(membership));
        when(healthScheduleRepository.findByIsActiveTrue()).thenReturn(List.of(schedule));
        when(objectMapper.readValue(anyString(), any(com.fasterxml.jackson.core.type.TypeReference.class)))
                .thenReturn(List.of(AgentType.SECURITY));

        List<HealthScheduleResponse> responses = healthMonitorService.getActiveSchedules();

        assertEquals(1, responses.size());
    }

    @Test
    void getActiveSchedules_excludesOtherTeamSchedules() {
        UUID otherTeamId = UUID.randomUUID();
        TeamMember membership = TeamMember.builder().role(TeamRole.MEMBER).build();
        membership.setId(UUID.randomUUID());
        Team otherTeam = Team.builder().name("Other Team").build();
        otherTeam.setId(otherTeamId);
        membership.setTeam(otherTeam);

        when(teamMemberRepository.findByUserId(userId)).thenReturn(List.of(membership));
        when(healthScheduleRepository.findByIsActiveTrue()).thenReturn(List.of(schedule));

        List<HealthScheduleResponse> responses = healthMonitorService.getActiveSchedules();

        assertTrue(responses.isEmpty());
    }

    // --- updateSchedule ---

    @Test
    void updateSchedule_deactivate_success() throws JsonProcessingException {
        TeamMember adminMember = TeamMember.builder().role(TeamRole.ADMIN).build();

        when(healthScheduleRepository.findById(scheduleId)).thenReturn(Optional.of(schedule));
        when(teamMemberRepository.findByTeamIdAndUserId(teamId, userId)).thenReturn(Optional.of(adminMember));
        when(healthScheduleRepository.save(any(HealthSchedule.class))).thenReturn(schedule);
        when(objectMapper.readValue(anyString(), any(com.fasterxml.jackson.core.type.TypeReference.class)))
                .thenReturn(List.of(AgentType.SECURITY));

        HealthScheduleResponse response = healthMonitorService.updateSchedule(scheduleId, false);

        assertFalse(schedule.getIsActive());
    }

    @Test
    void updateSchedule_activate_setsNextRun() throws JsonProcessingException {
        schedule.setIsActive(false);
        TeamMember ownerMember = TeamMember.builder().role(TeamRole.OWNER).build();

        when(healthScheduleRepository.findById(scheduleId)).thenReturn(Optional.of(schedule));
        when(teamMemberRepository.findByTeamIdAndUserId(teamId, userId)).thenReturn(Optional.of(ownerMember));
        when(healthScheduleRepository.save(any(HealthSchedule.class))).thenReturn(schedule);
        when(objectMapper.readValue(anyString(), any(com.fasterxml.jackson.core.type.TypeReference.class)))
                .thenReturn(List.of(AgentType.SECURITY));

        healthMonitorService.updateSchedule(scheduleId, true);

        assertTrue(schedule.getIsActive());
        assertNotNull(schedule.getNextRunAt());
    }

    @Test
    void updateSchedule_notFound_throws() {
        when(healthScheduleRepository.findById(scheduleId)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> healthMonitorService.updateSchedule(scheduleId, true));
    }

    @Test
    void updateSchedule_notAdmin_throws() {
        TeamMember memberRole = TeamMember.builder().role(TeamRole.MEMBER).build();

        when(healthScheduleRepository.findById(scheduleId)).thenReturn(Optional.of(schedule));
        when(teamMemberRepository.findByTeamIdAndUserId(teamId, userId)).thenReturn(Optional.of(memberRole));

        assertThrows(AccessDeniedException.class, () -> healthMonitorService.updateSchedule(scheduleId, false));
    }

    // --- deleteSchedule ---

    @Test
    void deleteSchedule_asAdmin_success() {
        TeamMember adminMember = TeamMember.builder().role(TeamRole.ADMIN).build();

        when(healthScheduleRepository.findById(scheduleId)).thenReturn(Optional.of(schedule));
        when(teamMemberRepository.findByTeamIdAndUserId(teamId, userId)).thenReturn(Optional.of(adminMember));

        healthMonitorService.deleteSchedule(scheduleId);

        verify(healthScheduleRepository).delete(schedule);
    }

    @Test
    void deleteSchedule_notFound_throws() {
        when(healthScheduleRepository.findById(scheduleId)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> healthMonitorService.deleteSchedule(scheduleId));
    }

    // --- markScheduleRun ---

    @Test
    void markScheduleRun_success() {
        when(healthScheduleRepository.findById(scheduleId)).thenReturn(Optional.of(schedule));

        healthMonitorService.markScheduleRun(scheduleId);

        assertNotNull(schedule.getLastRunAt());
        verify(healthScheduleRepository).save(schedule);
    }

    @Test
    void markScheduleRun_notFound_throws() {
        when(healthScheduleRepository.findById(scheduleId)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> healthMonitorService.markScheduleRun(scheduleId));
    }

    // --- createSnapshot ---

    @Test
    void createSnapshot_success() {
        CreateHealthSnapshotRequest request = new CreateHealthSnapshotRequest(
                projectId, null, 85, "{}", 70, 90, new BigDecimal("78.50"));

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(true);
        when(healthSnapshotRepository.save(any(HealthSnapshot.class))).thenAnswer(inv -> {
            HealthSnapshot s = inv.getArgument(0);
            s.setId(snapshotId);
            return s;
        });

        HealthSnapshotResponse response = healthMonitorService.createSnapshot(request);

        assertNotNull(response);
        assertEquals(85, response.healthScore());
    }

    @Test
    void createSnapshot_withJob_success() {
        CreateHealthSnapshotRequest request = new CreateHealthSnapshotRequest(
                projectId, jobId, 85, null, null, null, null);

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(true);
        when(qaJobRepository.findById(jobId)).thenReturn(Optional.of(qaJob));
        when(healthSnapshotRepository.save(any(HealthSnapshot.class))).thenAnswer(inv -> {
            HealthSnapshot s = inv.getArgument(0);
            s.setId(snapshotId);
            return s;
        });

        HealthSnapshotResponse response = healthMonitorService.createSnapshot(request);

        assertEquals(jobId, response.jobId());
    }

    @Test
    void createSnapshot_projectNotFound_throws() {
        CreateHealthSnapshotRequest request = new CreateHealthSnapshotRequest(
                projectId, null, 85, null, null, null, null);

        when(projectRepository.findById(projectId)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> healthMonitorService.createSnapshot(request));
    }

    // --- getSnapshots ---

    @Test
    void getSnapshots_success() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<HealthSnapshot> page = new PageImpl<>(List.of(snapshot), pageable, 1);

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(true);
        when(healthSnapshotRepository.findByProjectId(projectId, pageable)).thenReturn(page);

        PageResponse<HealthSnapshotResponse> result = healthMonitorService.getSnapshots(projectId, pageable);

        assertEquals(1, result.content().size());
    }

    // --- getLatestSnapshot ---

    @Test
    void getLatestSnapshot_success() {
        when(healthSnapshotRepository.findFirstByProjectIdOrderByCapturedAtDesc(projectId))
                .thenReturn(Optional.of(snapshot));

        HealthSnapshotResponse response = healthMonitorService.getLatestSnapshot(projectId);

        assertEquals(85, response.healthScore());
    }

    @Test
    void getLatestSnapshot_notFound_throws() {
        when(healthSnapshotRepository.findFirstByProjectIdOrderByCapturedAtDesc(projectId))
                .thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> healthMonitorService.getLatestSnapshot(projectId));
    }

    // --- getHealthTrend ---

    @Test
    void getHealthTrend_success() {
        HealthSnapshot s1 = HealthSnapshot.builder().project(project).healthScore(80)
                .capturedAt(Instant.now().minusSeconds(3600)).build();
        s1.setId(UUID.randomUUID());
        HealthSnapshot s2 = HealthSnapshot.builder().project(project).healthScore(85)
                .capturedAt(Instant.now()).build();
        s2.setId(UUID.randomUUID());

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(true);
        when(healthSnapshotRepository.findByProjectIdOrderByCapturedAtDesc(projectId))
                .thenReturn(List.of(s2, s1));

        List<HealthSnapshotResponse> result = healthMonitorService.getHealthTrend(projectId, 10);

        assertEquals(2, result.size());
        // Should be reversed (chronological order)
        assertEquals(80, result.get(0).healthScore());
        assertEquals(85, result.get(1).healthScore());
    }

    @Test
    void getHealthTrend_limitsResults() {
        HealthSnapshot s1 = HealthSnapshot.builder().project(project).healthScore(80)
                .capturedAt(Instant.now().minusSeconds(7200)).build();
        s1.setId(UUID.randomUUID());
        HealthSnapshot s2 = HealthSnapshot.builder().project(project).healthScore(85)
                .capturedAt(Instant.now().minusSeconds(3600)).build();
        s2.setId(UUID.randomUUID());
        HealthSnapshot s3 = HealthSnapshot.builder().project(project).healthScore(90)
                .capturedAt(Instant.now()).build();
        s3.setId(UUID.randomUUID());

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(true);
        when(healthSnapshotRepository.findByProjectIdOrderByCapturedAtDesc(projectId))
                .thenReturn(List.of(s3, s2, s1));

        List<HealthSnapshotResponse> result = healthMonitorService.getHealthTrend(projectId, 2);

        assertEquals(2, result.size());
    }

    private void setSecurityContext(UUID userId) {
        var auth = new UsernamePasswordAuthenticationToken(userId, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
