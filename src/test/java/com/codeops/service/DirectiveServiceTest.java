package com.codeops.service;

import com.codeops.config.AppConstants;
import com.codeops.dto.request.AssignDirectiveRequest;
import com.codeops.dto.request.CreateDirectiveRequest;
import com.codeops.dto.request.UpdateDirectiveRequest;
import com.codeops.dto.response.DirectiveResponse;
import com.codeops.dto.response.ProjectDirectiveResponse;
import com.codeops.entity.*;
import com.codeops.entity.enums.DirectiveCategory;
import com.codeops.entity.enums.DirectiveScope;
import com.codeops.entity.enums.TeamRole;
import com.codeops.repository.*;
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
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DirectiveServiceTest {

    @Mock private DirectiveRepository directiveRepository;
    @Mock private ProjectDirectiveRepository projectDirectiveRepository;
    @Mock private ProjectRepository projectRepository;
    @Mock private TeamMemberRepository teamMemberRepository;
    @Mock private UserRepository userRepository;
    @Mock private TeamRepository teamRepository;

    @InjectMocks
    private DirectiveService directiveService;

    private UUID userId;
    private UUID teamId;
    private UUID projectId;
    private UUID directiveId;
    private User testUser;
    private Team testTeam;
    private Project testProject;
    private Directive testDirective;
    private TeamMember adminMember;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        teamId = UUID.randomUUID();
        projectId = UUID.randomUUID();
        directiveId = UUID.randomUUID();

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
                .owner(testUser)
                .build();
        testTeam.setId(teamId);
        testTeam.setCreatedAt(Instant.now());

        testProject = Project.builder()
                .name("Test Project")
                .team(testTeam)
                .createdBy(testUser)
                .build();
        testProject.setId(projectId);
        testProject.setCreatedAt(Instant.now());

        testDirective = Directive.builder()
                .name("Test Directive")
                .description("Test description")
                .contentMd("# Test content")
                .category(DirectiveCategory.ARCHITECTURE)
                .scope(DirectiveScope.TEAM)
                .team(testTeam)
                .createdBy(testUser)
                .version(1)
                .build();
        testDirective.setId(directiveId);
        testDirective.setCreatedAt(Instant.now());
        testDirective.setUpdatedAt(Instant.now());

        adminMember = TeamMember.builder()
                .team(testTeam)
                .user(testUser)
                .role(TeamRole.ADMIN)
                .joinedAt(Instant.now())
                .build();
        adminMember.setId(UUID.randomUUID());

        setSecurityContext(userId);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ── createDirective ──────────────────────────────────────────────

    @Test
    void createDirective_teamScope_success() {
        var request = new CreateDirectiveRequest(
                "New Directive", "desc", "# content", DirectiveCategory.STANDARDS,
                DirectiveScope.TEAM, teamId, null);

        when(teamMemberRepository.findByTeamIdAndUserId(teamId, userId))
                .thenReturn(Optional.of(adminMember));
        when(teamRepository.findById(teamId)).thenReturn(Optional.of(testTeam));
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(directiveRepository.save(any(Directive.class))).thenAnswer(invocation -> {
            Directive d = invocation.getArgument(0);
            d.setId(UUID.randomUUID());
            d.setCreatedAt(Instant.now());
            d.setUpdatedAt(Instant.now());
            return d;
        });

        DirectiveResponse response = directiveService.createDirective(request);

        assertNotNull(response);
        assertEquals("New Directive", response.name());
        assertEquals(DirectiveScope.TEAM, response.scope());
        assertEquals(teamId, response.teamId());
        assertNull(response.projectId());
        assertEquals(1, response.version());
        verify(directiveRepository).save(any(Directive.class));
    }

    @Test
    void createDirective_projectScope_success() {
        var request = new CreateDirectiveRequest(
                "Project Directive", "desc", "# content", DirectiveCategory.CONTEXT,
                DirectiveScope.PROJECT, null, projectId);

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(testProject));
        when(teamMemberRepository.findByTeamIdAndUserId(teamId, userId))
                .thenReturn(Optional.of(adminMember));
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(directiveRepository.save(any(Directive.class))).thenAnswer(invocation -> {
            Directive d = invocation.getArgument(0);
            d.setId(UUID.randomUUID());
            d.setCreatedAt(Instant.now());
            d.setUpdatedAt(Instant.now());
            return d;
        });

        DirectiveResponse response = directiveService.createDirective(request);

        assertNotNull(response);
        assertEquals("Project Directive", response.name());
        assertEquals(DirectiveScope.PROJECT, response.scope());
        assertEquals(projectId, response.projectId());
        verify(directiveRepository).save(any(Directive.class));
    }

    @Test
    void createDirective_teamScope_missingTeamId_throws() {
        var request = new CreateDirectiveRequest(
                "Directive", "desc", "# content", DirectiveCategory.ARCHITECTURE,
                DirectiveScope.TEAM, null, null);

        assertThrows(IllegalArgumentException.class,
                () -> directiveService.createDirective(request));
        verify(directiveRepository, never()).save(any());
    }

    @Test
    void createDirective_projectScope_missingProjectId_throws() {
        var request = new CreateDirectiveRequest(
                "Directive", "desc", "# content", DirectiveCategory.ARCHITECTURE,
                DirectiveScope.PROJECT, null, null);

        assertThrows(IllegalArgumentException.class,
                () -> directiveService.createDirective(request));
        verify(directiveRepository, never()).save(any());
    }

    @Test
    void createDirective_teamScope_notAdmin_throws() {
        var request = new CreateDirectiveRequest(
                "Directive", "desc", "# content", DirectiveCategory.ARCHITECTURE,
                DirectiveScope.TEAM, teamId, null);

        TeamMember viewerMember = TeamMember.builder()
                .team(testTeam).user(testUser).role(TeamRole.VIEWER).joinedAt(Instant.now()).build();
        when(teamMemberRepository.findByTeamIdAndUserId(teamId, userId))
                .thenReturn(Optional.of(viewerMember));

        assertThrows(AccessDeniedException.class,
                () -> directiveService.createDirective(request));
        verify(directiveRepository, never()).save(any());
    }

    @Test
    void createDirective_teamScope_notMember_throws() {
        var request = new CreateDirectiveRequest(
                "Directive", "desc", "# content", DirectiveCategory.ARCHITECTURE,
                DirectiveScope.TEAM, teamId, null);

        when(teamMemberRepository.findByTeamIdAndUserId(teamId, userId))
                .thenReturn(Optional.empty());

        assertThrows(AccessDeniedException.class,
                () -> directiveService.createDirective(request));
    }

    @Test
    void createDirective_projectScope_projectNotFound_throws() {
        var request = new CreateDirectiveRequest(
                "Directive", "desc", "# content", DirectiveCategory.ARCHITECTURE,
                DirectiveScope.PROJECT, null, projectId);

        when(projectRepository.findById(projectId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> directiveService.createDirective(request));
    }

    // ── getDirective ─────────────────────────────────────────────────

    @Test
    void getDirective_teamScoped_success() {
        when(directiveRepository.findById(directiveId)).thenReturn(Optional.of(testDirective));
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(true);

        DirectiveResponse response = directiveService.getDirective(directiveId);

        assertNotNull(response);
        assertEquals(directiveId, response.id());
        assertEquals("Test Directive", response.name());
        assertEquals(DirectiveCategory.ARCHITECTURE, response.category());
    }

    @Test
    void getDirective_notFound_throws() {
        when(directiveRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> directiveService.getDirective(UUID.randomUUID()));
    }

    @Test
    void getDirective_notTeamMember_throws() {
        when(directiveRepository.findById(directiveId)).thenReturn(Optional.of(testDirective));
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(false);

        assertThrows(AccessDeniedException.class,
                () -> directiveService.getDirective(directiveId));
    }

    @Test
    void getDirective_projectScoped_verifiesMembershipViaProjectTeam() {
        Directive projectDirective = Directive.builder()
                .name("Project Dir")
                .description("desc")
                .contentMd("# md")
                .category(DirectiveCategory.CONVENTIONS)
                .scope(DirectiveScope.PROJECT)
                .team(null)
                .project(testProject)
                .createdBy(testUser)
                .version(1)
                .build();
        projectDirective.setId(UUID.randomUUID());
        projectDirective.setCreatedAt(Instant.now());
        projectDirective.setUpdatedAt(Instant.now());

        when(directiveRepository.findById(projectDirective.getId()))
                .thenReturn(Optional.of(projectDirective));
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(true);

        DirectiveResponse response = directiveService.getDirective(projectDirective.getId());
        assertNotNull(response);
        assertEquals(projectId, response.projectId());
    }

    // ── getDirectivesForTeam ─────────────────────────────────────────

    @Test
    void getDirectivesForTeam_success() {
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(true);
        when(directiveRepository.findByTeamId(teamId)).thenReturn(List.of(testDirective));

        List<DirectiveResponse> responses = directiveService.getDirectivesForTeam(teamId);

        assertEquals(1, responses.size());
        assertEquals("Test Directive", responses.get(0).name());
    }

    @Test
    void getDirectivesForTeam_notMember_throws() {
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(false);

        assertThrows(AccessDeniedException.class,
                () -> directiveService.getDirectivesForTeam(teamId));
    }

    @Test
    void getDirectivesForTeam_empty_returnsEmptyList() {
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(true);
        when(directiveRepository.findByTeamId(teamId)).thenReturn(Collections.emptyList());

        List<DirectiveResponse> responses = directiveService.getDirectivesForTeam(teamId);
        assertTrue(responses.isEmpty());
    }

    // ── getDirectivesForProject ──────────────────────────────────────

    @Test
    void getDirectivesForProject_success() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(testProject));
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(true);
        when(directiveRepository.findByProjectId(projectId)).thenReturn(List.of(testDirective));

        List<DirectiveResponse> responses = directiveService.getDirectivesForProject(projectId);

        assertEquals(1, responses.size());
    }

    @Test
    void getDirectivesForProject_projectNotFound_throws() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> directiveService.getDirectivesForProject(projectId));
    }

    @Test
    void getDirectivesForProject_notMember_throws() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(testProject));
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(false);

        assertThrows(AccessDeniedException.class,
                () -> directiveService.getDirectivesForProject(projectId));
    }

    // ── updateDirective ──────────────────────────────────────────────

    @Test
    void updateDirective_byCreator_success() {
        var request = new UpdateDirectiveRequest("Updated Name", "Updated desc", "# new content", DirectiveCategory.CONVENTIONS);

        when(directiveRepository.findById(directiveId)).thenReturn(Optional.of(testDirective));
        when(directiveRepository.save(any(Directive.class))).thenReturn(testDirective);

        DirectiveResponse response = directiveService.updateDirective(directiveId, request);

        assertEquals("Updated Name", testDirective.getName());
        assertEquals("Updated desc", testDirective.getDescription());
        assertEquals("# new content", testDirective.getContentMd());
        assertEquals(DirectiveCategory.CONVENTIONS, testDirective.getCategory());
        assertEquals(2, testDirective.getVersion()); // version bumped because contentMd changed
        verify(directiveRepository).save(testDirective);
    }

    @Test
    void updateDirective_nullFields_notUpdated() {
        var request = new UpdateDirectiveRequest(null, null, null, null);

        when(directiveRepository.findById(directiveId)).thenReturn(Optional.of(testDirective));
        when(directiveRepository.save(any(Directive.class))).thenReturn(testDirective);

        directiveService.updateDirective(directiveId, request);

        assertEquals("Test Directive", testDirective.getName());
        assertEquals("Test description", testDirective.getDescription());
        assertEquals("# Test content", testDirective.getContentMd());
        assertEquals(1, testDirective.getVersion()); // version NOT bumped
    }

    @Test
    void updateDirective_contentMdOnly_bumpsVersion() {
        var request = new UpdateDirectiveRequest(null, null, "# changed content", null);

        when(directiveRepository.findById(directiveId)).thenReturn(Optional.of(testDirective));
        when(directiveRepository.save(any(Directive.class))).thenReturn(testDirective);

        directiveService.updateDirective(directiveId, request);

        assertEquals("# changed content", testDirective.getContentMd());
        assertEquals(2, testDirective.getVersion());
    }

    @Test
    void updateDirective_byTeamAdmin_notCreator_success() {
        UUID adminId = UUID.randomUUID();
        setSecurityContext(adminId);

        User adminUser = User.builder().email("admin@codeops.dev").passwordHash("h").displayName("Admin").build();
        adminUser.setId(adminId);

        TeamMember admin = TeamMember.builder()
                .team(testTeam).user(adminUser).role(TeamRole.OWNER).joinedAt(Instant.now()).build();

        when(directiveRepository.findById(directiveId)).thenReturn(Optional.of(testDirective));
        when(teamMemberRepository.findByTeamIdAndUserId(teamId, adminId))
                .thenReturn(Optional.of(admin));
        when(directiveRepository.save(any(Directive.class))).thenReturn(testDirective);

        var request = new UpdateDirectiveRequest("Admin Updated", null, null, null);
        DirectiveResponse response = directiveService.updateDirective(directiveId, request);

        assertEquals("Admin Updated", testDirective.getName());
    }

    @Test
    void updateDirective_notCreatorNotAdmin_throws() {
        UUID otherId = UUID.randomUUID();
        setSecurityContext(otherId);

        TeamMember viewer = TeamMember.builder()
                .team(testTeam).user(testUser).role(TeamRole.MEMBER).joinedAt(Instant.now()).build();

        when(directiveRepository.findById(directiveId)).thenReturn(Optional.of(testDirective));
        when(teamMemberRepository.findByTeamIdAndUserId(teamId, otherId))
                .thenReturn(Optional.of(viewer));

        var request = new UpdateDirectiveRequest("Fail", null, null, null);
        assertThrows(AccessDeniedException.class,
                () -> directiveService.updateDirective(directiveId, request));
    }

    @Test
    void updateDirective_notFound_throws() {
        when(directiveRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

        var request = new UpdateDirectiveRequest("X", null, null, null);
        assertThrows(EntityNotFoundException.class,
                () -> directiveService.updateDirective(UUID.randomUUID(), request));
    }

    // ── deleteDirective ──────────────────────────────────────────────

    @Test
    void deleteDirective_byCreator_success() {
        when(directiveRepository.findById(directiveId)).thenReturn(Optional.of(testDirective));
        when(projectDirectiveRepository.findByDirectiveId(directiveId)).thenReturn(Collections.emptyList());

        directiveService.deleteDirective(directiveId);

        verify(projectDirectiveRepository).deleteAll(Collections.emptyList());
        verify(directiveRepository).delete(testDirective);
    }

    @Test
    void deleteDirective_deletesProjectDirectiveAssociations() {
        ProjectDirective pd = ProjectDirective.builder()
                .id(new ProjectDirectiveId(projectId, directiveId))
                .project(testProject)
                .directive(testDirective)
                .enabled(true)
                .build();

        when(directiveRepository.findById(directiveId)).thenReturn(Optional.of(testDirective));
        when(projectDirectiveRepository.findByDirectiveId(directiveId)).thenReturn(List.of(pd));

        directiveService.deleteDirective(directiveId);

        verify(projectDirectiveRepository).deleteAll(List.of(pd));
        verify(directiveRepository).delete(testDirective);
    }

    @Test
    void deleteDirective_notFound_throws() {
        when(directiveRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> directiveService.deleteDirective(UUID.randomUUID()));
    }

    @Test
    void deleteDirective_notCreatorNotAdmin_throws() {
        UUID otherId = UUID.randomUUID();
        setSecurityContext(otherId);

        TeamMember viewer = TeamMember.builder()
                .team(testTeam).user(testUser).role(TeamRole.VIEWER).joinedAt(Instant.now()).build();

        when(directiveRepository.findById(directiveId)).thenReturn(Optional.of(testDirective));
        when(teamMemberRepository.findByTeamIdAndUserId(teamId, otherId))
                .thenReturn(Optional.of(viewer));

        assertThrows(AccessDeniedException.class,
                () -> directiveService.deleteDirective(directiveId));
        verify(directiveRepository, never()).delete(any());
    }

    // ── assignToProject ──────────────────────────────────────────────

    @Test
    void assignToProject_success() {
        var request = new AssignDirectiveRequest(projectId, directiveId, true);

        when(directiveRepository.findById(directiveId)).thenReturn(Optional.of(testDirective));
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(testProject));
        when(teamMemberRepository.findByTeamIdAndUserId(teamId, userId))
                .thenReturn(Optional.of(adminMember));
        when(projectDirectiveRepository.findByProjectId(projectId)).thenReturn(Collections.emptyList());
        when(projectDirectiveRepository.save(any(ProjectDirective.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProjectDirectiveResponse response = directiveService.assignToProject(request);

        assertNotNull(response);
        assertEquals(projectId, response.projectId());
        assertEquals(directiveId, response.directiveId());
        assertTrue(response.enabled());
        assertEquals("Test Directive", response.directiveName());
        verify(projectDirectiveRepository).save(any(ProjectDirective.class));
    }

    @Test
    void assignToProject_maxLimitReached_throws() {
        var request = new AssignDirectiveRequest(projectId, directiveId, true);

        when(directiveRepository.findById(directiveId)).thenReturn(Optional.of(testDirective));
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(testProject));
        when(teamMemberRepository.findByTeamIdAndUserId(teamId, userId))
                .thenReturn(Optional.of(adminMember));

        // Create a list of exactly MAX_DIRECTIVES_PER_PROJECT items
        List<ProjectDirective> existingDirectives = IntStream.range(0, AppConstants.MAX_DIRECTIVES_PER_PROJECT)
                .mapToObj(i -> ProjectDirective.builder()
                        .id(new ProjectDirectiveId(projectId, UUID.randomUUID()))
                        .project(testProject)
                        .directive(testDirective)
                        .enabled(true)
                        .build())
                .toList();
        when(projectDirectiveRepository.findByProjectId(projectId)).thenReturn(existingDirectives);

        assertThrows(IllegalArgumentException.class,
                () -> directiveService.assignToProject(request));
        verify(projectDirectiveRepository, never()).save(any());
    }

    @Test
    void assignToProject_directiveNotFound_throws() {
        var request = new AssignDirectiveRequest(projectId, directiveId, true);

        when(directiveRepository.findById(directiveId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> directiveService.assignToProject(request));
    }

    @Test
    void assignToProject_projectNotFound_throws() {
        var request = new AssignDirectiveRequest(projectId, directiveId, true);

        when(directiveRepository.findById(directiveId)).thenReturn(Optional.of(testDirective));
        when(projectRepository.findById(projectId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> directiveService.assignToProject(request));
    }

    @Test
    void assignToProject_notAdmin_throws() {
        var request = new AssignDirectiveRequest(projectId, directiveId, true);

        TeamMember member = TeamMember.builder()
                .team(testTeam).user(testUser).role(TeamRole.MEMBER).joinedAt(Instant.now()).build();

        when(directiveRepository.findById(directiveId)).thenReturn(Optional.of(testDirective));
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(testProject));
        when(teamMemberRepository.findByTeamIdAndUserId(teamId, userId))
                .thenReturn(Optional.of(member));

        assertThrows(AccessDeniedException.class,
                () -> directiveService.assignToProject(request));
    }

    // ── removeFromProject ────────────────────────────────────────────

    @Test
    void removeFromProject_success() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(testProject));
        when(teamMemberRepository.findByTeamIdAndUserId(teamId, userId))
                .thenReturn(Optional.of(adminMember));

        directiveService.removeFromProject(projectId, directiveId);

        verify(projectDirectiveRepository).deleteByProjectIdAndDirectiveId(projectId, directiveId);
    }

    @Test
    void removeFromProject_projectNotFound_throws() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> directiveService.removeFromProject(projectId, directiveId));
    }

    @Test
    void removeFromProject_notAdmin_throws() {
        TeamMember member = TeamMember.builder()
                .team(testTeam).user(testUser).role(TeamRole.MEMBER).joinedAt(Instant.now()).build();

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(testProject));
        when(teamMemberRepository.findByTeamIdAndUserId(teamId, userId))
                .thenReturn(Optional.of(member));

        assertThrows(AccessDeniedException.class,
                () -> directiveService.removeFromProject(projectId, directiveId));
    }

    // ── getProjectDirectives ─────────────────────────────────────────

    @Test
    void getProjectDirectives_success() {
        ProjectDirective pd = ProjectDirective.builder()
                .id(new ProjectDirectiveId(projectId, directiveId))
                .project(testProject)
                .directive(testDirective)
                .enabled(true)
                .build();

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(testProject));
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(true);
        when(projectDirectiveRepository.findByProjectId(projectId)).thenReturn(List.of(pd));

        List<ProjectDirectiveResponse> responses = directiveService.getProjectDirectives(projectId);

        assertEquals(1, responses.size());
        assertEquals(projectId, responses.get(0).projectId());
        assertEquals(directiveId, responses.get(0).directiveId());
        assertTrue(responses.get(0).enabled());
    }

    @Test
    void getProjectDirectives_projectNotFound_throws() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> directiveService.getProjectDirectives(projectId));
    }

    @Test
    void getProjectDirectives_notMember_throws() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(testProject));
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(false);

        assertThrows(AccessDeniedException.class,
                () -> directiveService.getProjectDirectives(projectId));
    }

    // ── getEnabledDirectivesForProject ───────────────────────────────

    @Test
    void getEnabledDirectivesForProject_success() {
        ProjectDirective pd = ProjectDirective.builder()
                .id(new ProjectDirectiveId(projectId, directiveId))
                .project(testProject)
                .directive(testDirective)
                .enabled(true)
                .build();

        when(projectDirectiveRepository.findByProjectIdAndEnabledTrue(projectId))
                .thenReturn(List.of(pd));

        List<DirectiveResponse> responses = directiveService.getEnabledDirectivesForProject(projectId);

        assertEquals(1, responses.size());
        assertEquals("Test Directive", responses.get(0).name());
    }

    @Test
    void getEnabledDirectivesForProject_noneEnabled_returnsEmpty() {
        when(projectDirectiveRepository.findByProjectIdAndEnabledTrue(projectId))
                .thenReturn(Collections.emptyList());

        List<DirectiveResponse> responses = directiveService.getEnabledDirectivesForProject(projectId);
        assertTrue(responses.isEmpty());
    }

    // ── toggleProjectDirective ───────────────────────────────────────

    @Test
    void toggleProjectDirective_enable_success() {
        ProjectDirectiveId pdId = new ProjectDirectiveId(projectId, directiveId);
        ProjectDirective pd = ProjectDirective.builder()
                .id(pdId)
                .project(testProject)
                .directive(testDirective)
                .enabled(false)
                .build();

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(testProject));
        when(teamMemberRepository.findByTeamIdAndUserId(teamId, userId))
                .thenReturn(Optional.of(adminMember));
        when(projectDirectiveRepository.findById(pdId)).thenReturn(Optional.of(pd));
        when(projectDirectiveRepository.save(any(ProjectDirective.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProjectDirectiveResponse response = directiveService.toggleProjectDirective(projectId, directiveId, true);

        assertTrue(response.enabled());
        assertTrue(pd.getEnabled());
        verify(projectDirectiveRepository).save(pd);
    }

    @Test
    void toggleProjectDirective_disable_success() {
        ProjectDirectiveId pdId = new ProjectDirectiveId(projectId, directiveId);
        ProjectDirective pd = ProjectDirective.builder()
                .id(pdId)
                .project(testProject)
                .directive(testDirective)
                .enabled(true)
                .build();

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(testProject));
        when(teamMemberRepository.findByTeamIdAndUserId(teamId, userId))
                .thenReturn(Optional.of(adminMember));
        when(projectDirectiveRepository.findById(pdId)).thenReturn(Optional.of(pd));
        when(projectDirectiveRepository.save(any(ProjectDirective.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProjectDirectiveResponse response = directiveService.toggleProjectDirective(projectId, directiveId, false);

        assertFalse(response.enabled());
        assertFalse(pd.getEnabled());
    }

    @Test
    void toggleProjectDirective_assignmentNotFound_throws() {
        ProjectDirectiveId pdId = new ProjectDirectiveId(projectId, directiveId);

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(testProject));
        when(teamMemberRepository.findByTeamIdAndUserId(teamId, userId))
                .thenReturn(Optional.of(adminMember));
        when(projectDirectiveRepository.findById(pdId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> directiveService.toggleProjectDirective(projectId, directiveId, true));
    }

    @Test
    void toggleProjectDirective_notAdmin_throws() {
        TeamMember member = TeamMember.builder()
                .team(testTeam).user(testUser).role(TeamRole.MEMBER).joinedAt(Instant.now()).build();

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(testProject));
        when(teamMemberRepository.findByTeamIdAndUserId(teamId, userId))
                .thenReturn(Optional.of(member));

        assertThrows(AccessDeniedException.class,
                () -> directiveService.toggleProjectDirective(projectId, directiveId, true));
    }

    // ── helper ───────────────────────────────────────────────────────

    private void setSecurityContext(UUID userId) {
        var auth = new UsernamePasswordAuthenticationToken(userId, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
