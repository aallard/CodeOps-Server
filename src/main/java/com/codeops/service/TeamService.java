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

/**
 * Manages team lifecycle, membership, role assignments, and invitations.
 *
 * <p>Teams are the primary organizational unit in CodeOps. Each team has an owner,
 * members with assigned roles ({@link TeamRole}), and can have pending invitations.
 * All mutating operations verify that the calling user has the appropriate team
 * role (OWNER or ADMIN) before proceeding.</p>
 *
 * <p>Team membership is enforced via the {@link TeamMember} join entity, and
 * invitations are tracked through the {@link Invitation} entity with expiration
 * and status management.</p>
 *
 * @see TeamController
 * @see Team
 * @see TeamMember
 * @see Invitation
 */
@Service
@RequiredArgsConstructor
@Transactional
public class TeamService {

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final UserRepository userRepository;
    private final InvitationRepository invitationRepository;

    /**
     * Creates a new team and assigns the current user as OWNER.
     *
     * <p>The calling user is automatically added as both the team owner and the
     * first team member with the {@link TeamRole#OWNER} role.</p>
     *
     * @param request the team creation request containing name, description, and optional webhook URL
     * @return the created team as a response DTO
     * @throws EntityNotFoundException if the current user is not found
     */
    public TeamResponse createTeam(CreateTeamRequest request) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

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

    /**
     * Retrieves a team by its ID.
     *
     * @param teamId the ID of the team to retrieve
     * @return the team as a response DTO including member count
     * @throws EntityNotFoundException if the team is not found
     * @throws AccessDeniedException if the current user is not a member of the team
     */
    @Transactional(readOnly = true)
    public TeamResponse getTeam(UUID teamId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new EntityNotFoundException("Team not found"));
        verifyTeamMembership(teamId);
        return mapToTeamResponse(team);
    }

    /**
     * Retrieves all teams that the current authenticated user belongs to.
     *
     * @return a list of team response DTOs for every team the user is a member of
     */
    @Transactional(readOnly = true)
    public List<TeamResponse> getTeamsForUser() {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        return teamMemberRepository.findByUserId(currentUserId).stream()
                .map(member -> mapToTeamResponse(member.getTeam()))
                .toList();
    }

    /**
     * Updates a team's name, description, or Teams webhook URL.
     *
     * <p>Only non-null fields in the request are applied. Requires the calling user
     * to have OWNER or ADMIN role on the team.</p>
     *
     * @param teamId the ID of the team to update
     * @param request the update request containing optional name, description, and webhook URL
     * @return the updated team as a response DTO
     * @throws EntityNotFoundException if the team is not found
     * @throws AccessDeniedException if the current user does not have OWNER or ADMIN role
     */
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

    /**
     * Deletes a team permanently.
     *
     * <p>Only the team owner can delete a team. This is a hard delete that removes
     * the team and all associated data via cascading.</p>
     *
     * @param teamId the ID of the team to delete
     * @throws EntityNotFoundException if the team is not found
     * @throws AccessDeniedException if the current user is not the team owner
     */
    public void deleteTeam(UUID teamId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new EntityNotFoundException("Team not found"));

        TeamRole currentRole = getCurrentUserTeamRole(teamId);
        if (currentRole != TeamRole.OWNER) {
            throw new AccessDeniedException("Only the team owner can delete the team");
        }

        teamRepository.delete(team);
    }

    /**
     * Retrieves all members of a team.
     *
     * @param teamId the ID of the team whose members to retrieve
     * @return a list of team member response DTOs with user details and roles
     * @throws AccessDeniedException if the current user is not a member of the team
     */
    @Transactional(readOnly = true)
    public List<TeamMemberResponse> getTeamMembers(UUID teamId) {
        verifyTeamMembership(teamId);
        return teamMemberRepository.findByTeamId(teamId).stream()
                .map(member -> mapToTeamMemberResponse(member, member.getUser()))
                .toList();
    }

    /**
     * Updates a team member's role.
     *
     * <p>Requires the calling user to have OWNER or ADMIN role. Special rules apply:</p>
     * <ul>
     *   <li>The owner's role cannot be changed directly (must use ownership transfer)</li>
     *   <li>Transferring ownership (setting role to OWNER) is restricted to the current owner,
     *       who is automatically demoted to ADMIN and the team's owner reference is updated</li>
     * </ul>
     *
     * @param teamId the ID of the team
     * @param userId the ID of the user whose role to update
     * @param request the request containing the new role
     * @return the updated team member as a response DTO
     * @throws EntityNotFoundException if the team member is not found
     * @throws IllegalArgumentException if attempting to change the owner's role directly
     * @throws AccessDeniedException if the current user lacks permission for the role change
     */
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

    /**
     * Removes a member from a team.
     *
     * <p>A user can remove themselves (self-removal) without admin privileges. Removing
     * another user requires OWNER or ADMIN role. The team owner cannot be removed.</p>
     *
     * @param teamId the ID of the team
     * @param userId the ID of the user to remove
     * @throws EntityNotFoundException if the team member is not found
     * @throws IllegalArgumentException if attempting to remove the team owner
     * @throws AccessDeniedException if the current user lacks permission (non-self removal without admin role)
     */
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

    /**
     * Creates a pending invitation to join a team.
     *
     * <p>Validates that the team has not reached the maximum member limit
     * ({@link AppConstants#MAX_TEAM_MEMBERS}), the user is not already a member,
     * and no pending invitation already exists for the email. The invitation is
     * assigned a random token and configured to expire after
     * {@link AppConstants#INVITATION_EXPIRY_DAYS} days.</p>
     *
     * @param teamId the ID of the team to invite the user to
     * @param request the invitation request containing the email address and role
     * @return the created invitation as a response DTO
     * @throws AccessDeniedException if the current user does not have OWNER or ADMIN role
     * @throws IllegalArgumentException if the team is at capacity, the user is already a member,
     *                                  or a pending invitation already exists
     * @throws EntityNotFoundException if the team or inviting user is not found
     */
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

        List<Invitation> pendingForTeam = invitationRepository.findByTeamIdAndEmailAndStatusForUpdate(teamId, request.email(), InvitationStatus.PENDING);
        if (!pendingForTeam.isEmpty()) {
            throw new IllegalArgumentException("A pending invitation already exists for this email");
        }

        UUID currentUserId = SecurityUtils.getCurrentUserId();
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new EntityNotFoundException("Team not found"));
        User invitedBy = userRepository.findById(currentUserId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

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

    /**
     * Accepts a team invitation using its unique token.
     *
     * <p>Validates that the invitation is still pending and not expired, and that the
     * current user's email matches the invitation email. On success, creates a new
     * team member with the role specified in the invitation and marks the invitation
     * as accepted.</p>
     *
     * @param token the unique invitation token
     * @return the team that the user joined as a response DTO
     * @throws EntityNotFoundException if the invitation or current user is not found
     * @throws IllegalArgumentException if the invitation is no longer pending or has expired
     * @throws AccessDeniedException if the current user's email does not match the invitation
     */
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

    /**
     * Retrieves all pending invitations for a team.
     *
     * @param teamId the ID of the team whose pending invitations to retrieve
     * @return a list of pending invitation response DTOs
     * @throws AccessDeniedException if the current user does not have OWNER or ADMIN role
     */
    @Transactional(readOnly = true)
    public List<InvitationResponse> getTeamInvitations(UUID teamId) {
        verifyTeamAdmin(teamId);
        return invitationRepository.findByTeamIdAndStatus(teamId, InvitationStatus.PENDING).stream()
                .map(this::mapToInvitationResponse)
                .toList();
    }

    /**
     * Cancels a pending invitation by setting its status to EXPIRED.
     *
     * @param invitationId the ID of the invitation to cancel
     * @throws EntityNotFoundException if the invitation is not found
     * @throws AccessDeniedException if the current user does not have OWNER or ADMIN role on the invitation's team
     */
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
