package com.codeops.service;

import com.codeops.dto.request.CreateGitHubConnectionRequest;
import com.codeops.dto.response.GitHubConnectionResponse;
import com.codeops.entity.GitHubConnection;
import com.codeops.entity.TeamMember;
import com.codeops.entity.enums.TeamRole;
import com.codeops.repository.GitHubConnectionRepository;
import com.codeops.repository.TeamMemberRepository;
import com.codeops.repository.TeamRepository;
import com.codeops.repository.UserRepository;
import com.codeops.security.SecurityUtils;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Manages GitHub connection lifecycle including creation, retrieval, deletion,
 * and credential decryption for teams.
 *
 * <p>All operations enforce team membership or admin/owner role requirements
 * before proceeding. Credentials are encrypted at rest using AES-256-GCM via
 * {@link EncryptionService} and are never returned in standard API responses.</p>
 *
 * @see IntegrationController
 * @see GitHubConnection
 * @see EncryptionService
 */
@Service
@RequiredArgsConstructor
@Transactional
public class GitHubConnectionService {

    private final GitHubConnectionRepository gitHubConnectionRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final EncryptionService encryptionService;
    private final TeamRepository teamRepository;
    private final UserRepository userRepository;

    /**
     * Creates a new GitHub connection for the specified team.
     *
     * <p>Encrypts the provided credentials before persisting. The current user
     * is recorded as the connection creator.</p>
     *
     * @param teamId the ID of the team to associate the connection with
     * @param request the connection creation request containing name, auth type,
     *                credentials, and GitHub username
     * @return the created GitHub connection as a response DTO (credentials excluded)
     * @throws EntityNotFoundException if the team or current user is not found
     * @throws AccessDeniedException if the current user does not have OWNER or ADMIN role on the team
     */
    public GitHubConnectionResponse createConnection(UUID teamId, CreateGitHubConnectionRequest request) {
        verifyTeamAdmin(teamId);

        String encryptedCredentials = encryptionService.encrypt(request.credentials());

        GitHubConnection connection = GitHubConnection.builder()
                .team(teamRepository.findById(teamId).orElseThrow(() -> new EntityNotFoundException("Team not found")))
                .name(request.name())
                .authType(request.authType())
                .encryptedCredentials(encryptedCredentials)
                .githubUsername(request.githubUsername())
                .isActive(true)
                .createdBy(userRepository.findById(SecurityUtils.getCurrentUserId()).orElseThrow(() -> new EntityNotFoundException("User not found")))
                .build();
        connection = gitHubConnectionRepository.save(connection);

        return mapToResponse(connection);
    }

    /**
     * Retrieves all active GitHub connections for the specified team.
     *
     * @param teamId the ID of the team whose connections to retrieve
     * @return a list of active GitHub connection response DTOs
     * @throws AccessDeniedException if the current user is not a member of the team
     */
    @Transactional(readOnly = true)
    public List<GitHubConnectionResponse> getConnections(UUID teamId) {
        verifyTeamMembership(teamId);
        return gitHubConnectionRepository.findByTeamIdAndIsActiveTrue(teamId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    /**
     * Retrieves a single GitHub connection by its ID.
     *
     * @param connectionId the ID of the GitHub connection to retrieve
     * @return the GitHub connection as a response DTO
     * @throws EntityNotFoundException if no GitHub connection exists with the given ID
     * @throws AccessDeniedException if the current user is not a member of the connection's team
     */
    @Transactional(readOnly = true)
    public GitHubConnectionResponse getConnection(UUID connectionId) {
        GitHubConnection connection = gitHubConnectionRepository.findById(connectionId)
                .orElseThrow(() -> new EntityNotFoundException("GitHub connection not found"));
        verifyTeamMembership(connection.getTeam().getId());
        return mapToResponse(connection);
    }

    /**
     * Soft-deletes a GitHub connection by setting its active flag to {@code false}.
     *
     * <p>The connection record is retained in the database but excluded from
     * active connection queries.</p>
     *
     * @param connectionId the ID of the GitHub connection to deactivate
     * @throws EntityNotFoundException if no GitHub connection exists with the given ID
     * @throws AccessDeniedException if the current user does not have OWNER or ADMIN role on the connection's team
     */
    public void deleteConnection(UUID connectionId) {
        GitHubConnection connection = gitHubConnectionRepository.findById(connectionId)
                .orElseThrow(() -> new EntityNotFoundException("GitHub connection not found"));
        verifyTeamAdmin(connection.getTeam().getId());
        connection.setIsActive(false);
        gitHubConnectionRepository.save(connection);
    }

    /**
     * Decrypts and returns the stored credentials for a GitHub connection.
     *
     * <p>Access is restricted to team members with ADMIN or OWNER role.
     * This method should only be used internally for authenticated GitHub API
     * calls and must never expose credentials in API responses.</p>
     *
     * @param connectionId the ID of the GitHub connection whose credentials to decrypt
     * @return the decrypted credential string (e.g., personal access token)
     * @throws EntityNotFoundException if no GitHub connection exists with the given ID
     * @throws AccessDeniedException if the current user is not a team member or lacks ADMIN/OWNER role
     */
    public String getDecryptedCredentials(UUID connectionId) {
        GitHubConnection connection = gitHubConnectionRepository.findById(connectionId)
                .orElseThrow(() -> new EntityNotFoundException("GitHub connection not found"));
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        UUID teamId = connection.getTeam().getId();
        TeamMember member = teamMemberRepository.findByTeamIdAndUserId(teamId, currentUserId)
                .orElseThrow(() -> new AccessDeniedException("Not a member of this team"));
        if (member.getRole() != TeamRole.ADMIN && member.getRole() != TeamRole.OWNER) {
            throw new AccessDeniedException("Only ADMIN or OWNER can access credentials");
        }
        return encryptionService.decrypt(connection.getEncryptedCredentials());
    }

    private GitHubConnectionResponse mapToResponse(GitHubConnection connection) {
        return new GitHubConnectionResponse(
                connection.getId(),
                connection.getTeam().getId(),
                connection.getName(),
                connection.getAuthType(),
                connection.getGithubUsername(),
                connection.getIsActive(),
                connection.getCreatedAt()
        );
    }

    private void verifyTeamMembership(UUID teamId) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        if (!teamMemberRepository.existsByTeamIdAndUserId(teamId, currentUserId)) {
            throw new AccessDeniedException("Not a member of this team");
        }
    }

    private void verifyTeamAdmin(UUID teamId) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        TeamMember member = teamMemberRepository.findByTeamIdAndUserId(teamId, currentUserId)
                .orElseThrow(() -> new AccessDeniedException("Not a member of this team"));
        if (member.getRole() != TeamRole.OWNER && member.getRole() != TeamRole.ADMIN) {
            throw new AccessDeniedException("Requires OWNER or ADMIN role");
        }
    }
}
