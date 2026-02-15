package com.codeops.service;

import com.codeops.dto.request.CreateJiraConnectionRequest;
import com.codeops.dto.response.JiraConnectionResponse;
import com.codeops.entity.JiraConnection;
import com.codeops.entity.TeamMember;
import com.codeops.entity.enums.TeamRole;
import com.codeops.repository.JiraConnectionRepository;
import com.codeops.repository.TeamMemberRepository;
import com.codeops.repository.TeamRepository;
import com.codeops.repository.UserRepository;
import com.codeops.security.SecurityUtils;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Manages Jira connection lifecycle including creation, retrieval, deletion,
 * and credential decryption for teams.
 *
 * <p>All operations enforce team membership or admin/owner role requirements
 * before proceeding. API tokens are encrypted at rest using AES-256-GCM via
 * {@link EncryptionService} and are never returned in standard API responses.</p>
 *
 * @see IntegrationController
 * @see JiraConnection
 * @see EncryptionService
 */
@Service
@RequiredArgsConstructor
@Transactional
public class JiraConnectionService {

    private static final Logger log = LoggerFactory.getLogger(JiraConnectionService.class);

    private final JiraConnectionRepository jiraConnectionRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final EncryptionService encryptionService;
    private final TeamRepository teamRepository;
    private final UserRepository userRepository;

    /**
     * Creates a new Jira connection for the specified team.
     *
     * <p>Encrypts the provided API token before persisting. The current user
     * is recorded as the connection creator.</p>
     *
     * @param teamId the ID of the team to associate the connection with
     * @param request the connection creation request containing name, instance URL,
     *                email, and API token
     * @return the created Jira connection as a response DTO (API token excluded)
     * @throws EntityNotFoundException if the team or current user is not found
     * @throws AccessDeniedException if the current user does not have OWNER or ADMIN role on the team
     */
    public JiraConnectionResponse createConnection(UUID teamId, CreateJiraConnectionRequest request) {
        log.debug("createConnection called with teamId={}, name={}, instanceUrl={}", teamId, request.name(), request.instanceUrl());
        verifyTeamAdmin(teamId);

        String encryptedToken = encryptionService.encrypt(request.apiToken());

        JiraConnection connection = JiraConnection.builder()
                .team(teamRepository.findById(teamId).orElseThrow(() -> new EntityNotFoundException("Team not found")))
                .name(request.name())
                .instanceUrl(request.instanceUrl())
                .email(request.email())
                .encryptedApiToken(encryptedToken)
                .isActive(true)
                .createdBy(userRepository.findById(SecurityUtils.getCurrentUserId()).orElseThrow(() -> new EntityNotFoundException("User not found")))
                .build();
        connection = jiraConnectionRepository.save(connection);
        log.info("Created Jira connection id={} for teamId={}, name={}", connection.getId(), teamId, request.name());

        return mapToResponse(connection);
    }

    /**
     * Retrieves all active Jira connections for the specified team.
     *
     * @param teamId the ID of the team whose connections to retrieve
     * @return a list of active Jira connection response DTOs
     * @throws AccessDeniedException if the current user is not a member of the team
     */
    @Transactional(readOnly = true)
    public List<JiraConnectionResponse> getConnections(UUID teamId) {
        log.debug("getConnections called with teamId={}", teamId);
        verifyTeamMembership(teamId);
        return jiraConnectionRepository.findByTeamIdAndIsActiveTrue(teamId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    /**
     * Retrieves a single Jira connection by its ID.
     *
     * @param connectionId the ID of the Jira connection to retrieve
     * @return the Jira connection as a response DTO
     * @throws EntityNotFoundException if no Jira connection exists with the given ID
     * @throws AccessDeniedException if the current user is not a member of the connection's team
     */
    @Transactional(readOnly = true)
    public JiraConnectionResponse getConnection(UUID connectionId) {
        log.debug("getConnection called with connectionId={}", connectionId);
        JiraConnection connection = jiraConnectionRepository.findById(connectionId)
                .orElseThrow(() -> new EntityNotFoundException("Jira connection not found"));
        verifyTeamMembership(connection.getTeam().getId());
        return mapToResponse(connection);
    }

    /**
     * Soft-deletes a Jira connection by setting its active flag to {@code false}.
     *
     * <p>The connection record is retained in the database but excluded from
     * active connection queries.</p>
     *
     * @param connectionId the ID of the Jira connection to deactivate
     * @throws EntityNotFoundException if no Jira connection exists with the given ID
     * @throws AccessDeniedException if the current user does not have OWNER or ADMIN role on the connection's team
     */
    public void deleteConnection(UUID connectionId) {
        log.debug("deleteConnection called with connectionId={}", connectionId);
        JiraConnection connection = jiraConnectionRepository.findById(connectionId)
                .orElseThrow(() -> new EntityNotFoundException("Jira connection not found"));
        verifyTeamAdmin(connection.getTeam().getId());
        connection.setIsActive(false);
        jiraConnectionRepository.save(connection);
        log.info("Soft-deleted Jira connection id={}", connectionId);
    }

    /**
     * Decrypts and returns the stored API token for a Jira connection.
     *
     * <p>Access is restricted to team members with ADMIN or OWNER role.
     * This method should only be used internally for authenticated Jira API
     * calls and must never expose the token in API responses.</p>
     *
     * @param connectionId the ID of the Jira connection whose API token to decrypt
     * @return the decrypted API token string
     * @throws EntityNotFoundException if no Jira connection exists with the given ID
     * @throws AccessDeniedException if the current user is not a team member or lacks ADMIN/OWNER role
     */
    public String getDecryptedApiToken(UUID connectionId) {
        log.debug("getDecryptedApiToken called with connectionId={}", connectionId);
        JiraConnection connection = jiraConnectionRepository.findById(connectionId)
                .orElseThrow(() -> new EntityNotFoundException("Jira connection not found"));
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        UUID teamId = connection.getTeam().getId();
        TeamMember member = teamMemberRepository.findByTeamIdAndUserId(teamId, currentUserId)
                .orElseThrow(() -> new AccessDeniedException("Not a member of this team"));
        if (member.getRole() != TeamRole.ADMIN && member.getRole() != TeamRole.OWNER) {
            throw new AccessDeniedException("Only ADMIN or OWNER can access credentials");
        }
        return encryptionService.decrypt(connection.getEncryptedApiToken());
    }

    /**
     * Retrieves the full connection details for a Jira connection, including
     * the decrypted API token, instance URL, and email.
     *
     * <p>Access is restricted to team members with ADMIN or OWNER role.
     * Intended for internal use when making authenticated Jira API calls.</p>
     *
     * @param connectionId the ID of the Jira connection whose details to retrieve
     * @return a {@link JiraConnectionDetails} record containing instance URL, email, and decrypted API token
     * @throws EntityNotFoundException if no Jira connection exists with the given ID
     * @throws AccessDeniedException if the current user is not a team member or lacks ADMIN/OWNER role
     */
    public JiraConnectionDetails getConnectionDetails(UUID connectionId) {
        log.debug("getConnectionDetails called with connectionId={}", connectionId);
        JiraConnection connection = jiraConnectionRepository.findById(connectionId)
                .orElseThrow(() -> new EntityNotFoundException("Jira connection not found"));
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        UUID teamId = connection.getTeam().getId();
        TeamMember member = teamMemberRepository.findByTeamIdAndUserId(teamId, currentUserId)
                .orElseThrow(() -> new AccessDeniedException("Not a member of this team"));
        if (member.getRole() != TeamRole.ADMIN && member.getRole() != TeamRole.OWNER) {
            throw new AccessDeniedException("Only ADMIN or OWNER can access credentials");
        }
        String decryptedToken = encryptionService.decrypt(connection.getEncryptedApiToken());
        return new JiraConnectionDetails(connection.getInstanceUrl(), connection.getEmail(), decryptedToken);
    }

    /**
     * Holds decrypted Jira connection details needed for API authentication.
     *
     * @param instanceUrl the base URL of the Jira instance (e.g., {@code https://myorg.atlassian.net})
     * @param email the email address associated with the Jira API token
     * @param apiToken the decrypted Jira API token
     */
    public record JiraConnectionDetails(String instanceUrl, String email, String apiToken) {}

    private JiraConnectionResponse mapToResponse(JiraConnection connection) {
        return new JiraConnectionResponse(
                connection.getId(),
                connection.getTeam().getId(),
                connection.getName(),
                connection.getInstanceUrl(),
                connection.getEmail(),
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
