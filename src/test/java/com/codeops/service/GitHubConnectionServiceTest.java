package com.codeops.service;

import com.codeops.dto.request.CreateGitHubConnectionRequest;
import com.codeops.dto.response.GitHubConnectionResponse;
import com.codeops.entity.GitHubConnection;
import com.codeops.entity.Team;
import com.codeops.entity.TeamMember;
import com.codeops.entity.User;
import com.codeops.entity.enums.GitHubAuthType;
import com.codeops.entity.enums.TeamRole;
import com.codeops.repository.GitHubConnectionRepository;
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
class GitHubConnectionServiceTest {

    @Mock private GitHubConnectionRepository gitHubConnectionRepository;
    @Mock private TeamMemberRepository teamMemberRepository;
    @Mock private EncryptionService encryptionService;
    @Mock private TeamRepository teamRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private GitHubConnectionService gitHubConnectionService;

    private UUID userId;
    private UUID teamId;
    private UUID connectionId;
    private User testUser;
    private Team testTeam;
    private TeamMember adminMember;
    private TeamMember regularMember;
    private GitHubConnection testConnection;

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

        testConnection = GitHubConnection.builder()
                .team(testTeam)
                .name("My GitHub PAT")
                .authType(GitHubAuthType.PAT)
                .encryptedCredentials("encrypted-creds")
                .githubUsername("testuser")
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
        CreateGitHubConnectionRequest request = new CreateGitHubConnectionRequest(
                "My PAT", GitHubAuthType.PAT, "ghp_abc123", "testuser");

        when(teamMemberRepository.findByTeamIdAndUserId(teamId, userId)).thenReturn(Optional.of(adminMember));
        when(encryptionService.encrypt("ghp_abc123")).thenReturn("encrypted-value");
        when(teamRepository.findById(teamId)).thenReturn(Optional.of(testTeam));
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(gitHubConnectionRepository.save(any(GitHubConnection.class))).thenAnswer(invocation -> {
            GitHubConnection saved = invocation.getArgument(0);
            saved.setId(connectionId);
            saved.setCreatedAt(Instant.now());
            return saved;
        });

        GitHubConnectionResponse response = gitHubConnectionService.createConnection(teamId, request);

        assertNotNull(response);
        assertEquals(connectionId, response.id());
        assertEquals(teamId, response.teamId());
        assertEquals("My PAT", response.name());
        assertEquals(GitHubAuthType.PAT, response.authType());
        assertEquals("testuser", response.githubUsername());
        assertTrue(response.isActive());
        verify(encryptionService).encrypt("ghp_abc123");
        verify(gitHubConnectionRepository).save(any(GitHubConnection.class));
    }

    @Test
    void createConnection_asOwner_success() {
        setSecurityContext(userId);
        TeamMember ownerMember = TeamMember.builder()
                .team(testTeam).user(testUser).role(TeamRole.OWNER).joinedAt(Instant.now()).build();
        ownerMember.setId(UUID.randomUUID());

        CreateGitHubConnectionRequest request = new CreateGitHubConnectionRequest(
                "SSH Key", GitHubAuthType.SSH, "ssh-rsa AAAA...", null);

        when(teamMemberRepository.findByTeamIdAndUserId(teamId, userId)).thenReturn(Optional.of(ownerMember));
        when(encryptionService.encrypt("ssh-rsa AAAA...")).thenReturn("encrypted-ssh");
        when(teamRepository.findById(teamId)).thenReturn(Optional.of(testTeam));
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(gitHubConnectionRepository.save(any(GitHubConnection.class))).thenAnswer(invocation -> {
            GitHubConnection saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            saved.setCreatedAt(Instant.now());
            return saved;
        });

        GitHubConnectionResponse response = gitHubConnectionService.createConnection(teamId, request);
        assertNotNull(response);
        assertEquals(GitHubAuthType.SSH, response.authType());
    }

    @Test
    void createConnection_asMember_throwsAccessDenied() {
        setSecurityContext(userId);
        when(teamMemberRepository.findByTeamIdAndUserId(teamId, userId)).thenReturn(Optional.of(regularMember));

        CreateGitHubConnectionRequest request = new CreateGitHubConnectionRequest(
                "PAT", GitHubAuthType.PAT, "creds", "user");

        assertThrows(AccessDeniedException.class, () ->
                gitHubConnectionService.createConnection(teamId, request));
        verify(gitHubConnectionRepository, never()).save(any());
    }

    @Test
    void createConnection_notAMember_throwsAccessDenied() {
        setSecurityContext(userId);
        when(teamMemberRepository.findByTeamIdAndUserId(teamId, userId)).thenReturn(Optional.empty());

        CreateGitHubConnectionRequest request = new CreateGitHubConnectionRequest(
                "PAT", GitHubAuthType.PAT, "creds", "user");

        assertThrows(AccessDeniedException.class, () ->
                gitHubConnectionService.createConnection(teamId, request));
    }

    @Test
    void createConnection_teamNotFound_throwsEntityNotFound() {
        setSecurityContext(userId);
        when(teamMemberRepository.findByTeamIdAndUserId(teamId, userId)).thenReturn(Optional.of(adminMember));
        when(encryptionService.encrypt(any())).thenReturn("encrypted");
        when(teamRepository.findById(teamId)).thenReturn(Optional.empty());

        CreateGitHubConnectionRequest request = new CreateGitHubConnectionRequest(
                "PAT", GitHubAuthType.PAT, "creds", "user");

        assertThrows(EntityNotFoundException.class, () ->
                gitHubConnectionService.createConnection(teamId, request));
    }

    @Test
    void createConnection_userNotFound_throwsEntityNotFound() {
        setSecurityContext(userId);
        when(teamMemberRepository.findByTeamIdAndUserId(teamId, userId)).thenReturn(Optional.of(adminMember));
        when(encryptionService.encrypt(any())).thenReturn("encrypted");
        when(teamRepository.findById(teamId)).thenReturn(Optional.of(testTeam));
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        CreateGitHubConnectionRequest request = new CreateGitHubConnectionRequest(
                "PAT", GitHubAuthType.PAT, "creds", "user");

        assertThrows(EntityNotFoundException.class, () ->
                gitHubConnectionService.createConnection(teamId, request));
    }

    // --- getConnections ---

    @Test
    void getConnections_asMember_success() {
        setSecurityContext(userId);
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(true);
        when(gitHubConnectionRepository.findByTeamIdAndIsActiveTrue(teamId))
                .thenReturn(List.of(testConnection));

        List<GitHubConnectionResponse> result = gitHubConnectionService.getConnections(teamId);

        assertEquals(1, result.size());
        assertEquals(connectionId, result.get(0).id());
        assertEquals("My GitHub PAT", result.get(0).name());
    }

    @Test
    void getConnections_emptyList() {
        setSecurityContext(userId);
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(true);
        when(gitHubConnectionRepository.findByTeamIdAndIsActiveTrue(teamId)).thenReturn(List.of());

        List<GitHubConnectionResponse> result = gitHubConnectionService.getConnections(teamId);
        assertTrue(result.isEmpty());
    }

    @Test
    void getConnections_notAMember_throwsAccessDenied() {
        setSecurityContext(userId);
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(false);

        assertThrows(AccessDeniedException.class, () ->
                gitHubConnectionService.getConnections(teamId));
    }

    // --- getConnection ---

    @Test
    void getConnection_asMember_success() {
        setSecurityContext(userId);
        when(gitHubConnectionRepository.findById(connectionId)).thenReturn(Optional.of(testConnection));
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(true);

        GitHubConnectionResponse response = gitHubConnectionService.getConnection(connectionId);

        assertEquals(connectionId, response.id());
        assertEquals("My GitHub PAT", response.name());
    }

    @Test
    void getConnection_connectionNotFound_throwsEntityNotFound() {
        when(gitHubConnectionRepository.findById(connectionId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () ->
                gitHubConnectionService.getConnection(connectionId));
    }

    @Test
    void getConnection_notAMember_throwsAccessDenied() {
        setSecurityContext(userId);
        when(gitHubConnectionRepository.findById(connectionId)).thenReturn(Optional.of(testConnection));
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(false);

        assertThrows(AccessDeniedException.class, () ->
                gitHubConnectionService.getConnection(connectionId));
    }

    // --- deleteConnection ---

    @Test
    void deleteConnection_asAdmin_success() {
        setSecurityContext(userId);
        when(gitHubConnectionRepository.findById(connectionId)).thenReturn(Optional.of(testConnection));
        when(teamMemberRepository.findByTeamIdAndUserId(teamId, userId)).thenReturn(Optional.of(adminMember));
        when(gitHubConnectionRepository.save(any(GitHubConnection.class))).thenReturn(testConnection);

        gitHubConnectionService.deleteConnection(connectionId);

        assertFalse(testConnection.getIsActive());
        verify(gitHubConnectionRepository).save(testConnection);
    }

    @Test
    void deleteConnection_connectionNotFound_throwsEntityNotFound() {
        when(gitHubConnectionRepository.findById(connectionId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () ->
                gitHubConnectionService.deleteConnection(connectionId));
    }

    @Test
    void deleteConnection_asMember_throwsAccessDenied() {
        setSecurityContext(userId);
        when(gitHubConnectionRepository.findById(connectionId)).thenReturn(Optional.of(testConnection));
        when(teamMemberRepository.findByTeamIdAndUserId(teamId, userId)).thenReturn(Optional.of(regularMember));

        assertThrows(AccessDeniedException.class, () ->
                gitHubConnectionService.deleteConnection(connectionId));
        verify(gitHubConnectionRepository, never()).save(any());
    }

    // --- getDecryptedCredentials ---

    @Test
    void getDecryptedCredentials_asAdmin_success() {
        setSecurityContext(userId);
        when(gitHubConnectionRepository.findById(connectionId)).thenReturn(Optional.of(testConnection));
        when(teamMemberRepository.findByTeamIdAndUserId(teamId, userId)).thenReturn(Optional.of(adminMember));
        when(encryptionService.decrypt("encrypted-creds")).thenReturn("ghp_abc123");

        String result = gitHubConnectionService.getDecryptedCredentials(connectionId);

        assertEquals("ghp_abc123", result);
        verify(encryptionService).decrypt("encrypted-creds");
    }

    @Test
    void getDecryptedCredentials_asOwner_success() {
        setSecurityContext(userId);
        TeamMember ownerMember = TeamMember.builder()
                .team(testTeam).user(testUser).role(TeamRole.OWNER).joinedAt(Instant.now()).build();
        ownerMember.setId(UUID.randomUUID());

        when(gitHubConnectionRepository.findById(connectionId)).thenReturn(Optional.of(testConnection));
        when(teamMemberRepository.findByTeamIdAndUserId(teamId, userId)).thenReturn(Optional.of(ownerMember));
        when(encryptionService.decrypt("encrypted-creds")).thenReturn("ghp_abc123");

        String result = gitHubConnectionService.getDecryptedCredentials(connectionId);
        assertEquals("ghp_abc123", result);
    }

    @Test
    void getDecryptedCredentials_asMember_throwsAccessDenied() {
        setSecurityContext(userId);
        when(gitHubConnectionRepository.findById(connectionId)).thenReturn(Optional.of(testConnection));
        when(teamMemberRepository.findByTeamIdAndUserId(teamId, userId)).thenReturn(Optional.of(regularMember));

        assertThrows(AccessDeniedException.class, () ->
                gitHubConnectionService.getDecryptedCredentials(connectionId));
        verify(encryptionService, never()).decrypt(any());
    }

    @Test
    void getDecryptedCredentials_asViewer_throwsAccessDenied() {
        setSecurityContext(userId);
        TeamMember viewerMember = TeamMember.builder()
                .team(testTeam).user(testUser).role(TeamRole.VIEWER).joinedAt(Instant.now()).build();
        viewerMember.setId(UUID.randomUUID());

        when(gitHubConnectionRepository.findById(connectionId)).thenReturn(Optional.of(testConnection));
        when(teamMemberRepository.findByTeamIdAndUserId(teamId, userId)).thenReturn(Optional.of(viewerMember));

        assertThrows(AccessDeniedException.class, () ->
                gitHubConnectionService.getDecryptedCredentials(connectionId));
    }

    @Test
    void getDecryptedCredentials_notAMember_throwsAccessDenied() {
        setSecurityContext(userId);
        when(gitHubConnectionRepository.findById(connectionId)).thenReturn(Optional.of(testConnection));
        when(teamMemberRepository.findByTeamIdAndUserId(teamId, userId)).thenReturn(Optional.empty());

        assertThrows(AccessDeniedException.class, () ->
                gitHubConnectionService.getDecryptedCredentials(connectionId));
    }

    @Test
    void getDecryptedCredentials_connectionNotFound_throwsEntityNotFound() {
        when(gitHubConnectionRepository.findById(connectionId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () ->
                gitHubConnectionService.getDecryptedCredentials(connectionId));
    }

    private void setSecurityContext(UUID userId) {
        var auth = new UsernamePasswordAuthenticationToken(userId, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
