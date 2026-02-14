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

@Service
@RequiredArgsConstructor
@Transactional
public class GitHubConnectionService {

    private final GitHubConnectionRepository gitHubConnectionRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final EncryptionService encryptionService;
    private final TeamRepository teamRepository;
    private final UserRepository userRepository;

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

    @Transactional(readOnly = true)
    public List<GitHubConnectionResponse> getConnections(UUID teamId) {
        verifyTeamMembership(teamId);
        return gitHubConnectionRepository.findByTeamIdAndIsActiveTrue(teamId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public GitHubConnectionResponse getConnection(UUID connectionId) {
        GitHubConnection connection = gitHubConnectionRepository.findById(connectionId)
                .orElseThrow(() -> new EntityNotFoundException("GitHub connection not found"));
        verifyTeamMembership(connection.getTeam().getId());
        return mapToResponse(connection);
    }

    public void deleteConnection(UUID connectionId) {
        GitHubConnection connection = gitHubConnectionRepository.findById(connectionId)
                .orElseThrow(() -> new EntityNotFoundException("GitHub connection not found"));
        verifyTeamAdmin(connection.getTeam().getId());
        connection.setIsActive(false);
        gitHubConnectionRepository.save(connection);
    }

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
