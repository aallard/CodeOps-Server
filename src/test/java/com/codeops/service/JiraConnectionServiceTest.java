package com.codeops.service;

import com.codeops.dto.request.CreateJiraConnectionRequest;
import com.codeops.dto.response.JiraConnectionResponse;
import com.codeops.entity.JiraConnection;
import com.codeops.entity.Team;
import com.codeops.entity.TeamMember;
import com.codeops.entity.User;
import com.codeops.entity.enums.TeamRole;
import com.codeops.repository.JiraConnectionRepository;
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
class JiraConnectionServiceTest {

    @Mock private JiraConnectionRepository jiraConnectionRepository;
    @Mock private TeamMemberRepository teamMemberRepository;
    @Mock private EncryptionService encryptionService;
    @Mock private TeamRepository teamRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private JiraConnectionService jiraConnectionService;

    private UUID userId;
    private UUID teamId;
    private UUID connectionId;
    private User testUser;
    private Team testTeam;
    private TeamMember adminMember;
    private TeamMember regularMember;
    private JiraConnection testConnection;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        teamId = UUID.randomUUID();
        connectionId = UUID.randomUUID();

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

        adminMember = TeamMember.builder()
                .team(testTeam)
                .user(testUser)
                .role(TeamRole.ADMIN)
                .joinedAt(Instant.now())
                .build();
        adminMember.setId(UUID.randomUUID());

        regularMember = TeamMember.builder()
                .team(testTeam)
                .user(testUser)
                .role(TeamRole.MEMBER)
                .joinedAt(Instant.now())
                .build();
        regularMember.setId(UUID.randomUUID());

        testConnection = JiraConnection.builder()
                .team(testTeam)
                .name("My Jira")
                .instanceUrl("https://mycompany.atlassian.net")
                .email("jira@codeops.dev")
                .encryptedApiToken("encrypted-token")
                .isActive(true)
                .createdBy(testUser)
                .build();
        testConnection.setId(connectionId);
        testConnection.setCreatedAt(Instant.now());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // --- createConnection ---

    @Test
    void createConnection_asAdmin_success() {
        setSecurityContext(userId);
        CreateJiraConnectionRequest request = new CreateJiraConnectionRequest(
                "My Jira", "https://mycompany.atlassian.net", "jira@codeops.dev", "token123");

        when(teamMemberRepository.findByTeamIdAndUserId(teamId, userId)).thenReturn(Optional.of(adminMember));
        when(encryptionService.encrypt("token123")).thenReturn("encrypted-token");
        when(teamRepository.findById(teamId)).thenReturn(Optional.of(testTeam));
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(jiraConnectionRepository.save(any(JiraConnection.class))).thenAnswer(invocation -> {
            JiraConnection saved = invocation.getArgument(0);
            saved.setId(connectionId);
            saved.setCreatedAt(Instant.now());
            return saved;
        });

        JiraConnectionResponse response = jiraConnectionService.createConnection(teamId, request);

        assertNotNull(response);
        assertEquals(connectionId, response.id());
        assertEquals(teamId, response.teamId());
        assertEquals("My Jira", response.name());
        assertEquals("https://mycompany.atlassian.net", response.instanceUrl());
        assertEquals("jira@codeops.dev", response.email());
        assertTrue(response.isActive());
        verify(encryptionService).encrypt("token123");
        verify(jiraConnectionRepository).save(any(JiraConnection.class));
    }

    @Test
    void createConnection_asOwner_success() {
        setSecurityContext(userId);
        TeamMember ownerMember = TeamMember.builder()
                .team(testTeam).user(testUser).role(TeamRole.OWNER).joinedAt(Instant.now()).build();
        ownerMember.setId(UUID.randomUUID());

        CreateJiraConnectionRequest request = new CreateJiraConnectionRequest(
                "Jira Cloud", "https://example.atlassian.net", "admin@example.com", "api-token");

        when(teamMemberRepository.findByTeamIdAndUserId(teamId, userId)).thenReturn(Optional.of(ownerMember));
        when(encryptionService.encrypt("api-token")).thenReturn("encrypted");
        when(teamRepository.findById(teamId)).thenReturn(Optional.of(testTeam));
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(jiraConnectionRepository.save(any(JiraConnection.class))).thenAnswer(invocation -> {
            JiraConnection saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            saved.setCreatedAt(Instant.now());
            return saved;
        });

        JiraConnectionResponse response = jiraConnectionService.createConnection(teamId, request);
        assertNotNull(response);
        assertEquals("Jira Cloud", response.name());
    }

    @Test
    void createConnection_asMember_throwsAccessDenied() {
        setSecurityContext(userId);
        when(teamMemberRepository.findByTeamIdAndUserId(teamId, userId)).thenReturn(Optional.of(regularMember));

        CreateJiraConnectionRequest request = new CreateJiraConnectionRequest(
                "Jira", "https://example.atlassian.net", "user@example.com", "token");

        assertThrows(AccessDeniedException.class, () ->
                jiraConnectionService.createConnection(teamId, request));
        verify(jiraConnectionRepository, never()).save(any());
    }

    @Test
    void createConnection_notAMember_throwsAccessDenied() {
        setSecurityContext(userId);
        when(teamMemberRepository.findByTeamIdAndUserId(teamId, userId)).thenReturn(Optional.empty());

        CreateJiraConnectionRequest request = new CreateJiraConnectionRequest(
                "Jira", "https://example.atlassian.net", "user@example.com", "token");

        assertThrows(AccessDeniedException.class, () ->
                jiraConnectionService.createConnection(teamId, request));
    }

    @Test
    void createConnection_teamNotFound_throwsEntityNotFound() {
        setSecurityContext(userId);
        when(teamMemberRepository.findByTeamIdAndUserId(teamId, userId)).thenReturn(Optional.of(adminMember));
        when(encryptionService.encrypt(any())).thenReturn("encrypted");
        when(teamRepository.findById(teamId)).thenReturn(Optional.empty());

        CreateJiraConnectionRequest request = new CreateJiraConnectionRequest(
                "Jira", "https://example.atlassian.net", "user@example.com", "token");

        assertThrows(EntityNotFoundException.class, () ->
                jiraConnectionService.createConnection(teamId, request));
    }

    @Test
    void createConnection_userNotFound_throwsEntityNotFound() {
        setSecurityContext(userId);
        when(teamMemberRepository.findByTeamIdAndUserId(teamId, userId)).thenReturn(Optional.of(adminMember));
        when(encryptionService.encrypt(any())).thenReturn("encrypted");
        when(teamRepository.findById(teamId)).thenReturn(Optional.of(testTeam));
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        CreateJiraConnectionRequest request = new CreateJiraConnectionRequest(
                "Jira", "https://example.atlassian.net", "user@example.com", "token");

        assertThrows(EntityNotFoundException.class, () ->
                jiraConnectionService.createConnection(teamId, request));
    }

    // --- getConnections ---

    @Test
    void getConnections_asMember_success() {
        setSecurityContext(userId);
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(true);
        when(jiraConnectionRepository.findByTeamIdAndIsActiveTrue(teamId))
                .thenReturn(List.of(testConnection));

        List<JiraConnectionResponse> result = jiraConnectionService.getConnections(teamId);

        assertEquals(1, result.size());
        assertEquals(connectionId, result.get(0).id());
        assertEquals("My Jira", result.get(0).name());
        assertEquals("https://mycompany.atlassian.net", result.get(0).instanceUrl());
    }

    @Test
    void getConnections_emptyList() {
        setSecurityContext(userId);
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(true);
        when(jiraConnectionRepository.findByTeamIdAndIsActiveTrue(teamId)).thenReturn(List.of());

        List<JiraConnectionResponse> result = jiraConnectionService.getConnections(teamId);
        assertTrue(result.isEmpty());
    }

    @Test
    void getConnections_notAMember_throwsAccessDenied() {
        setSecurityContext(userId);
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(false);

        assertThrows(AccessDeniedException.class, () ->
                jiraConnectionService.getConnections(teamId));
    }

    // --- getConnection ---

    @Test
    void getConnection_asMember_success() {
        setSecurityContext(userId);
        when(jiraConnectionRepository.findById(connectionId)).thenReturn(Optional.of(testConnection));
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(true);

        JiraConnectionResponse response = jiraConnectionService.getConnection(connectionId);

        assertEquals(connectionId, response.id());
        assertEquals("My Jira", response.name());
    }

    @Test
    void getConnection_connectionNotFound_throwsEntityNotFound() {
        when(jiraConnectionRepository.findById(connectionId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () ->
                jiraConnectionService.getConnection(connectionId));
    }

    @Test
    void getConnection_notAMember_throwsAccessDenied() {
        setSecurityContext(userId);
        when(jiraConnectionRepository.findById(connectionId)).thenReturn(Optional.of(testConnection));
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(false);

        assertThrows(AccessDeniedException.class, () ->
                jiraConnectionService.getConnection(connectionId));
    }

    // --- deleteConnection ---

    @Test
    void deleteConnection_asAdmin_success() {
        setSecurityContext(userId);
        when(jiraConnectionRepository.findById(connectionId)).thenReturn(Optional.of(testConnection));
        when(teamMemberRepository.findByTeamIdAndUserId(teamId, userId)).thenReturn(Optional.of(adminMember));
        when(jiraConnectionRepository.save(any(JiraConnection.class))).thenReturn(testConnection);

        jiraConnectionService.deleteConnection(connectionId);

        assertFalse(testConnection.getIsActive());
        verify(jiraConnectionRepository).save(testConnection);
    }

    @Test
    void deleteConnection_connectionNotFound_throwsEntityNotFound() {
        when(jiraConnectionRepository.findById(connectionId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () ->
                jiraConnectionService.deleteConnection(connectionId));
    }

    @Test
    void deleteConnection_asMember_throwsAccessDenied() {
        setSecurityContext(userId);
        when(jiraConnectionRepository.findById(connectionId)).thenReturn(Optional.of(testConnection));
        when(teamMemberRepository.findByTeamIdAndUserId(teamId, userId)).thenReturn(Optional.of(regularMember));

        assertThrows(AccessDeniedException.class, () ->
                jiraConnectionService.deleteConnection(connectionId));
        verify(jiraConnectionRepository, never()).save(any());
    }

    // --- getDecryptedApiToken ---

    @Test
    void getDecryptedApiToken_asAdmin_success() {
        setSecurityContext(userId);
        when(jiraConnectionRepository.findById(connectionId)).thenReturn(Optional.of(testConnection));
        when(teamMemberRepository.findByTeamIdAndUserId(teamId, userId)).thenReturn(Optional.of(adminMember));
        when(encryptionService.decrypt("encrypted-token")).thenReturn("token123");

        String result = jiraConnectionService.getDecryptedApiToken(connectionId);

        assertEquals("token123", result);
        verify(encryptionService).decrypt("encrypted-token");
    }

    @Test
    void getDecryptedApiToken_asOwner_success() {
        setSecurityContext(userId);
        TeamMember ownerMember = TeamMember.builder()
                .team(testTeam).user(testUser).role(TeamRole.OWNER).joinedAt(Instant.now()).build();
        ownerMember.setId(UUID.randomUUID());

        when(jiraConnectionRepository.findById(connectionId)).thenReturn(Optional.of(testConnection));
        when(teamMemberRepository.findByTeamIdAndUserId(teamId, userId)).thenReturn(Optional.of(ownerMember));
        when(encryptionService.decrypt("encrypted-token")).thenReturn("token123");

        String result = jiraConnectionService.getDecryptedApiToken(connectionId);
        assertEquals("token123", result);
    }

    @Test
    void getDecryptedApiToken_asMember_throwsAccessDenied() {
        setSecurityContext(userId);
        when(jiraConnectionRepository.findById(connectionId)).thenReturn(Optional.of(testConnection));
        when(teamMemberRepository.findByTeamIdAndUserId(teamId, userId)).thenReturn(Optional.of(regularMember));

        assertThrows(AccessDeniedException.class, () ->
                jiraConnectionService.getDecryptedApiToken(connectionId));
        verify(encryptionService, never()).decrypt(any());
    }

    @Test
    void getDecryptedApiToken_asViewer_throwsAccessDenied() {
        setSecurityContext(userId);
        TeamMember viewerMember = TeamMember.builder()
                .team(testTeam).user(testUser).role(TeamRole.VIEWER).joinedAt(Instant.now()).build();
        viewerMember.setId(UUID.randomUUID());

        when(jiraConnectionRepository.findById(connectionId)).thenReturn(Optional.of(testConnection));
        when(teamMemberRepository.findByTeamIdAndUserId(teamId, userId)).thenReturn(Optional.of(viewerMember));

        assertThrows(AccessDeniedException.class, () ->
                jiraConnectionService.getDecryptedApiToken(connectionId));
    }

    @Test
    void getDecryptedApiToken_notAMember_throwsAccessDenied() {
        setSecurityContext(userId);
        when(jiraConnectionRepository.findById(connectionId)).thenReturn(Optional.of(testConnection));
        when(teamMemberRepository.findByTeamIdAndUserId(teamId, userId)).thenReturn(Optional.empty());

        assertThrows(AccessDeniedException.class, () ->
                jiraConnectionService.getDecryptedApiToken(connectionId));
    }

    @Test
    void getDecryptedApiToken_connectionNotFound_throwsEntityNotFound() {
        when(jiraConnectionRepository.findById(connectionId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () ->
                jiraConnectionService.getDecryptedApiToken(connectionId));
    }

    // --- getConnectionDetails ---

    @Test
    void getConnectionDetails_asAdmin_success() {
        setSecurityContext(userId);
        when(jiraConnectionRepository.findById(connectionId)).thenReturn(Optional.of(testConnection));
        when(teamMemberRepository.findByTeamIdAndUserId(teamId, userId)).thenReturn(Optional.of(adminMember));
        when(encryptionService.decrypt("encrypted-token")).thenReturn("decrypted-token");

        JiraConnectionService.JiraConnectionDetails details =
                jiraConnectionService.getConnectionDetails(connectionId);

        assertEquals("https://mycompany.atlassian.net", details.instanceUrl());
        assertEquals("jira@codeops.dev", details.email());
        assertEquals("decrypted-token", details.apiToken());
    }

    @Test
    void getConnectionDetails_asOwner_success() {
        setSecurityContext(userId);
        TeamMember ownerMember = TeamMember.builder()
                .team(testTeam).user(testUser).role(TeamRole.OWNER).joinedAt(Instant.now()).build();
        ownerMember.setId(UUID.randomUUID());

        when(jiraConnectionRepository.findById(connectionId)).thenReturn(Optional.of(testConnection));
        when(teamMemberRepository.findByTeamIdAndUserId(teamId, userId)).thenReturn(Optional.of(ownerMember));
        when(encryptionService.decrypt("encrypted-token")).thenReturn("decrypted-token");

        JiraConnectionService.JiraConnectionDetails details =
                jiraConnectionService.getConnectionDetails(connectionId);

        assertNotNull(details);
        assertEquals("decrypted-token", details.apiToken());
    }

    @Test
    void getConnectionDetails_asMember_throwsAccessDenied() {
        setSecurityContext(userId);
        when(jiraConnectionRepository.findById(connectionId)).thenReturn(Optional.of(testConnection));
        when(teamMemberRepository.findByTeamIdAndUserId(teamId, userId)).thenReturn(Optional.of(regularMember));

        assertThrows(AccessDeniedException.class, () ->
                jiraConnectionService.getConnectionDetails(connectionId));
    }

    @Test
    void getConnectionDetails_connectionNotFound_throwsEntityNotFound() {
        when(jiraConnectionRepository.findById(connectionId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () ->
                jiraConnectionService.getConnectionDetails(connectionId));
    }

    @Test
    void getConnectionDetails_notAMember_throwsAccessDenied() {
        setSecurityContext(userId);
        when(jiraConnectionRepository.findById(connectionId)).thenReturn(Optional.of(testConnection));
        when(teamMemberRepository.findByTeamIdAndUserId(teamId, userId)).thenReturn(Optional.empty());

        assertThrows(AccessDeniedException.class, () ->
                jiraConnectionService.getConnectionDetails(connectionId));
    }

    private void setSecurityContext(UUID userId) {
        var auth = new UsernamePasswordAuthenticationToken(userId, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
