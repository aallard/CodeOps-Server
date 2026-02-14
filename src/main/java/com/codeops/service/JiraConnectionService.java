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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class JiraConnectionService {

    private final JiraConnectionRepository jiraConnectionRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final EncryptionService encryptionService;
    private final TeamRepository teamRepository;
    private final UserRepository userRepository;

    public JiraConnectionResponse createConnection(UUID teamId, CreateJiraConnectionRequest request) {
        verifyTeamAdmin(teamId);

        String encryptedToken = encryptionService.encrypt(request.apiToken());

        JiraConnection connection = JiraConnection.builder()
                .team(teamRepository.getReferenceById(teamId))
                .name(request.name())
                .instanceUrl(request.instanceUrl())
                .email(request.email())
                .encryptedApiToken(encryptedToken)
                .isActive(true)
                .createdBy(userRepository.getReferenceById(SecurityUtils.getCurrentUserId()))
                .build();
        connection = jiraConnectionRepository.save(connection);

        return mapToResponse(connection);
    }

    @Transactional(readOnly = true)
    public List<JiraConnectionResponse> getConnections(UUID teamId) {
        verifyTeamMembership(teamId);
        return jiraConnectionRepository.findByTeamIdAndIsActiveTrue(teamId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public JiraConnectionResponse getConnection(UUID connectionId) {
        JiraConnection connection = jiraConnectionRepository.findById(connectionId)
                .orElseThrow(() -> new EntityNotFoundException("Jira connection not found"));
        verifyTeamMembership(connection.getTeam().getId());
        return mapToResponse(connection);
    }

    public void deleteConnection(UUID connectionId) {
        JiraConnection connection = jiraConnectionRepository.findById(connectionId)
                .orElseThrow(() -> new EntityNotFoundException("Jira connection not found"));
        verifyTeamAdmin(connection.getTeam().getId());
        connection.setIsActive(false);
        jiraConnectionRepository.save(connection);
    }

    public String getDecryptedApiToken(UUID connectionId) {
        JiraConnection connection = jiraConnectionRepository.findById(connectionId)
                .orElseThrow(() -> new EntityNotFoundException("Jira connection not found"));
        return encryptionService.decrypt(connection.getEncryptedApiToken());
    }

    public JiraConnectionDetails getConnectionDetails(UUID connectionId) {
        JiraConnection connection = jiraConnectionRepository.findById(connectionId)
                .orElseThrow(() -> new EntityNotFoundException("Jira connection not found"));
        String decryptedToken = encryptionService.decrypt(connection.getEncryptedApiToken());
        return new JiraConnectionDetails(connection.getInstanceUrl(), connection.getEmail(), decryptedToken);
    }

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
