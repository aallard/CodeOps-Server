package com.codeops.service;

import com.codeops.config.AppConstants;
import com.codeops.dto.request.CreateTeamRequest;
import com.codeops.dto.request.InviteMemberRequest;
import com.codeops.dto.request.UpdateMemberRoleRequest;
import com.codeops.dto.request.UpdateTeamRequest;
import com.codeops.dto.response.InvitationResponse;
import com.codeops.dto.response.TeamMemberResponse;
import com.codeops.dto.response.TeamResponse;
import com.codeops.entity.Invitation;
import com.codeops.entity.Team;
import com.codeops.entity.TeamMember;
import com.codeops.entity.User;
import com.codeops.entity.enums.InvitationStatus;
import com.codeops.entity.enums.TeamRole;
import com.codeops.repository.InvitationRepository;
import com.codeops.repository.TeamMemberRepository;
import com.codeops.repository.TeamRepository;
import com.codeops.repository.UserRepository;
import com.codeops.security.SecurityUtils;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class TeamService {

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final UserRepository userRepository;
    private final InvitationRepository invitationRepository;

    public TeamResponse createTeam(CreateTeamRequest request) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        User currentUser = userRepository.getReferenceById(currentUserId);

        Team team = Team.builder()
                .name(request.name())
                .description(request.description())
                .owner(currentUser)
                .teamsWebhookUrl(request.teamsWebhookUrl())
                .build();
        team = teamRepository.save(team);

        TeamMember member = TeamMember.builder()
                .team(team)
                .user(currentUser)
                .role(TeamRole.OWNER)
                .joinedAt(Instant.now())
                .build();
        teamMemberRepository.save(member);

        return mapToTeamResponse(team);
    }

    @Transactional(readOnly = true)
    public TeamResponse getTeam(UUID teamId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new EntityNotFoundException("Team not found"));
        verifyTeamMembership(teamId);
        return mapToTeamResponse(team);
    }

    @Transactional(readOnly = true)
    public List<TeamResponse> getTeamsForUser() {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        return teamMemberRepository.findByUserId(currentUserId).stream()
                .map(member -> mapToTeamResponse(member.getTeam()))
                .toList();
    }

    public TeamResponse updateTeam(UUID teamId, UpdateTeamRequest request) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new EntityNotFoundException("Team not found"));
        verifyTeamAdmin(teamId);

        if (request.name() != null) {
            team.setName(request.name());
        }
        if (request.description() != null) {
            team.setDescription(request.description());
        }
        if (request.teamsWebhookUrl() != null) {
            team.setTeamsWebhookUrl(request.teamsWebhookUrl());
        }

        team = teamRepository.save(team);
        return mapToTeamResponse(team);
    }

    public void deleteTeam(UUID teamId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new EntityNotFoundException("Team not found"));

        TeamRole currentRole = getCurrentUserTeamRole(teamId);
        if (currentRole != TeamRole.OWNER) {
            throw new AccessDeniedException("Only the team owner can delete the team");
        }

        teamRepository.delete(team);
    }

    @Transactional(readOnly = true)
    public List<TeamMemberResponse> getTeamMembers(UUID teamId) {
        verifyTeamMembership(teamId);
        return teamMemberRepository.findByTeamId(teamId).stream()
                .map(member -> mapToTeamMemberResponse(member, member.getUser()))
                .toList();
    }

    public TeamMemberResponse updateMemberRole(UUID teamId, UUID userId, UpdateMemberRoleRequest request) {
        verifyTeamAdmin(teamId);

        TeamMember member = teamMemberRepository.findByTeamIdAndUserId(teamId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Team member not found"));

        if (member.getRole() == TeamRole.OWNER && request.role() != TeamRole.OWNER) {
            throw new IllegalArgumentException("Cannot change the owner's role directly");
        }

        if (request.role() == TeamRole.OWNER) {
            TeamRole currentRole = getCurrentUserTeamRole(teamId);
            if (currentRole != TeamRole.OWNER) {
                throw new AccessDeniedException("Only the current owner can transfer ownership");
            }
            UUID currentUserId = SecurityUtils.getCurrentUserId();
            TeamMember currentOwnerMember = teamMemberRepository.findByTeamIdAndUserId(teamId, currentUserId)
                    .orElseThrow(() -> new EntityNotFoundException("Current user member not found"));
            currentOwnerMember.setRole(TeamRole.ADMIN);
            teamMemberRepository.save(currentOwnerMember);

            Team team = teamRepository.findById(teamId)
                    .orElseThrow(() -> new EntityNotFoundException("Team not found"));
            team.setOwner(member.getUser());
            teamRepository.save(team);
        }

        member.setRole(request.role());
        member = teamMemberRepository.save(member);
        return mapToTeamMemberResponse(member, member.getUser());
    }

    public void removeMember(UUID teamId, UUID userId) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        boolean isSelfRemoval = currentUserId.equals(userId);

        if (!isSelfRemoval) {
            verifyTeamAdmin(teamId);
        }

        TeamMember member = teamMemberRepository.findByTeamIdAndUserId(teamId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Team member not found"));

        if (member.getRole() == TeamRole.OWNER) {
            throw new IllegalArgumentException("Cannot remove the team owner");
        }

        teamMemberRepository.delete(member);
    }

    public InvitationResponse inviteMember(UUID teamId, InviteMemberRequest request) {
        verifyTeamAdmin(teamId);

        long memberCount = teamMemberRepository.countByTeamId(teamId);
        if (memberCount >= AppConstants.MAX_TEAM_MEMBERS) {
            throw new IllegalArgumentException("Team has reached the maximum number of members");
        }

        User existingUser = userRepository.findByEmail(request.email()).orElse(null);
        if (existingUser != null && teamMemberRepository.existsByTeamIdAndUserId(teamId, existingUser.getId())) {
            throw new IllegalArgumentException("User is already a member of this team");
        }

        List<Invitation> pendingInvitations = invitationRepository.findByEmailAndStatus(request.email(), InvitationStatus.PENDING);
        boolean hasPendingForTeam = pendingInvitations.stream()
                .anyMatch(inv -> inv.getTeam().getId().equals(teamId));
        if (hasPendingForTeam) {
            throw new IllegalArgumentException("A pending invitation already exists for this email");
        }

        UUID currentUserId = SecurityUtils.getCurrentUserId();
        Team team = teamRepository.getReferenceById(teamId);
        User invitedBy = userRepository.getReferenceById(currentUserId);

        Invitation invitation = Invitation.builder()
                .team(team)
                .email(request.email())
                .invitedBy(invitedBy)
                .role(request.role())
                .token(UUID.randomUUID().toString())
                .status(InvitationStatus.PENDING)
                .expiresAt(Instant.now().plus(AppConstants.INVITATION_EXPIRY_DAYS, ChronoUnit.DAYS))
                .build();
        invitation = invitationRepository.save(invitation);

        return mapToInvitationResponse(invitation);
    }

    public TeamResponse acceptInvitation(String token) {
        Invitation invitation = invitationRepository.findByToken(token)
                .orElseThrow(() -> new EntityNotFoundException("Invitation not found"));

        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new IllegalArgumentException("Invitation is no longer valid");
        }

        if (invitation.getExpiresAt().isBefore(Instant.now())) {
            invitation.setStatus(InvitationStatus.EXPIRED);
            invitationRepository.save(invitation);
            throw new IllegalArgumentException("Invitation has expired");
        }

        UUID currentUserId = SecurityUtils.getCurrentUserId();
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        if (!currentUser.getEmail().equalsIgnoreCase(invitation.getEmail())) {
            throw new AccessDeniedException("This invitation is for a different email address");
        }

        TeamMember member = TeamMember.builder()
                .team(invitation.getTeam())
                .user(currentUser)
                .role(invitation.getRole())
                .joinedAt(Instant.now())
                .build();
        teamMemberRepository.save(member);

        invitation.setStatus(InvitationStatus.ACCEPTED);
        invitationRepository.save(invitation);

        return mapToTeamResponse(invitation.getTeam());
    }

    @Transactional(readOnly = true)
    public List<InvitationResponse> getTeamInvitations(UUID teamId) {
        verifyTeamAdmin(teamId);
        return invitationRepository.findByTeamIdAndStatus(teamId, InvitationStatus.PENDING).stream()
                .map(this::mapToInvitationResponse)
                .toList();
    }

    public void cancelInvitation(UUID invitationId) {
        Invitation invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new EntityNotFoundException("Invitation not found"));
        verifyTeamAdmin(invitation.getTeam().getId());
        invitation.setStatus(InvitationStatus.EXPIRED);
        invitationRepository.save(invitation);
    }

    private TeamResponse mapToTeamResponse(Team team) {
        long memberCount = teamMemberRepository.countByTeamId(team.getId());
        return new TeamResponse(
                team.getId(),
                team.getName(),
                team.getDescription(),
                team.getOwner().getId(),
                team.getOwner().getDisplayName(),
                team.getTeamsWebhookUrl(),
                (int) memberCount,
                team.getCreatedAt(),
                team.getUpdatedAt()
        );
    }

    private TeamMemberResponse mapToTeamMemberResponse(TeamMember member, User user) {
        return new TeamMemberResponse(
                member.getId(),
                user.getId(),
                user.getDisplayName(),
                user.getEmail(),
                user.getAvatarUrl(),
                member.getRole(),
                member.getJoinedAt()
        );
    }

    private InvitationResponse mapToInvitationResponse(Invitation inv) {
        return new InvitationResponse(
                inv.getId(),
                inv.getEmail(),
                inv.getRole(),
                inv.getStatus(),
                inv.getInvitedBy().getDisplayName(),
                inv.getExpiresAt(),
                inv.getCreatedAt()
        );
    }

    private void verifyTeamMembership(UUID teamId) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        if (!teamMemberRepository.existsByTeamIdAndUserId(teamId, currentUserId)) {
            throw new AccessDeniedException("Not a member of this team");
        }
    }

    private void verifyTeamAdmin(UUID teamId) {
        TeamRole role = getCurrentUserTeamRole(teamId);
        if (role != TeamRole.OWNER && role != TeamRole.ADMIN) {
            throw new AccessDeniedException("Requires OWNER or ADMIN role");
        }
    }

    private TeamRole getCurrentUserTeamRole(UUID teamId) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        TeamMember member = teamMemberRepository.findByTeamIdAndUserId(teamId, currentUserId)
                .orElseThrow(() -> new AccessDeniedException("Not a member of this team"));
        return member.getRole();
    }
}
