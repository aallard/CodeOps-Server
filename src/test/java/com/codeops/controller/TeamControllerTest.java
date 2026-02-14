package com.codeops.controller;

import com.codeops.dto.request.CreateTeamRequest;
import com.codeops.dto.request.InviteMemberRequest;
import com.codeops.dto.request.UpdateMemberRoleRequest;
import com.codeops.dto.request.UpdateTeamRequest;
import com.codeops.dto.response.InvitationResponse;
import com.codeops.dto.response.TeamMemberResponse;
import com.codeops.dto.response.TeamResponse;
import com.codeops.entity.enums.InvitationStatus;
import com.codeops.entity.enums.TeamRole;
import com.codeops.service.AuditLogService;
import com.codeops.service.TeamService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeamControllerTest {

    @Mock
    private TeamService teamService;

    @Mock
    private AuditLogService auditLogService;

    private TeamController controller;

    private final UUID currentUserId = UUID.randomUUID();
    private final UUID teamId = UUID.randomUUID();
    private final UUID memberId = UUID.randomUUID();
    private final UUID invitationId = UUID.randomUUID();
    private final Instant now = Instant.now();

    @BeforeEach
    void setUp() {
        controller = new TeamController(teamService, auditLogService);
        setSecurityContext(currentUserId);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void setSecurityContext(UUID userId) {
        var auth = new UsernamePasswordAuthenticationToken(userId, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private TeamResponse teamResponse() {
        return new TeamResponse(teamId, "My Team", "Description", currentUserId, "Owner", null, 1, now, now);
    }

    @Test
    void createTeam_returns201WithTeamResponse() {
        CreateTeamRequest request = new CreateTeamRequest("My Team", "Description", null);
        TeamResponse expected = teamResponse();
        when(teamService.createTeam(request)).thenReturn(expected);

        ResponseEntity<TeamResponse> response = controller.createTeam(request);

        assertEquals(201, response.getStatusCode().value());
        assertEquals(expected, response.getBody());
        verify(teamService).createTeam(request);
        verify(auditLogService).log(currentUserId, teamId, "TEAM_CREATED", "TEAM", teamId, null);
    }

    @Test
    void getTeams_returns200WithList() {
        List<TeamResponse> expected = List.of(teamResponse());
        when(teamService.getTeamsForUser()).thenReturn(expected);

        ResponseEntity<List<TeamResponse>> response = controller.getTeams();

        assertEquals(200, response.getStatusCode().value());
        assertEquals(expected, response.getBody());
        verify(teamService).getTeamsForUser();
    }

    @Test
    void getTeam_returns200WithTeam() {
        TeamResponse expected = teamResponse();
        when(teamService.getTeam(teamId)).thenReturn(expected);

        ResponseEntity<TeamResponse> response = controller.getTeam(teamId);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(expected, response.getBody());
        verify(teamService).getTeam(teamId);
    }

    @Test
    void updateTeam_returns200WithUpdatedTeam() {
        UpdateTeamRequest request = new UpdateTeamRequest("Updated Team", "New Desc", null);
        TeamResponse expected = new TeamResponse(teamId, "Updated Team", "New Desc", currentUserId, "Owner", null, 1, now, now);
        when(teamService.updateTeam(teamId, request)).thenReturn(expected);

        ResponseEntity<TeamResponse> response = controller.updateTeam(teamId, request);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(expected, response.getBody());
        verify(teamService).updateTeam(teamId, request);
        verify(auditLogService).log(currentUserId, teamId, "TEAM_UPDATED", "TEAM", teamId, null);
    }

    @Test
    void deleteTeam_returns204AndLogsAudit() {
        ResponseEntity<Void> response = controller.deleteTeam(teamId);

        assertEquals(204, response.getStatusCode().value());
        assertNull(response.getBody());
        verify(teamService).deleteTeam(teamId);
        verify(auditLogService).log(currentUserId, null, "TEAM_DELETED", "TEAM", teamId, null);
    }

    @Test
    void getTeamMembers_returns200WithList() {
        TeamMemberResponse member = new TeamMemberResponse(
                UUID.randomUUID(), memberId, "Member", "member@example.com", null, TeamRole.MEMBER, now);
        List<TeamMemberResponse> expected = List.of(member);
        when(teamService.getTeamMembers(teamId)).thenReturn(expected);

        ResponseEntity<List<TeamMemberResponse>> response = controller.getTeamMembers(teamId);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(expected, response.getBody());
        verify(teamService).getTeamMembers(teamId);
    }

    @Test
    void updateMemberRole_returns200WithUpdatedMember() {
        UpdateMemberRoleRequest request = new UpdateMemberRoleRequest(TeamRole.ADMIN);
        TeamMemberResponse expected = new TeamMemberResponse(
                UUID.randomUUID(), memberId, "Member", "member@example.com", null, TeamRole.ADMIN, now);
        when(teamService.updateMemberRole(teamId, memberId, request)).thenReturn(expected);

        ResponseEntity<TeamMemberResponse> response = controller.updateMemberRole(teamId, memberId, request);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(expected, response.getBody());
        verify(teamService).updateMemberRole(teamId, memberId, request);
        verify(auditLogService).log(currentUserId, teamId, "MEMBER_ROLE_UPDATED", "TEAM_MEMBER", memberId, null);
    }

    @Test
    void removeMember_returns204AndLogsAudit() {
        ResponseEntity<Void> response = controller.removeMember(teamId, memberId);

        assertEquals(204, response.getStatusCode().value());
        assertNull(response.getBody());
        verify(teamService).removeMember(teamId, memberId);
        verify(auditLogService).log(currentUserId, teamId, "MEMBER_REMOVED", "TEAM_MEMBER", memberId, null);
    }

    @Test
    void inviteMember_returns201WithInvitation() {
        InviteMemberRequest request = new InviteMemberRequest("invite@example.com", TeamRole.MEMBER);
        InvitationResponse expected = new InvitationResponse(
                invitationId, "invite@example.com", TeamRole.MEMBER, InvitationStatus.PENDING,
                "Owner", now.plusSeconds(604800), now);
        when(teamService.inviteMember(teamId, request)).thenReturn(expected);

        ResponseEntity<InvitationResponse> response = controller.inviteMember(teamId, request);

        assertEquals(201, response.getStatusCode().value());
        assertEquals(expected, response.getBody());
        verify(teamService).inviteMember(teamId, request);
        verify(auditLogService).log(currentUserId, teamId, "MEMBER_INVITED", "INVITATION", invitationId, null);
    }

    @Test
    void getTeamInvitations_returns200WithList() {
        InvitationResponse invitation = new InvitationResponse(
                invitationId, "invite@example.com", TeamRole.MEMBER, InvitationStatus.PENDING,
                "Owner", now.plusSeconds(604800), now);
        List<InvitationResponse> expected = List.of(invitation);
        when(teamService.getTeamInvitations(teamId)).thenReturn(expected);

        ResponseEntity<List<InvitationResponse>> response = controller.getTeamInvitations(teamId);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(expected, response.getBody());
        verify(teamService).getTeamInvitations(teamId);
    }

    @Test
    void cancelInvitation_returns204AndLogsAudit() {
        ResponseEntity<Void> response = controller.cancelInvitation(teamId, invitationId);

        assertEquals(204, response.getStatusCode().value());
        assertNull(response.getBody());
        verify(teamService).cancelInvitation(invitationId);
        verify(auditLogService).log(currentUserId, teamId, "INVITATION_CANCELLED", "INVITATION", invitationId, null);
    }

    @Test
    void acceptInvitation_returns200WithTeamResponse() {
        String token = "invitation-token";
        TeamResponse expected = teamResponse();
        when(teamService.acceptInvitation(token)).thenReturn(expected);

        ResponseEntity<TeamResponse> response = controller.acceptInvitation(token);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(expected, response.getBody());
        verify(teamService).acceptInvitation(token);
        verify(auditLogService).log(currentUserId, teamId, "INVITATION_ACCEPTED", "TEAM", teamId, null);
    }
}
