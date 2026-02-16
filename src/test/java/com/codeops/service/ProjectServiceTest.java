package com.codeops.service;

import com.codeops.config.AppConstants;
import com.codeops.dto.request.CreateProjectRequest;
import com.codeops.dto.request.UpdateProjectRequest;
import com.codeops.dto.response.PageResponse;
import com.codeops.dto.response.ProjectResponse;
import com.codeops.entity.*;
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

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock private ProjectRepository projectRepository;
    @Mock private TeamMemberRepository teamMemberRepository;
    @Mock private UserRepository userRepository;
    @Mock private TeamRepository teamRepository;
    @Mock private GitHubConnectionRepository gitHubConnectionRepository;
    @Mock private JiraConnectionRepository jiraConnectionRepository;
    @Mock private ObjectMapper objectMapper;
    @Mock private RemediationTaskRepository remediationTaskRepository;
    @Mock private ComplianceItemRepository complianceItemRepository;
    @Mock private SpecificationRepository specificationRepository;
    @Mock private FindingRepository findingRepository;
    @Mock private AgentRunRepository agentRunRepository;
    @Mock private BugInvestigationRepository bugInvestigationRepository;
    @Mock private TechDebtItemRepository techDebtItemRepository;
    @Mock private DependencyVulnerabilityRepository dependencyVulnerabilityRepository;
    @Mock private DependencyScanRepository dependencyScanRepository;
    @Mock private HealthSnapshotRepository healthSnapshotRepository;
    @Mock private QaJobRepository qaJobRepository;
    @Mock private HealthScheduleRepository healthScheduleRepository;
    @Mock private ProjectDirectiveRepository projectDirectiveRepository;
    @Mock private DirectiveRepository directiveRepository;

    @InjectMocks
    private ProjectService projectService;

    private UUID userId;
    private UUID teamId;
    private UUID projectId;
    private User testUser;
    private Team testTeam;
    private Project testProject;
    private TeamMember adminMember;
    private TeamMember ownerMember;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        teamId = UUID.randomUUID();
        projectId = UUID.randomUUID();

        testUser = User.builder()
                .email("test@codeops.dev")
                .passwordHash("hash")
                .displayName("Test User")
                .isActive(true)
                .build();
        testUser.setId(userId);
        testUser.setCreatedAt(Instant.now());

        testTeam = Team.builder()
                .name("Test Team")
                .description("A test team")
                .owner(testUser)
                .build();
        testTeam.setId(teamId);
        testTeam.setCreatedAt(Instant.now());

        testProject = Project.builder()
                .team(testTeam)
                .name("Test Project")
                .description("A test project")
                .repoUrl("https://github.com/test/repo")
                .repoFullName("test/repo")
                .defaultBranch("main")
                .techStack("Java, Spring Boot")
                .healthScore(AppConstants.DEFAULT_HEALTH_SCORE)
                .isArchived(false)
                .createdBy(testUser)
                .build();
        testProject.setId(projectId);
        testProject.setCreatedAt(Instant.now());
        testProject.setUpdatedAt(Instant.now());

        adminMember = TeamMember.builder()
                .team(testTeam)
                .user(testUser)
                .role(TeamRole.ADMIN)
                .joinedAt(Instant.now())
                .build();
        adminMember.setId(UUID.randomUUID());

        ownerMember = TeamMember.builder()
                .team(testTeam)
                .user(testUser)
                .role(TeamRole.OWNER)
                .joinedAt(Instant.now())
                .build();
        ownerMember.setId(UUID.randomUUID());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // --- createProject ---

    @Test
    void createProject_success() throws JsonProcessingException {
        setSecurityContext(userId);
        CreateProjectRequest request = new CreateProjectRequest(
                "New Project", "Description", null, "https://github.com/o/r",
                "o/r", "main", null, "PROJ", "Task",
                List.of("label1"), "backend", "Java"
        );

        when(teamMemberRepository.findByTeamIdAndUserId(teamId, userId))
                .thenReturn(Optional.of(adminMember));
        when(projectRepository.countByTeamId(teamId)).thenReturn(0L);
        when(teamRepository.findById(teamId)).thenReturn(Optional.of(testTeam));
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(objectMapper.writeValueAsString(List.of("label1"))).thenReturn("[\"label1\"]");
        when(projectRepository.save(any(Project.class))).thenAnswer(invocation -> {
            Project saved = invocation.getArgument(0);
            saved.setId(projectId);
            saved.setCreatedAt(Instant.now());
            saved.setUpdatedAt(Instant.now());
            return saved;
        });

        ProjectResponse response = projectService.createProject(teamId, request);

        assertNotNull(response);
        assertEquals("New Project", response.name());
        assertEquals("Description", response.description());
        assertEquals(teamId, response.teamId());
        verify(projectRepository).save(any(Project.class));
    }

    @Test
    void createProject_maxLimitReached_throws() {
        setSecurityContext(userId);
        CreateProjectRequest request = new CreateProjectRequest(
                "New Project", "Desc", null, null, null,
                null, null, null, null, null, null, null
        );

        when(teamMemberRepository.findByTeamIdAndUserId(teamId, userId))
                .thenReturn(Optional.of(adminMember));
        when(projectRepository.countByTeamId(teamId))
                .thenReturn((long) AppConstants.MAX_PROJECTS_PER_TEAM);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> projectService.createProject(teamId, request));
        assertEquals("Team has reached the maximum number of projects", ex.getMessage());
        verify(projectRepository, never()).save(any());
    }

    @Test
    void createProject_notTeamAdmin_throws() {
        setSecurityContext(userId);
        TeamMember memberRole = TeamMember.builder()
                .team(testTeam).user(testUser).role(TeamRole.MEMBER).joinedAt(Instant.now()).build();
        CreateProjectRequest request = new CreateProjectRequest(
                "New Project", "Desc", null, null, null,
                null, null, null, null, null, null, null
        );

        when(teamMemberRepository.findByTeamIdAndUserId(teamId, userId))
                .thenReturn(Optional.of(memberRole));

        assertThrows(AccessDeniedException.class,
                () -> projectService.createProject(teamId, request));
        verify(projectRepository, never()).save(any());
    }

    @Test
    void createProject_notTeamMember_throws() {
        setSecurityContext(userId);
        CreateProjectRequest request = new CreateProjectRequest(
                "New Project", "Desc", null, null, null,
                null, null, null, null, null, null, null
        );

        when(teamMemberRepository.findByTeamIdAndUserId(teamId, userId))
                .thenReturn(Optional.empty());

        assertThrows(AccessDeniedException.class,
                () -> projectService.createProject(teamId, request));
    }

    @Test
    void createProject_withGithubAndJiraConnections() throws JsonProcessingException {
        setSecurityContext(userId);
        UUID ghConnId = UUID.randomUUID();
        UUID jiraConnId = UUID.randomUUID();

        GitHubConnection ghConn = GitHubConnection.builder().name("gh").build();
        ghConn.setId(ghConnId);
        JiraConnection jiraConn = JiraConnection.builder().name("jira").instanceUrl("https://jira.test").email("j@t.com").encryptedApiToken("enc").build();
        jiraConn.setId(jiraConnId);

        CreateProjectRequest request = new CreateProjectRequest(
                "New Project", "Desc", ghConnId, "https://github.com/o/r",
                "o/r", "develop", jiraConnId, "PROJ", "Story",
                null, "frontend", "React"
        );

        when(teamMemberRepository.findByTeamIdAndUserId(teamId, userId))
                .thenReturn(Optional.of(adminMember));
        when(projectRepository.countByTeamId(teamId)).thenReturn(0L);
        when(teamRepository.findById(teamId)).thenReturn(Optional.of(testTeam));
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(gitHubConnectionRepository.findById(ghConnId)).thenReturn(Optional.of(ghConn));
        when(jiraConnectionRepository.findById(jiraConnId)).thenReturn(Optional.of(jiraConn));
        when(projectRepository.save(any(Project.class))).thenAnswer(invocation -> {
            Project saved = invocation.getArgument(0);
            saved.setId(projectId);
            saved.setCreatedAt(Instant.now());
            saved.setUpdatedAt(Instant.now());
            return saved;
        });

        ProjectResponse response = projectService.createProject(teamId, request);

        assertNotNull(response);
        assertEquals(ghConnId, response.githubConnectionId());
        assertEquals(jiraConnId, response.jiraConnectionId());
        verify(gitHubConnectionRepository).findById(ghConnId);
        verify(jiraConnectionRepository).findById(jiraConnId);
    }

    @Test
    void createProject_defaultBranchAndIssueType_whenNull() throws JsonProcessingException {
        setSecurityContext(userId);
        CreateProjectRequest request = new CreateProjectRequest(
                "New Project", "Desc", null, null, null,
                null, null, null, null, null, null, null
        );

        when(teamMemberRepository.findByTeamIdAndUserId(teamId, userId))
                .thenReturn(Optional.of(adminMember));
        when(projectRepository.countByTeamId(teamId)).thenReturn(0L);
        when(teamRepository.findById(teamId)).thenReturn(Optional.of(testTeam));
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(projectRepository.save(any(Project.class))).thenAnswer(invocation -> {
            Project saved = invocation.getArgument(0);
            saved.setId(projectId);
            saved.setCreatedAt(Instant.now());
            saved.setUpdatedAt(Instant.now());
            return saved;
        });

        ProjectResponse response = projectService.createProject(teamId, request);

        assertNotNull(response);
        assertEquals("main", response.defaultBranch());
        assertEquals("Task", response.jiraDefaultIssueType());
    }

    // --- getProject ---

    @Test
    void getProject_success() {
        setSecurityContext(userId);
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(testProject));
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(true);

        ProjectResponse response = projectService.getProject(projectId);

        assertNotNull(response);
        assertEquals(projectId, response.id());
        assertEquals("Test Project", response.name());
        assertEquals(teamId, response.teamId());
    }

    @Test
    void getProject_notFound_throws() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> projectService.getProject(projectId));
    }

    @Test
    void getProject_notTeamMember_throws() {
        setSecurityContext(userId);
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(testProject));
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(false);

        assertThrows(AccessDeniedException.class,
                () -> projectService.getProject(projectId));
    }

    // --- getProjectsForTeam ---

    @Test
    void getProjectsForTeam_success() {
        setSecurityContext(userId);
        Project project2 = Project.builder()
                .team(testTeam).name("Project 2").description("Second")
                .defaultBranch("main").healthScore(80).isArchived(false).createdBy(testUser).build();
        project2.setId(UUID.randomUUID());
        project2.setCreatedAt(Instant.now());
        project2.setUpdatedAt(Instant.now());

        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(true);
        when(projectRepository.findByTeamIdAndIsArchivedFalse(teamId))
                .thenReturn(List.of(testProject, project2));

        List<ProjectResponse> responses = projectService.getProjectsForTeam(teamId);

        assertEquals(2, responses.size());
        assertEquals("Test Project", responses.get(0).name());
        assertEquals("Project 2", responses.get(1).name());
    }

    @Test
    void getProjectsForTeam_notMember_throws() {
        setSecurityContext(userId);
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(false);

        assertThrows(AccessDeniedException.class,
                () -> projectService.getProjectsForTeam(teamId));
    }

    // --- getAllProjectsForTeam (paginated) ---

    @Test
    void getAllProjectsForTeam_excludeArchived() {
        setSecurityContext(userId);
        Pageable pageable = PageRequest.of(0, 20);
        Page<Project> page = new PageImpl<>(List.of(testProject), pageable, 1);

        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(true);
        when(projectRepository.findByTeamIdAndIsArchivedFalse(teamId, pageable)).thenReturn(page);

        PageResponse<ProjectResponse> response = projectService.getAllProjectsForTeam(teamId, false, pageable);

        assertEquals(1, response.content().size());
        assertEquals(1, response.totalElements());
        assertTrue(response.isLast());
        verify(projectRepository).findByTeamIdAndIsArchivedFalse(teamId, pageable);
        verify(projectRepository, never()).findByTeamId(eq(teamId), any(Pageable.class));
    }

    @Test
    void getAllProjectsForTeam_includeArchived() {
        setSecurityContext(userId);
        Pageable pageable = PageRequest.of(0, 20);
        Page<Project> page = new PageImpl<>(List.of(testProject), pageable, 1);

        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(true);
        when(projectRepository.findByTeamId(teamId, pageable)).thenReturn(page);

        PageResponse<ProjectResponse> response = projectService.getAllProjectsForTeam(teamId, true, pageable);

        assertEquals(1, response.content().size());
        verify(projectRepository).findByTeamId(teamId, pageable);
        verify(projectRepository, never()).findByTeamIdAndIsArchivedFalse(eq(teamId), any(Pageable.class));
    }

    // --- updateProject ---

    @Test
    void updateProject_success() {
        setSecurityContext(userId);
        UpdateProjectRequest request = new UpdateProjectRequest(
                "Updated Name", "Updated Desc", null, null, null,
                null, null, null, null, null, null, "Python", null
        );

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(testProject));
        when(teamMemberRepository.findByTeamIdAndUserId(teamId, userId))
                .thenReturn(Optional.of(adminMember));
        when(projectRepository.save(any(Project.class))).thenReturn(testProject);

        ProjectResponse response = projectService.updateProject(projectId, request);

        assertEquals("Updated Name", testProject.getName());
        assertEquals("Updated Desc", testProject.getDescription());
        assertEquals("Python", testProject.getTechStack());
        verify(projectRepository).save(testProject);
    }

    @Test
    void updateProject_notFound_throws() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.empty());

        UpdateProjectRequest request = new UpdateProjectRequest(
                "Name", null, null, null, null, null,
                null, null, null, null, null, null, null
        );

        assertThrows(EntityNotFoundException.class,
                () -> projectService.updateProject(projectId, request));
    }

    @Test
    void updateProject_notAdmin_throws() {
        setSecurityContext(userId);
        TeamMember memberRole = TeamMember.builder()
                .team(testTeam).user(testUser).role(TeamRole.MEMBER).joinedAt(Instant.now()).build();

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(testProject));
        when(teamMemberRepository.findByTeamIdAndUserId(teamId, userId))
                .thenReturn(Optional.of(memberRole));

        UpdateProjectRequest request = new UpdateProjectRequest(
                "Name", null, null, null, null, null,
                null, null, null, null, null, null, null
        );

        assertThrows(AccessDeniedException.class,
                () -> projectService.updateProject(projectId, request));
        verify(projectRepository, never()).save(any());
    }

    @Test
    void updateProject_nullFieldsNotOverwritten() {
        setSecurityContext(userId);
        testProject.setName("Original Name");
        testProject.setDescription("Original Description");
        testProject.setTechStack("Java");

        UpdateProjectRequest request = new UpdateProjectRequest(
                null, null, null, null, null, null,
                null, null, null, null, null, null, null
        );

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(testProject));
        when(teamMemberRepository.findByTeamIdAndUserId(teamId, userId))
                .thenReturn(Optional.of(adminMember));
        when(projectRepository.save(any(Project.class))).thenReturn(testProject);

        projectService.updateProject(projectId, request);

        assertEquals("Original Name", testProject.getName());
        assertEquals("Original Description", testProject.getDescription());
        assertEquals("Java", testProject.getTechStack());
    }

    @Test
    void updateProject_withGithubAndJiraConnections() {
        setSecurityContext(userId);
        UUID ghConnId = UUID.randomUUID();
        UUID jiraConnId = UUID.randomUUID();
        GitHubConnection ghConn = GitHubConnection.builder().name("gh").build();
        ghConn.setId(ghConnId);
        JiraConnection jiraConn = JiraConnection.builder().name("jira").instanceUrl("https://jira.test").email("j@t.com").encryptedApiToken("enc").build();
        jiraConn.setId(jiraConnId);

        UpdateProjectRequest request = new UpdateProjectRequest(
                null, null, ghConnId, null, null, null,
                jiraConnId, null, null, null, null, null, null
        );

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(testProject));
        when(teamMemberRepository.findByTeamIdAndUserId(teamId, userId))
                .thenReturn(Optional.of(adminMember));
        when(gitHubConnectionRepository.findById(ghConnId)).thenReturn(Optional.of(ghConn));
        when(jiraConnectionRepository.findById(jiraConnId)).thenReturn(Optional.of(jiraConn));
        when(projectRepository.save(any(Project.class))).thenReturn(testProject);

        projectService.updateProject(projectId, request);

        verify(gitHubConnectionRepository).findById(ghConnId);
        verify(jiraConnectionRepository).findById(jiraConnId);
    }

    @Test
    void updateProject_isArchivedField() {
        setSecurityContext(userId);
        UpdateProjectRequest request = new UpdateProjectRequest(
                null, null, null, null, null, null,
                null, null, null, null, null, null, true
        );

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(testProject));
        when(teamMemberRepository.findByTeamIdAndUserId(teamId, userId))
                .thenReturn(Optional.of(adminMember));
        when(projectRepository.save(any(Project.class))).thenReturn(testProject);

        projectService.updateProject(projectId, request);

        assertTrue(testProject.getIsArchived());
    }

    // --- archiveProject ---

    @Test
    void archiveProject_success() {
        setSecurityContext(userId);
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(testProject));
        when(teamMemberRepository.findByTeamIdAndUserId(teamId, userId))
                .thenReturn(Optional.of(adminMember));

        projectService.archiveProject(projectId);

        assertTrue(testProject.getIsArchived());
        verify(projectRepository).save(testProject);
    }

    @Test
    void archiveProject_notFound_throws() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> projectService.archiveProject(projectId));
    }

    @Test
    void archiveProject_notAdmin_throws() {
        setSecurityContext(userId);
        TeamMember memberRole = TeamMember.builder()
                .team(testTeam).user(testUser).role(TeamRole.VIEWER).joinedAt(Instant.now()).build();

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(testProject));
        when(teamMemberRepository.findByTeamIdAndUserId(teamId, userId))
                .thenReturn(Optional.of(memberRole));

        assertThrows(AccessDeniedException.class,
                () -> projectService.archiveProject(projectId));
        verify(projectRepository, never()).save(any());
    }

    // --- unarchiveProject ---

    @Test
    void unarchiveProject_success() {
        setSecurityContext(userId);
        testProject.setIsArchived(true);

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(testProject));
        when(teamMemberRepository.findByTeamIdAndUserId(teamId, userId))
                .thenReturn(Optional.of(adminMember));

        projectService.unarchiveProject(projectId);

        assertFalse(testProject.getIsArchived());
        verify(projectRepository).save(testProject);
    }

    @Test
    void unarchiveProject_notFound_throws() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> projectService.unarchiveProject(projectId));
    }

    @Test
    void unarchiveProject_notAdmin_throws() {
        setSecurityContext(userId);
        TeamMember memberRole = TeamMember.builder()
                .team(testTeam).user(testUser).role(TeamRole.MEMBER).joinedAt(Instant.now()).build();

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(testProject));
        when(teamMemberRepository.findByTeamIdAndUserId(teamId, userId))
                .thenReturn(Optional.of(memberRole));

        assertThrows(AccessDeniedException.class,
                () -> projectService.unarchiveProject(projectId));
        verify(projectRepository, never()).save(any());
    }

    // --- deleteProject ---

    @Test
    void deleteProject_ownerOnly_success() {
        setSecurityContext(userId);
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(testProject));
        when(teamMemberRepository.findByTeamIdAndUserId(teamId, userId))
                .thenReturn(Optional.of(ownerMember));

        projectService.deleteProject(projectId);

        // Verify cascade cleanup in FK-safe order
        var inOrder = inOrder(
                remediationTaskRepository, complianceItemRepository,
                specificationRepository, findingRepository,
                agentRunRepository, bugInvestigationRepository,
                techDebtItemRepository, dependencyVulnerabilityRepository,
                dependencyScanRepository, healthSnapshotRepository,
                qaJobRepository, healthScheduleRepository,
                projectDirectiveRepository, directiveRepository,
                projectRepository
        );
        inOrder.verify(remediationTaskRepository).deleteJoinTableByProjectId(projectId);
        inOrder.verify(remediationTaskRepository).deleteAllByProjectId(projectId);
        inOrder.verify(complianceItemRepository).deleteAllByProjectId(projectId);
        inOrder.verify(specificationRepository).deleteAllByProjectId(projectId);
        inOrder.verify(findingRepository).deleteAllByProjectId(projectId);
        inOrder.verify(agentRunRepository).deleteAllByProjectId(projectId);
        inOrder.verify(bugInvestigationRepository).deleteAllByProjectId(projectId);
        inOrder.verify(techDebtItemRepository).deleteAllByProjectId(projectId);
        inOrder.verify(dependencyVulnerabilityRepository).deleteAllByProjectId(projectId);
        inOrder.verify(dependencyScanRepository).deleteAllByProjectId(projectId);
        inOrder.verify(healthSnapshotRepository).deleteAllByProjectId(projectId);
        inOrder.verify(qaJobRepository).deleteAllByProjectId(projectId);
        inOrder.verify(healthScheduleRepository).deleteAllByProjectId(projectId);
        inOrder.verify(projectDirectiveRepository).deleteAllByProjectId(projectId);
        inOrder.verify(directiveRepository).deleteAllByProjectId(projectId);
        inOrder.verify(projectRepository).delete(testProject);
    }

    @Test
    void deleteProject_adminRole_throws() {
        setSecurityContext(userId);
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(testProject));
        when(teamMemberRepository.findByTeamIdAndUserId(teamId, userId))
                .thenReturn(Optional.of(adminMember));

        assertThrows(AccessDeniedException.class,
                () -> projectService.deleteProject(projectId));
        verify(projectRepository, never()).delete(any());
    }

    @Test
    void deleteProject_memberRole_throws() {
        setSecurityContext(userId);
        TeamMember memberRole = TeamMember.builder()
                .team(testTeam).user(testUser).role(TeamRole.MEMBER).joinedAt(Instant.now()).build();

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(testProject));
        when(teamMemberRepository.findByTeamIdAndUserId(teamId, userId))
                .thenReturn(Optional.of(memberRole));

        assertThrows(AccessDeniedException.class,
                () -> projectService.deleteProject(projectId));
        verify(projectRepository, never()).delete(any());
    }

    @Test
    void deleteProject_notFound_throws() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> projectService.deleteProject(projectId));
    }

    @Test
    void deleteProject_notTeamMember_throws() {
        setSecurityContext(userId);
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(testProject));
        when(teamMemberRepository.findByTeamIdAndUserId(teamId, userId))
                .thenReturn(Optional.empty());

        assertThrows(AccessDeniedException.class,
                () -> projectService.deleteProject(projectId));
        verify(projectRepository, never()).delete(any());
    }

    // --- updateHealthScore ---

    @Test
    void updateHealthScore_success() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(testProject));

        projectService.updateHealthScore(projectId, 75);

        assertEquals(75, testProject.getHealthScore());
        assertNotNull(testProject.getLastAuditAt());
        verify(projectRepository).save(testProject);
    }

    @Test
    void updateHealthScore_notFound_throws() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> projectService.updateHealthScore(projectId, 50));
    }

    @Test
    void updateHealthScore_setsLastAuditAt() {
        Instant before = Instant.now();
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(testProject));

        projectService.updateHealthScore(projectId, 90);

        assertNotNull(testProject.getLastAuditAt());
        assertTrue(testProject.getLastAuditAt().compareTo(before) >= 0);
    }

    // --- ownerCanAlsoPerformAdminActions ---

    @Test
    void createProject_ownerRole_success() throws JsonProcessingException {
        setSecurityContext(userId);
        CreateProjectRequest request = new CreateProjectRequest(
                "Owner Project", "Desc", null, null, null,
                null, null, null, null, null, null, null
        );

        when(teamMemberRepository.findByTeamIdAndUserId(teamId, userId))
                .thenReturn(Optional.of(ownerMember));
        when(projectRepository.countByTeamId(teamId)).thenReturn(0L);
        when(teamRepository.findById(teamId)).thenReturn(Optional.of(testTeam));
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(projectRepository.save(any(Project.class))).thenAnswer(invocation -> {
            Project saved = invocation.getArgument(0);
            saved.setId(projectId);
            saved.setCreatedAt(Instant.now());
            saved.setUpdatedAt(Instant.now());
            return saved;
        });

        ProjectResponse response = projectService.createProject(teamId, request);

        assertNotNull(response);
        assertEquals("Owner Project", response.name());
    }

    // --- helper methods ---

    private void setSecurityContext(UUID userId) {
        var auth = new UsernamePasswordAuthenticationToken(userId, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
