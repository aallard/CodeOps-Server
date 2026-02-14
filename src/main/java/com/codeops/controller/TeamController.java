package com.codeops.controller;

import com.codeops.dto.request.CreateTeamRequest;
import com.codeops.dto.request.InviteMemberRequest;
import com.codeops.dto.request.UpdateMemberRoleRequest;
import com.codeops.dto.request.UpdateTeamRequest;
import com.codeops.dto.response.InvitationResponse;
import com.codeops.dto.response.TeamMemberResponse;
import com.codeops.dto.response.TeamResponse;
import com.codeops.security.SecurityUtils;
import com.codeops.service.AuditLogService;
import com.codeops.service.TeamService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for team management operations.
 *
 * <p>Teams are the primary organizational unit in CodeOps. Each team owns projects,
 * personas, and members. This controller handles team CRUD, member management
 * (listing, role updates, removal), and invitation workflows (invite, list, cancel,
 * accept). All endpoints require authentication. Authorization is enforced at the
 * service layer based on team membership and role.</p>
 *
 * <p>Mutating operations record an audit log entry via {@link AuditLogService}.</p>
 *
 * @see TeamService
 * @see AuditLogService
 */
@RestController
@RequestMapping("/api/v1/teams")
@RequiredArgsConstructor
@Tag(name = "Teams")
public class TeamController {

    private final TeamService teamService;
    private final AuditLogService auditLogService;

    /**
     * Creates a new team. The authenticated user becomes the team owner.
     *
     * <p>POST /api/v1/teams</p>
     *
     * <p>Requires authentication. Logs a TEAM_CREATED audit event.</p>
     *
     * @param request the team creation payload including name and optional settings
     * @return the created team with HTTP 201 status
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TeamResponse> createTeam(@Valid @RequestBody CreateTeamRequest request) {
        TeamResponse response = teamService.createTeam(request);
        auditLogService.log(SecurityUtils.getCurrentUserId(), response.id(), "TEAM_CREATED", "TEAM", response.id(), null);
        return ResponseEntity.status(201).body(response);
    }

    /**
     * Retrieves all teams the currently authenticated user is a member of.
     *
     * <p>GET /api/v1/teams</p>
     *
     * <p>Requires authentication.</p>
     *
     * @return a list of teams the current user belongs to
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<TeamResponse>> getTeams() {
        return ResponseEntity.ok(teamService.getTeamsForUser());
    }

    /**
     * Retrieves a team by its identifier.
     *
     * <p>GET /api/v1/teams/{teamId}</p>
     *
     * <p>Requires authentication.</p>
     *
     * @param teamId the UUID of the team to retrieve
     * @return the team details
     */
    @GetMapping("/{teamId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TeamResponse> getTeam(@PathVariable UUID teamId) {
        return ResponseEntity.ok(teamService.getTeam(teamId));
    }

    /**
     * Updates an existing team's details.
     *
     * <p>PUT /api/v1/teams/{teamId}</p>
     *
     * <p>Requires authentication. Logs a TEAM_UPDATED audit event.</p>
     *
     * @param teamId  the UUID of the team to update
     * @param request the update payload with fields to modify (e.g., name, settings)
     * @return the updated team
     */
    @PutMapping("/{teamId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TeamResponse> updateTeam(@PathVariable UUID teamId,
                                                   @Valid @RequestBody UpdateTeamRequest request) {
        TeamResponse response = teamService.updateTeam(teamId, request);
        auditLogService.log(SecurityUtils.getCurrentUserId(), teamId, "TEAM_UPDATED", "TEAM", teamId, null);
        return ResponseEntity.ok(response);
    }

    /**
     * Permanently deletes a team and all associated resources.
     *
     * <p>DELETE /api/v1/teams/{teamId}</p>
     *
     * <p>Requires authentication. Logs a TEAM_DELETED audit event.
     * Returns HTTP 204 No Content on success.</p>
     *
     * @param teamId the UUID of the team to delete
     * @return empty response with HTTP 204 status
     */
    @DeleteMapping("/{teamId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteTeam(@PathVariable UUID teamId) {
        teamService.deleteTeam(teamId);
        auditLogService.log(SecurityUtils.getCurrentUserId(), null, "TEAM_DELETED", "TEAM", teamId, null);
        return ResponseEntity.noContent().build();
    }

    /**
     * Retrieves all members of a specific team.
     *
     * <p>GET /api/v1/teams/{teamId}/members</p>
     *
     * <p>Requires authentication.</p>
     *
     * @param teamId the UUID of the team whose members to list
     * @return a list of team members with their roles
     */
    @GetMapping("/{teamId}/members")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<TeamMemberResponse>> getTeamMembers(@PathVariable UUID teamId) {
        return ResponseEntity.ok(teamService.getTeamMembers(teamId));
    }

    /**
     * Updates the role of a team member.
     *
     * <p>PUT /api/v1/teams/{teamId}/members/{userId}/role</p>
     *
     * <p>Requires authentication. Logs a MEMBER_ROLE_UPDATED audit event.</p>
     *
     * @param teamId  the UUID of the team
     * @param userId  the UUID of the user whose role to update
     * @param request the role update payload containing the new role
     * @return the updated team member details
     */
    @PutMapping("/{teamId}/members/{userId}/role")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TeamMemberResponse> updateMemberRole(@PathVariable UUID teamId,
                                                                @PathVariable UUID userId,
                                                                @Valid @RequestBody UpdateMemberRoleRequest request) {
        TeamMemberResponse response = teamService.updateMemberRole(teamId, userId, request);
        auditLogService.log(SecurityUtils.getCurrentUserId(), teamId, "MEMBER_ROLE_UPDATED", "TEAM_MEMBER", userId, null);
        return ResponseEntity.ok(response);
    }

    /**
     * Removes a member from a team.
     *
     * <p>DELETE /api/v1/teams/{teamId}/members/{userId}</p>
     *
     * <p>Requires authentication. Logs a MEMBER_REMOVED audit event.
     * Returns HTTP 204 No Content on success.</p>
     *
     * @param teamId the UUID of the team
     * @param userId the UUID of the user to remove from the team
     * @return empty response with HTTP 204 status
     */
    @DeleteMapping("/{teamId}/members/{userId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> removeMember(@PathVariable UUID teamId, @PathVariable UUID userId) {
        teamService.removeMember(teamId, userId);
        auditLogService.log(SecurityUtils.getCurrentUserId(), teamId, "MEMBER_REMOVED", "TEAM_MEMBER", userId, null);
        return ResponseEntity.noContent().build();
    }

    /**
     * Sends an invitation to a user to join a team.
     *
     * <p>POST /api/v1/teams/{teamId}/invitations</p>
     *
     * <p>Requires authentication. Logs a MEMBER_INVITED audit event.</p>
     *
     * @param teamId  the UUID of the team to invite the user to
     * @param request the invitation payload including the invitee's email and desired role
     * @return the created invitation with HTTP 201 status
     */
    @PostMapping("/{teamId}/invitations")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<InvitationResponse> inviteMember(@PathVariable UUID teamId,
                                                           @Valid @RequestBody InviteMemberRequest request) {
        InvitationResponse response = teamService.inviteMember(teamId, request);
        auditLogService.log(SecurityUtils.getCurrentUserId(), teamId, "MEMBER_INVITED", "INVITATION", response.id(), null);
        return ResponseEntity.status(201).body(response);
    }

    /**
     * Retrieves all pending invitations for a team.
     *
     * <p>GET /api/v1/teams/{teamId}/invitations</p>
     *
     * <p>Requires authentication.</p>
     *
     * @param teamId the UUID of the team whose invitations to list
     * @return a list of pending invitations for the team
     */
    @GetMapping("/{teamId}/invitations")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<InvitationResponse>> getTeamInvitations(@PathVariable UUID teamId) {
        return ResponseEntity.ok(teamService.getTeamInvitations(teamId));
    }

    /**
     * Cancels a pending invitation.
     *
     * <p>DELETE /api/v1/teams/{teamId}/invitations/{invitationId}</p>
     *
     * <p>Requires authentication. Logs an INVITATION_CANCELLED audit event.
     * Returns HTTP 204 No Content on success.</p>
     *
     * @param teamId       the UUID of the team the invitation belongs to
     * @param invitationId the UUID of the invitation to cancel
     * @return empty response with HTTP 204 status
     */
    @DeleteMapping("/{teamId}/invitations/{invitationId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> cancelInvitation(@PathVariable UUID teamId,
                                                 @PathVariable UUID invitationId) {
        teamService.cancelInvitation(invitationId);
        auditLogService.log(SecurityUtils.getCurrentUserId(), teamId, "INVITATION_CANCELLED", "INVITATION", invitationId, null);
        return ResponseEntity.noContent().build();
    }

    /**
     * Accepts a team invitation using a token. The authenticated user is added
     * as a team member with the role specified in the invitation.
     *
     * <p>POST /api/v1/teams/invitations/{token}/accept</p>
     *
     * <p>Requires authentication. Logs an INVITATION_ACCEPTED audit event.</p>
     *
     * @param token the invitation token (received via email or link)
     * @return the team the user has been added to
     */
    @PostMapping("/invitations/{token}/accept")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TeamResponse> acceptInvitation(@PathVariable String token) {
        TeamResponse response = teamService.acceptInvitation(token);
        auditLogService.log(SecurityUtils.getCurrentUserId(), response.id(), "INVITATION_ACCEPTED", "TEAM", response.id(), null);
        return ResponseEntity.ok(response);
    }
}
