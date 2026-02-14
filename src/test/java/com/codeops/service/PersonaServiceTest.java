package com.codeops.service;

import com.codeops.config.AppConstants;
import com.codeops.dto.request.CreatePersonaRequest;
import com.codeops.dto.request.UpdatePersonaRequest;
import com.codeops.dto.response.PageResponse;
import com.codeops.dto.response.PersonaResponse;
import com.codeops.entity.Persona;
import com.codeops.entity.Team;
import com.codeops.entity.TeamMember;
import com.codeops.entity.User;
import com.codeops.entity.enums.AgentType;
import com.codeops.entity.enums.Scope;
import com.codeops.entity.enums.TeamRole;
import com.codeops.repository.PersonaRepository;
import com.codeops.repository.TeamMemberRepository;
import com.codeops.repository.TeamRepository;
import com.codeops.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
class PersonaServiceTest {

    @Mock private PersonaRepository personaRepository;
    @Mock private TeamMemberRepository teamMemberRepository;
    @Mock private UserRepository userRepository;
    @Mock private TeamRepository teamRepository;

    @InjectMocks
    private PersonaService personaService;

    private UUID userId;
    private UUID teamId;
    private UUID personaId;
    private User testUser;
    private Team testTeam;
    private Persona testPersona;
    private TeamMember adminMember;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        teamId = UUID.randomUUID();
        personaId = UUID.randomUUID();

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

        testPersona = Persona.builder()
                .name("Test Persona")
                .agentType(AgentType.CODE_QUALITY)
                .description("A test persona")
                .contentMd("# Persona content")
                .scope(Scope.TEAM)
                .team(testTeam)
                .createdBy(testUser)
                .isDefault(false)
                .version(1)
                .build();
        testPersona.setId(personaId);
        testPersona.setCreatedAt(Instant.now());
        testPersona.setUpdatedAt(Instant.now());

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

    // ── createPersona ────────────────────────────────────────────────

    @Test
    void createPersona_teamScope_success() {
        var request = new CreatePersonaRequest(
                "New Persona", AgentType.SECURITY, "desc", "# content",
                Scope.TEAM, teamId, false);

        when(teamMemberRepository.findByTeamIdAndUserId(teamId, userId))
                .thenReturn(Optional.of(adminMember));
        when(personaRepository.findByTeamId(teamId)).thenReturn(Collections.emptyList());
        when(teamRepository.findById(teamId)).thenReturn(Optional.of(testTeam));
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(personaRepository.save(any(Persona.class))).thenAnswer(invocation -> {
            Persona p = invocation.getArgument(0);
            p.setId(UUID.randomUUID());
            p.setCreatedAt(Instant.now());
            p.setUpdatedAt(Instant.now());
            return p;
        });

        PersonaResponse response = personaService.createPersona(request);

        assertNotNull(response);
        assertEquals("New Persona", response.name());
        assertEquals(AgentType.SECURITY, response.agentType());
        assertEquals(Scope.TEAM, response.scope());
        assertEquals(teamId, response.teamId());
        assertEquals(1, response.version());
        verify(personaRepository).save(any(Persona.class));
    }

    @Test
    void createPersona_userScope_success() {
        var request = new CreatePersonaRequest(
                "User Persona", AgentType.ARCHITECTURE, "desc", "# content",
                Scope.USER, null, false);

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(personaRepository.save(any(Persona.class))).thenAnswer(invocation -> {
            Persona p = invocation.getArgument(0);
            p.setId(UUID.randomUUID());
            p.setCreatedAt(Instant.now());
            p.setUpdatedAt(Instant.now());
            return p;
        });

        PersonaResponse response = personaService.createPersona(request);

        assertNotNull(response);
        assertEquals(Scope.USER, response.scope());
        assertNull(response.teamId());
        verify(teamMemberRepository, never()).findByTeamIdAndUserId(any(), any());
    }

    @Test
    void createPersona_systemScope_throws() {
        var request = new CreatePersonaRequest(
                "System", AgentType.SECURITY, "desc", "# content",
                Scope.SYSTEM, null, false);

        assertThrows(IllegalArgumentException.class,
                () -> personaService.createPersona(request));
        verify(personaRepository, never()).save(any());
    }

    @Test
    void createPersona_teamScope_missingTeamId_throws() {
        var request = new CreatePersonaRequest(
                "Persona", AgentType.SECURITY, "desc", "# content",
                Scope.TEAM, null, false);

        assertThrows(IllegalArgumentException.class,
                () -> personaService.createPersona(request));
        verify(personaRepository, never()).save(any());
    }

    @Test
    void createPersona_teamScope_notAdmin_throws() {
        var request = new CreatePersonaRequest(
                "Persona", AgentType.SECURITY, "desc", "# content",
                Scope.TEAM, teamId, false);

        TeamMember viewer = TeamMember.builder()
                .team(testTeam).user(testUser).role(TeamRole.VIEWER).joinedAt(Instant.now()).build();
        when(teamMemberRepository.findByTeamIdAndUserId(teamId, userId))
                .thenReturn(Optional.of(viewer));

        assertThrows(AccessDeniedException.class,
                () -> personaService.createPersona(request));
    }

    @Test
    void createPersona_maxLimitReached_throws() {
        var request = new CreatePersonaRequest(
                "Persona", AgentType.SECURITY, "desc", "# content",
                Scope.TEAM, teamId, false);

        when(teamMemberRepository.findByTeamIdAndUserId(teamId, userId))
                .thenReturn(Optional.of(adminMember));

        List<Persona> existingPersonas = IntStream.range(0, AppConstants.MAX_PERSONAS_PER_TEAM)
                .mapToObj(i -> testPersona)
                .toList();
        when(personaRepository.findByTeamId(teamId)).thenReturn(existingPersonas);

        assertThrows(IllegalArgumentException.class,
                () -> personaService.createPersona(request));
        verify(personaRepository, never()).save(any());
    }

    @Test
    void createPersona_withDefault_clearsExistingDefault() {
        var request = new CreatePersonaRequest(
                "Default Persona", AgentType.SECURITY, "desc", "# content",
                Scope.TEAM, teamId, true);

        Persona existingDefault = Persona.builder()
                .name("Old Default").agentType(AgentType.SECURITY).contentMd("# old")
                .scope(Scope.TEAM).team(testTeam).createdBy(testUser).isDefault(true).version(1).build();
        existingDefault.setId(UUID.randomUUID());
        existingDefault.setCreatedAt(Instant.now());
        existingDefault.setUpdatedAt(Instant.now());

        when(teamMemberRepository.findByTeamIdAndUserId(teamId, userId))
                .thenReturn(Optional.of(adminMember));
        when(personaRepository.findByTeamId(teamId)).thenReturn(Collections.emptyList());
        when(teamRepository.findById(teamId)).thenReturn(Optional.of(testTeam));
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(personaRepository.findByTeamIdAndAgentTypeAndIsDefaultTrue(teamId, AgentType.SECURITY))
                .thenReturn(Optional.of(existingDefault));
        when(personaRepository.save(any(Persona.class))).thenAnswer(invocation -> {
            Persona p = invocation.getArgument(0);
            if (p.getId() == null) {
                p.setId(UUID.randomUUID());
                p.setCreatedAt(Instant.now());
            }
            p.setUpdatedAt(Instant.now());
            return p;
        });

        personaService.createPersona(request);

        assertFalse(existingDefault.getIsDefault());
        // save called twice: once for clearing default, once for new persona
        verify(personaRepository, times(2)).save(any(Persona.class));
    }

    // ── getPersona ───────────────────────────────────────────────────

    @Test
    void getPersona_teamScoped_success() {
        when(personaRepository.findById(personaId)).thenReturn(Optional.of(testPersona));
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(true);

        PersonaResponse response = personaService.getPersona(personaId);

        assertNotNull(response);
        assertEquals(personaId, response.id());
        assertEquals("Test Persona", response.name());
        assertEquals(AgentType.CODE_QUALITY, response.agentType());
    }

    @Test
    void getPersona_noTeam_noMembershipCheck() {
        Persona userPersona = Persona.builder()
                .name("User Persona").agentType(AgentType.SECURITY).contentMd("# md")
                .scope(Scope.USER).team(null).createdBy(testUser).isDefault(false).version(1).build();
        userPersona.setId(UUID.randomUUID());
        userPersona.setCreatedAt(Instant.now());
        userPersona.setUpdatedAt(Instant.now());

        when(personaRepository.findById(userPersona.getId())).thenReturn(Optional.of(userPersona));

        PersonaResponse response = personaService.getPersona(userPersona.getId());

        assertNotNull(response);
        verify(teamMemberRepository, never()).existsByTeamIdAndUserId(any(), any());
    }

    @Test
    void getPersona_notFound_throws() {
        when(personaRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> personaService.getPersona(UUID.randomUUID()));
    }

    @Test
    void getPersona_notTeamMember_throws() {
        when(personaRepository.findById(personaId)).thenReturn(Optional.of(testPersona));
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(false);

        assertThrows(AccessDeniedException.class,
                () -> personaService.getPersona(personaId));
    }

    // ── getPersonasForTeam ───────────────────────────────────────────

    @Test
    void getPersonasForTeam_success() {
        Pageable pageable = PageRequest.of(0, 20);
        var page = new PageImpl<>(List.of(testPersona), pageable, 1);

        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(true);
        when(personaRepository.findByTeamId(teamId, pageable)).thenReturn(page);

        PageResponse<PersonaResponse> response = personaService.getPersonasForTeam(teamId, pageable);

        assertEquals(1, response.content().size());
        assertEquals(0, response.page());
        assertEquals(20, response.size());
        assertEquals(1, response.totalElements());
        assertTrue(response.isLast());
    }

    @Test
    void getPersonasForTeam_notMember_throws() {
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(false);

        assertThrows(AccessDeniedException.class,
                () -> personaService.getPersonasForTeam(teamId, PageRequest.of(0, 20)));
    }

    // ── getPersonasByAgentType ───────────────────────────────────────

    @Test
    void getPersonasByAgentType_success() {
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(true);
        when(personaRepository.findByTeamIdAndAgentType(teamId, AgentType.CODE_QUALITY))
                .thenReturn(List.of(testPersona));

        List<PersonaResponse> responses = personaService.getPersonasByAgentType(teamId, AgentType.CODE_QUALITY);

        assertEquals(1, responses.size());
        assertEquals(AgentType.CODE_QUALITY, responses.get(0).agentType());
    }

    @Test
    void getPersonasByAgentType_notMember_throws() {
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(false);

        assertThrows(AccessDeniedException.class,
                () -> personaService.getPersonasByAgentType(teamId, AgentType.SECURITY));
    }

    // ── getDefaultPersona ────────────────────────────────────────────

    @Test
    void getDefaultPersona_success() {
        Persona defaultPersona = Persona.builder()
                .name("Default").agentType(AgentType.SECURITY).contentMd("# default")
                .scope(Scope.TEAM).team(testTeam).createdBy(testUser).isDefault(true).version(1).build();
        defaultPersona.setId(UUID.randomUUID());
        defaultPersona.setCreatedAt(Instant.now());
        defaultPersona.setUpdatedAt(Instant.now());

        when(personaRepository.findByTeamIdAndAgentTypeAndIsDefaultTrue(teamId, AgentType.SECURITY))
                .thenReturn(Optional.of(defaultPersona));

        PersonaResponse response = personaService.getDefaultPersona(teamId, AgentType.SECURITY);

        assertNotNull(response);
        assertTrue(response.isDefault());
    }

    @Test
    void getDefaultPersona_notFound_throws() {
        when(personaRepository.findByTeamIdAndAgentTypeAndIsDefaultTrue(teamId, AgentType.SECURITY))
                .thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> personaService.getDefaultPersona(teamId, AgentType.SECURITY));
    }

    // ── getPersonasByUser ────────────────────────────────────────────

    @Test
    void getPersonasByUser_success() {
        when(personaRepository.findByCreatedById(userId)).thenReturn(List.of(testPersona));

        List<PersonaResponse> responses = personaService.getPersonasByUser(userId);

        assertEquals(1, responses.size());
    }

    @Test
    void getPersonasByUser_empty_returnsEmptyList() {
        when(personaRepository.findByCreatedById(userId)).thenReturn(Collections.emptyList());

        List<PersonaResponse> responses = personaService.getPersonasByUser(userId);
        assertTrue(responses.isEmpty());
    }

    // ── getSystemPersonas ────────────────────────────────────────────

    @Test
    void getSystemPersonas_success() {
        Persona systemPersona = Persona.builder()
                .name("System").agentType(AgentType.SECURITY).contentMd("# system")
                .scope(Scope.SYSTEM).team(null).createdBy(testUser).isDefault(false).version(1).build();
        systemPersona.setId(UUID.randomUUID());
        systemPersona.setCreatedAt(Instant.now());
        systemPersona.setUpdatedAt(Instant.now());

        when(personaRepository.findByScope(Scope.SYSTEM)).thenReturn(List.of(systemPersona));

        List<PersonaResponse> responses = personaService.getSystemPersonas();

        assertEquals(1, responses.size());
        assertEquals(Scope.SYSTEM, responses.get(0).scope());
    }

    // ── updatePersona ────────────────────────────────────────────────

    @Test
    void updatePersona_byCreator_success() {
        var request = new UpdatePersonaRequest("Updated Name", "Updated desc", "# new md", null);

        when(personaRepository.findById(personaId)).thenReturn(Optional.of(testPersona));
        when(personaRepository.save(any(Persona.class))).thenReturn(testPersona);

        PersonaResponse response = personaService.updatePersona(personaId, request);

        assertEquals("Updated Name", testPersona.getName());
        assertEquals("Updated desc", testPersona.getDescription());
        assertEquals("# new md", testPersona.getContentMd());
        assertEquals(2, testPersona.getVersion()); // version bumped
        verify(personaRepository).save(testPersona);
    }

    @Test
    void updatePersona_nullFields_notUpdated() {
        var request = new UpdatePersonaRequest(null, null, null, null);

        when(personaRepository.findById(personaId)).thenReturn(Optional.of(testPersona));
        when(personaRepository.save(any(Persona.class))).thenReturn(testPersona);

        personaService.updatePersona(personaId, request);

        assertEquals("Test Persona", testPersona.getName());
        assertEquals("A test persona", testPersona.getDescription());
        assertEquals("# Persona content", testPersona.getContentMd());
        assertEquals(1, testPersona.getVersion());
    }

    @Test
    void updatePersona_contentMdOnly_bumpsVersion() {
        var request = new UpdatePersonaRequest(null, null, "# changed", null);

        when(personaRepository.findById(personaId)).thenReturn(Optional.of(testPersona));
        when(personaRepository.save(any(Persona.class))).thenReturn(testPersona);

        personaService.updatePersona(personaId, request);

        assertEquals("# changed", testPersona.getContentMd());
        assertEquals(2, testPersona.getVersion());
    }

    @Test
    void updatePersona_setIsDefault_clearsExistingDefault() {
        var request = new UpdatePersonaRequest(null, null, null, true);

        Persona existingDefault = Persona.builder()
                .name("Old Default").agentType(AgentType.CODE_QUALITY).contentMd("# old")
                .scope(Scope.TEAM).team(testTeam).createdBy(testUser).isDefault(true).version(1).build();
        existingDefault.setId(UUID.randomUUID());
        existingDefault.setCreatedAt(Instant.now());
        existingDefault.setUpdatedAt(Instant.now());

        when(personaRepository.findById(personaId)).thenReturn(Optional.of(testPersona));
        when(personaRepository.findByTeamIdAndAgentTypeAndIsDefaultTrue(teamId, AgentType.CODE_QUALITY))
                .thenReturn(Optional.of(existingDefault));
        when(personaRepository.save(any(Persona.class))).thenAnswer(invocation -> invocation.getArgument(0));

        personaService.updatePersona(personaId, request);

        assertTrue(testPersona.getIsDefault());
        assertFalse(existingDefault.getIsDefault());
    }

    @Test
    void updatePersona_systemScope_throws() {
        Persona systemPersona = Persona.builder()
                .name("System").agentType(AgentType.SECURITY).contentMd("# system")
                .scope(Scope.SYSTEM).team(null).createdBy(testUser).isDefault(false).version(1).build();
        systemPersona.setId(UUID.randomUUID());

        when(personaRepository.findById(systemPersona.getId())).thenReturn(Optional.of(systemPersona));

        var request = new UpdatePersonaRequest("Hacked", null, null, null);
        assertThrows(IllegalArgumentException.class,
                () -> personaService.updatePersona(systemPersona.getId(), request));
        verify(personaRepository, never()).save(any());
    }

    @Test
    void updatePersona_byTeamAdmin_notCreator_success() {
        UUID adminId = UUID.randomUUID();
        setSecurityContext(adminId);

        User adminUser = User.builder().email("admin@codeops.dev").passwordHash("h").displayName("Admin").build();
        adminUser.setId(adminId);

        TeamMember admin = TeamMember.builder()
                .team(testTeam).user(adminUser).role(TeamRole.OWNER).joinedAt(Instant.now()).build();

        when(personaRepository.findById(personaId)).thenReturn(Optional.of(testPersona));
        when(teamMemberRepository.findByTeamIdAndUserId(teamId, adminId))
                .thenReturn(Optional.of(admin));
        when(personaRepository.save(any(Persona.class))).thenReturn(testPersona);

        var request = new UpdatePersonaRequest("Admin Updated", null, null, null);
        personaService.updatePersona(personaId, request);

        assertEquals("Admin Updated", testPersona.getName());
    }

    @Test
    void updatePersona_notCreatorNotAdmin_throws() {
        UUID otherId = UUID.randomUUID();
        setSecurityContext(otherId);

        TeamMember viewer = TeamMember.builder()
                .team(testTeam).user(testUser).role(TeamRole.MEMBER).joinedAt(Instant.now()).build();

        when(personaRepository.findById(personaId)).thenReturn(Optional.of(testPersona));
        when(teamMemberRepository.findByTeamIdAndUserId(teamId, otherId))
                .thenReturn(Optional.of(viewer));

        var request = new UpdatePersonaRequest("Fail", null, null, null);
        assertThrows(AccessDeniedException.class,
                () -> personaService.updatePersona(personaId, request));
    }

    @Test
    void updatePersona_notFound_throws() {
        when(personaRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

        var request = new UpdatePersonaRequest("X", null, null, null);
        assertThrows(EntityNotFoundException.class,
                () -> personaService.updatePersona(UUID.randomUUID(), request));
    }

    // ── deletePersona ────────────────────────────────────────────────

    @Test
    void deletePersona_byCreator_success() {
        when(personaRepository.findById(personaId)).thenReturn(Optional.of(testPersona));

        personaService.deletePersona(personaId);

        verify(personaRepository).delete(testPersona);
    }

    @Test
    void deletePersona_systemScope_throws() {
        Persona systemPersona = Persona.builder()
                .name("System").agentType(AgentType.SECURITY).contentMd("# system")
                .scope(Scope.SYSTEM).team(null).createdBy(testUser).isDefault(false).version(1).build();
        systemPersona.setId(UUID.randomUUID());

        when(personaRepository.findById(systemPersona.getId())).thenReturn(Optional.of(systemPersona));

        assertThrows(IllegalArgumentException.class,
                () -> personaService.deletePersona(systemPersona.getId()));
        verify(personaRepository, never()).delete(any());
    }

    @Test
    void deletePersona_notFound_throws() {
        when(personaRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> personaService.deletePersona(UUID.randomUUID()));
    }

    @Test
    void deletePersona_notCreatorNotAdmin_throws() {
        UUID otherId = UUID.randomUUID();
        setSecurityContext(otherId);

        TeamMember viewer = TeamMember.builder()
                .team(testTeam).user(testUser).role(TeamRole.VIEWER).joinedAt(Instant.now()).build();

        when(personaRepository.findById(personaId)).thenReturn(Optional.of(testPersona));
        when(teamMemberRepository.findByTeamIdAndUserId(teamId, otherId))
                .thenReturn(Optional.of(viewer));

        assertThrows(AccessDeniedException.class,
                () -> personaService.deletePersona(personaId));
        verify(personaRepository, never()).delete(any());
    }

    // ── setAsDefault ─────────────────────────────────────────────────

    @Test
    void setAsDefault_success() {
        when(personaRepository.findById(personaId)).thenReturn(Optional.of(testPersona));
        when(teamMemberRepository.findByTeamIdAndUserId(teamId, userId))
                .thenReturn(Optional.of(adminMember));
        when(personaRepository.findByTeamIdAndAgentTypeAndIsDefaultTrue(teamId, AgentType.CODE_QUALITY))
                .thenReturn(Optional.empty());
        when(personaRepository.save(any(Persona.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PersonaResponse response = personaService.setAsDefault(personaId);

        assertTrue(response.isDefault());
        assertTrue(testPersona.getIsDefault());
    }

    @Test
    void setAsDefault_clearsExistingDefault() {
        Persona existingDefault = Persona.builder()
                .name("Old Default").agentType(AgentType.CODE_QUALITY).contentMd("# old")
                .scope(Scope.TEAM).team(testTeam).createdBy(testUser).isDefault(true).version(1).build();
        existingDefault.setId(UUID.randomUUID());
        existingDefault.setCreatedAt(Instant.now());
        existingDefault.setUpdatedAt(Instant.now());

        when(personaRepository.findById(personaId)).thenReturn(Optional.of(testPersona));
        when(teamMemberRepository.findByTeamIdAndUserId(teamId, userId))
                .thenReturn(Optional.of(adminMember));
        when(personaRepository.findByTeamIdAndAgentTypeAndIsDefaultTrue(teamId, AgentType.CODE_QUALITY))
                .thenReturn(Optional.of(existingDefault));
        when(personaRepository.save(any(Persona.class))).thenAnswer(invocation -> invocation.getArgument(0));

        personaService.setAsDefault(personaId);

        assertFalse(existingDefault.getIsDefault());
        assertTrue(testPersona.getIsDefault());
        verify(personaRepository, times(2)).save(any(Persona.class));
    }

    @Test
    void setAsDefault_noAgentType_throws() {
        Persona noAgentType = Persona.builder()
                .name("No Agent").agentType(null).contentMd("# md")
                .scope(Scope.TEAM).team(testTeam).createdBy(testUser).isDefault(false).version(1).build();
        noAgentType.setId(UUID.randomUUID());

        when(personaRepository.findById(noAgentType.getId())).thenReturn(Optional.of(noAgentType));

        assertThrows(IllegalArgumentException.class,
                () -> personaService.setAsDefault(noAgentType.getId()));
    }

    @Test
    void setAsDefault_noTeam_throws() {
        Persona noTeam = Persona.builder()
                .name("No Team").agentType(AgentType.SECURITY).contentMd("# md")
                .scope(Scope.USER).team(null).createdBy(testUser).isDefault(false).version(1).build();
        noTeam.setId(UUID.randomUUID());

        when(personaRepository.findById(noTeam.getId())).thenReturn(Optional.of(noTeam));

        assertThrows(IllegalArgumentException.class,
                () -> personaService.setAsDefault(noTeam.getId()));
    }

    @Test
    void setAsDefault_notAdmin_throws() {
        TeamMember member = TeamMember.builder()
                .team(testTeam).user(testUser).role(TeamRole.MEMBER).joinedAt(Instant.now()).build();

        when(personaRepository.findById(personaId)).thenReturn(Optional.of(testPersona));
        when(teamMemberRepository.findByTeamIdAndUserId(teamId, userId))
                .thenReturn(Optional.of(member));

        assertThrows(AccessDeniedException.class,
                () -> personaService.setAsDefault(personaId));
    }

    @Test
    void setAsDefault_notFound_throws() {
        when(personaRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> personaService.setAsDefault(UUID.randomUUID()));
    }

    // ── removeDefault ────────────────────────────────────────────────

    @Test
    void removeDefault_withTeam_success() {
        testPersona.setIsDefault(true);

        when(personaRepository.findById(personaId)).thenReturn(Optional.of(testPersona));
        when(teamMemberRepository.findByTeamIdAndUserId(teamId, userId))
                .thenReturn(Optional.of(adminMember));
        when(personaRepository.save(any(Persona.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PersonaResponse response = personaService.removeDefault(personaId);

        assertFalse(response.isDefault());
        assertFalse(testPersona.getIsDefault());
    }

    @Test
    void removeDefault_withoutTeam_noAdminCheck() {
        Persona noTeam = Persona.builder()
                .name("No Team").agentType(AgentType.SECURITY).contentMd("# md")
                .scope(Scope.USER).team(null).createdBy(testUser).isDefault(true).version(1).build();
        noTeam.setId(UUID.randomUUID());
        noTeam.setCreatedAt(Instant.now());
        noTeam.setUpdatedAt(Instant.now());

        when(personaRepository.findById(noTeam.getId())).thenReturn(Optional.of(noTeam));
        when(personaRepository.save(any(Persona.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PersonaResponse response = personaService.removeDefault(noTeam.getId());

        assertFalse(response.isDefault());
        verify(teamMemberRepository, never()).findByTeamIdAndUserId(any(), any());
    }

    @Test
    void removeDefault_notFound_throws() {
        when(personaRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> personaService.removeDefault(UUID.randomUUID()));
    }

    @Test
    void removeDefault_notAdmin_throws() {
        TeamMember member = TeamMember.builder()
                .team(testTeam).user(testUser).role(TeamRole.MEMBER).joinedAt(Instant.now()).build();

        when(personaRepository.findById(personaId)).thenReturn(Optional.of(testPersona));
        when(teamMemberRepository.findByTeamIdAndUserId(teamId, userId))
                .thenReturn(Optional.of(member));

        assertThrows(AccessDeniedException.class,
                () -> personaService.removeDefault(personaId));
    }

    // ── helper ───────────────────────────────────────────────────────

    private void setSecurityContext(UUID userId) {
        var auth = new UsernamePasswordAuthenticationToken(userId, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
