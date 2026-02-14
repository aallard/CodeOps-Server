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

@RestController
@RequestMapping("/api/v1/teams")
@RequiredArgsConstructor
@Tag(name = "Teams")
public class TeamController {

    private final TeamService teamService;
    private final AuditLogService auditLogService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TeamResponse> createTeam(@Valid @RequestBody CreateTeamRequest request) {
        TeamResponse response = teamService.createTeam(request);
        auditLogService.log(SecurityUtils.getCurrentUserId(), response.id(), "TEAM_CREATED", "TEAM", response.id(), null);
        return ResponseEntity.status(201).body(response);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<TeamResponse>> getTeams() {
        return ResponseEntity.ok(teamService.getTeamsForUser());
    }

    @GetMapping("/{teamId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TeamResponse> getTeam(@PathVariable UUID teamId) {
        return ResponseEntity.ok(teamService.getTeam(teamId));
    }

    @PutMapping("/{teamId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TeamResponse> updateTeam(@PathVariable UUID teamId,
                                                   @Valid @RequestBody UpdateTeamRequest request) {
        TeamResponse response = teamService.updateTeam(teamId, request);
        auditLogService.log(SecurityUtils.getCurrentUserId(), teamId, "TEAM_UPDATED", "TEAM", teamId, null);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{teamId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteTeam(@PathVariable UUID teamId) {
        teamService.deleteTeam(teamId);
        auditLogService.log(SecurityUtils.getCurrentUserId(), null, "TEAM_DELETED", "TEAM", teamId, null);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{teamId}/members")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<TeamMemberResponse>> getTeamMembers(@PathVariable UUID teamId) {
        return ResponseEntity.ok(teamService.getTeamMembers(teamId));
    }

    @PutMapping("/{teamId}/members/{userId}/role")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TeamMemberResponse> updateMemberRole(@PathVariable UUID teamId,
                                                                @PathVariable UUID userId,
                                                                @Valid @RequestBody UpdateMemberRoleRequest request) {
        TeamMemberResponse response = teamService.updateMemberRole(teamId, userId, request);
        auditLogService.log(SecurityUtils.getCurrentUserId(), teamId, "MEMBER_ROLE_UPDATED", "TEAM_MEMBER", userId, null);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{teamId}/members/{userId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> removeMember(@PathVariable UUID teamId, @PathVariable UUID userId) {
        teamService.removeMember(teamId, userId);
        auditLogService.log(SecurityUtils.getCurrentUserId(), teamId, "MEMBER_REMOVED", "TEAM_MEMBER", userId, null);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{teamId}/invitations")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<InvitationResponse> inviteMember(@PathVariable UUID teamId,
                                                           @Valid @RequestBody InviteMemberRequest request) {
        InvitationResponse response = teamService.inviteMember(teamId, request);
        auditLogService.log(SecurityUtils.getCurrentUserId(), teamId, "MEMBER_INVITED", "INVITATION", response.id(), null);
        return ResponseEntity.status(201).body(response);
    }

    @GetMapping("/{teamId}/invitations")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<InvitationResponse>> getTeamInvitations(@PathVariable UUID teamId) {
        return ResponseEntity.ok(teamService.getTeamInvitations(teamId));
    }

    @DeleteMapping("/{teamId}/invitations/{invitationId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> cancelInvitation(@PathVariable UUID teamId,
                                                 @PathVariable UUID invitationId) {
        teamService.cancelInvitation(invitationId);
        auditLogService.log(SecurityUtils.getCurrentUserId(), teamId, "INVITATION_CANCELLED", "INVITATION", invitationId, null);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/invitations/{token}/accept")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TeamResponse> acceptInvitation(@PathVariable String token) {
        TeamResponse response = teamService.acceptInvitation(token);
        auditLogService.log(SecurityUtils.getCurrentUserId(), response.id(), "INVITATION_ACCEPTED", "TEAM", response.id(), null);
        return ResponseEntity.ok(response);
    }
}
