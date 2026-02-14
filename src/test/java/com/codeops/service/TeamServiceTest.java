package com.codeops.service;

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
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TeamServiceTest {

    @Mock private TeamRepository teamRepository;
    @Mock private TeamMemberRepository teamMemberRepository;
    @Mock private UserRepository userRepository;
    @Mock private InvitationRepository invitationRepository;

    @InjectMocks
    private TeamService teamService;

    private UUID currentUserId;
    private UUID teamId;
    private User currentUser;
    private Team testTeam;
    private TeamMember ownerMember;

    @BeforeEach
    void setUp() {
        currentUserId = UUID.randomUUID();
        teamId = UUID.randomUUID();

        currentUser = User.builder()
                .email("owner@codeops.dev")
                .passwordHash("hash")
                .displayName("Team Owner")
                .avatarUrl("https://avatar.url/owner.png")
                .isActive(true)
                .build();
        currentUser.setId(currentUserId);
        currentUser.setCreatedAt(Instant.now());

        testTeam = Team.builder()
                .name("Test Team")
                .description("A test team")
                .owner(currentUser)
                .teamsWebhookUrl("https://webhook.example.com")
                .build();
        testTeam.setId(teamId);
        testTeam.setCreatedAt(Instant.now());
        testTeam.setUpdatedAt(Instant.now());

        ownerMember = TeamMember.builder()
                .team(testTeam)
                .user(currentUser)
                .role(TeamRole.OWNER)
                .joinedAt(Instant.now())
                .build();
        ownerMember.setId(UUID.randomUUID());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // --- createTeam ---

    @Test
    void createTeam_success() {
        setSecurityContext(currentUserId);
        CreateTeamRequest request = new CreateTeamRequest("New Team", "Description", "https://webhook.url");

        when(userRepository.findById(currentUserId)).thenReturn(Optional.of(currentUser));
        when(teamRepository.save(any(Team.class))).thenAnswer(inv -> {
            Team t = inv.getArgument(0);
            t.setId(teamId);
            t.setCreatedAt(Instant.now());
            t.setUpdatedAt(Instant.now());
            return t;
        });
        when(teamMemberRepository.save(any(TeamMember.class))).thenAnswer(inv -> inv.getArgument(0));
        when(teamMemberRepository.countByTeamId(teamId)).thenReturn(1L);

        TeamResponse response = teamService.createTeam(request);

        assertNotNull(response);
        assertEquals("New Team", response.name());
        assertEquals("Description", response.description());
        assertEquals(currentUserId, response.ownerId());
        assertEquals("Team Owner", response.ownerName());
        assertEquals("https://webhook.url", response.teamsWebhookUrl());
        assertEquals(1, response.memberCount());

        verify(teamRepository).save(any(Team.class));
        verify(teamMemberRepository).save(argThat(member ->
                member.getRole() == TeamRole.OWNER &&
                member.getUser().equals(currentUser)
        ));
    }

    @Test
    void createTeam_userNotFound_throws() {
        setSecurityContext(currentUserId);
        CreateTeamRequest request = new CreateTeamRequest("Team", null, null);

        when(userRepository.findById(currentUserId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> teamService.createTeam(request));
        verify(teamRepository, never()).save(any());
    }

    // --- getTeam ---

    @Test
    void getTeam_memberAccess_success() {
        setSecurityContext(currentUserId);

        when(teamRepository.findById(teamId)).thenReturn(Optional.of(testTeam));
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, currentUserId)).thenReturn(true);
        when(teamMemberRepository.countByTeamId(teamId)).thenReturn(3L);

        TeamResponse response = teamService.getTeam(teamId);

        assertEquals(teamId, response.id());
        assertEquals("Test Team", response.name());
        assertEquals(3, response.memberCount());
    }

    @Test
    void getTeam_notMember_throwsAccessDenied() {
        setSecurityContext(currentUserId);

        when(teamRepository.findById(teamId)).thenReturn(Optional.of(testTeam));
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, currentUserId)).thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> teamService.getTeam(teamId));
    }

    @Test
    void getTeam_notFound_throws() {
        setSecurityContext(currentUserId);

        when(teamRepository.findById(teamId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> teamService.getTeam(teamId));
    }

    // --- getTeamsForUser ---

    @Test
    void getTeamsForUser_returnsTeams() {
        setSecurityContext(currentUserId);

        TeamMember member1 = TeamMember.builder()
                .team(testTeam)
                .user(currentUser)
                .role(TeamRole.OWNER)
                .joinedAt(Instant.now())
                .build();

        when(teamMemberRepository.findByUserId(currentUserId)).thenReturn(List.of(member1));
        when(teamMemberRepository.countByTeamId(teamId)).thenReturn(2L);

        List<TeamResponse> result = teamService.getTeamsForUser();

        assertEquals(1, result.size());
        assertEquals("Test Team", result.get(0).name());
        assertEquals(2, result.get(0).memberCount());
    }

    @Test
    void getTeamsForUser_noTeams_returnsEmpty() {
        setSecurityContext(currentUserId);

        when(teamMemberRepository.findByUserId(currentUserId)).thenReturn(List.of());

        List<TeamResponse> result = teamService.getTeamsForUser();

        assertTrue(result.isEmpty());
    }

    // --- updateTeam ---

    @Test
    void updateTeam_adminAccess_success() {
        setSecurityContext(currentUserId);
        UpdateTeamRequest request = new UpdateTeamRequest("Updated Name", "Updated Desc", "https://new-webhook.url");

        when(teamRepository.findById(teamId)).thenReturn(Optional.of(testTeam));
        when(teamMemberRepository.findByTeamIdAndUserId(teamId, currentUserId))
                .thenReturn(Optional.of(ownerMember));
        when(teamRepository.save(any(Team.class))).thenAnswer(inv -> inv.getArgument(0));
        when(teamMemberRepository.countByTeamId(teamId)).thenReturn(1L);

        TeamResponse response = teamService.updateTeam(teamId, request);

        assertEquals("Updated Name", response.name());
        assertEquals("Updated Desc", response.description());
        assertEquals("https://new-webhook.url", response.teamsWebhookUrl());
    }

    @Test
    void updateTeam_nullFields_preservesExisting() {
        setSecurityContext(currentUserId);
        UpdateTeamRequest request = new UpdateTeamRequest(null, null, null);

        when(teamRepository.findById(teamId)).thenReturn(Optional.of(testTeam));
        when(teamMemberRepository.findByTeamIdAndUserId(teamId, currentUserId))
                .thenReturn(Optional.of(ownerMember));
        when(teamRepository.save(any(Team.class))).thenAnswer(inv -> inv.getArgument(0));
        when(teamMemberRepository.countByTeamId(teamId)).thenReturn(1L);

        TeamResponse response = teamService.updateTeam(teamId, request);

        assertEquals("Test Team", response.name());
        assertEquals("A test team", response.description());
    }

    @Test
    void updateTeam_notAdmin_throwsAccessDenied() {
        setSecurityContext(currentUserId);
        UpdateTeamRequest request = new UpdateTeamRequest("Name", null, null);

        TeamMember viewerMember = TeamMember.builder()
                .team(testTeam).user(currentUser).role(TeamRole.VIEWER).joinedAt(Instant.now()).build();
        viewerMember.setId(UUID.randomUUID());

        when(teamRepository.findById(teamId)).thenReturn(Optional.of(testTeam));
        when(teamMemberRepository.findByTeamIdAndUserId(teamId, currentUserId))
                .thenReturn(Optional.of(viewerMember));

        assertThrows(AccessDeniedException.class, () -> teamService.updateTeam(teamId, request));
        verify(teamRepository, never()).save(any());
    }

    // --- deleteTeam ---

    @Test
    void deleteTeam_owner_success() {
        setSecurityContext(currentUserId);

        when(teamRepository.findById(teamId)).thenReturn(Optional.of(testTeam));
        when(teamMemberRepository.findByTeamIdAndUserId(teamId, currentUserId))
                .thenReturn(Optional.of(ownerMember));

        teamService.deleteTeam(teamId);

        verify(teamRepository).delete(testTeam);
    }

    @Test
    void deleteTeam_nonOwner_throwsAccessDenied() {
        setSecurityContext(currentUserId);

        TeamMember adminMember = TeamMember.builder()
                .team(testTeam).user(currentUser).role(TeamRole.ADMIN).joinedAt(Instant.now()).build();
        adminMember.setId(UUID.randomUUID());

        when(teamRepository.findById(teamId)).thenReturn(Optional.of(testTeam));
        when(teamMemberRepository.findByTeamIdAndUserId(teamId, currentUserId))
                .thenReturn(Optional.of(adminMember));

        assertThrows(AccessDeniedException.class, () -> teamService.deleteTeam(teamId));
        verify(teamRepository, never()).delete(any());
    }

    @Test
    void deleteTeam_notFound_throws() {
        setSecurityContext(currentUserId);

        when(teamRepository.findById(teamId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> teamService.deleteTeam(teamId));
    }

    // --- getTeamMembers ---

    @Test
    void getTeamMembers_memberAccess_returnsList() {
        setSecurityContext(currentUserId);

        UUID otherUserId = UUID.randomUUID();
        User otherUser = User.builder()
                .email("member@codeops.dev")
                .passwordHash("hash")
                .displayName("Team Member")
                .avatarUrl("https://avatar.url/member.png")
                .isActive(true)
                .build();
        otherUser.setId(otherUserId);
        otherUser.setCreatedAt(Instant.now());

        TeamMember memberEntry = TeamMember.builder()
                .team(testTeam).user(otherUser).role(TeamRole.MEMBER).joinedAt(Instant.now()).build();
        memberEntry.setId(UUID.randomUUID());

        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, currentUserId)).thenReturn(true);
        when(teamMemberRepository.findByTeamId(teamId)).thenReturn(List.of(ownerMember, memberEntry));

        List<TeamMemberResponse> result = teamService.getTeamMembers(teamId);

        assertEquals(2, result.size());
        assertEquals("Team Owner", result.get(0).displayName());
        assertEquals(TeamRole.OWNER, result.get(0).role());
        assertEquals("Team Member", result.get(1).displayName());
        assertEquals(TeamRole.MEMBER, result.get(1).role());
    }

    @Test
    void getTeamMembers_notMember_throwsAccessDenied() {
        setSecurityContext(currentUserId);

        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, currentUserId)).thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> teamService.getTeamMembers(teamId));
    }

    // --- updateMemberRole ---

    @Test
    void updateMemberRole_adminChangesRole_success() {
        setSecurityContext(currentUserId);
        UUID targetUserId = UUID.randomUUID();

        User targetUser = User.builder()
                .email("target@codeops.dev").passwordHash("hash").displayName("Target User")
                .avatarUrl(null).isActive(true).build();
        targetUser.setId(targetUserId);
        targetUser.setCreatedAt(Instant.now());

        TeamMember targetMember = TeamMember.builder()
                .team(testTeam).user(targetUser).role(TeamRole.MEMBER).joinedAt(Instant.now()).build();
        targetMember.setId(UUID.randomUUID());

        UpdateMemberRoleRequest request = new UpdateMemberRoleRequest(TeamRole.ADMIN);

        when(teamMemberRepository.findByTeamIdAndUserId(teamId, currentUserId))
                .thenReturn(Optional.of(ownerMember));
        when(teamMemberRepository.findByTeamIdAndUserId(teamId, targetUserId))
                .thenReturn(Optional.of(targetMember));
        when(teamMemberRepository.save(any(TeamMember.class))).thenAnswer(inv -> inv.getArgument(0));

        TeamMemberResponse response = teamService.updateMemberRole(teamId, targetUserId, request);

        assertEquals(TeamRole.ADMIN, response.role());
        assertEquals("Target User", response.displayName());
    }

    @Test
    void updateMemberRole_cannotChangeOwnerRole_throws() {
        setSecurityContext(currentUserId);
        UUID ownerTargetId = UUID.randomUUID();

        User ownerTarget = User.builder()
                .email("owner2@codeops.dev").passwordHash("hash").displayName("Owner Target")
                .isActive(true).build();
        ownerTarget.setId(ownerTargetId);
        ownerTarget.setCreatedAt(Instant.now());

        TeamMember ownerTargetMember = TeamMember.builder()
                .team(testTeam).user(ownerTarget).role(TeamRole.OWNER).joinedAt(Instant.now()).build();
        ownerTargetMember.setId(UUID.randomUUID());

        UpdateMemberRoleRequest request = new UpdateMemberRoleRequest(TeamRole.ADMIN);

        when(teamMemberRepository.findByTeamIdAndUserId(teamId, currentUserId))
                .thenReturn(Optional.of(ownerMember));
        when(teamMemberRepository.findByTeamIdAndUserId(teamId, ownerTargetId))
                .thenReturn(Optional.of(ownerTargetMember));

        assertThrows(IllegalArgumentException.class,
                () -> teamService.updateMemberRole(teamId, ownerTargetId, request));
    }

    @Test
    void updateMemberRole_transferOwnership_success() {
        setSecurityContext(currentUserId);
        UUID targetUserId = UUID.randomUUID();

        User targetUser = User.builder()
                .email("newowner@codeops.dev").passwordHash("hash").displayName("New Owner")
                .isActive(true).build();
        targetUser.setId(targetUserId);
        targetUser.setCreatedAt(Instant.now());

        TeamMember targetMember = TeamMember.builder()
                .team(testTeam).user(targetUser).role(TeamRole.ADMIN).joinedAt(Instant.now()).build();
        targetMember.setId(UUID.randomUUID());

        UpdateMemberRoleRequest request = new UpdateMemberRoleRequest(TeamRole.OWNER);

        // First call for verifyTeamAdmin, second call for getCurrentUserTeamRole inside ownership transfer,
        // third call for fetching currentOwnerMember
        when(teamMemberRepository.findByTeamIdAndUserId(teamId, currentUserId))
                .thenReturn(Optional.of(ownerMember));
        when(teamMemberRepository.findByTeamIdAndUserId(teamId, targetUserId))
                .thenReturn(Optional.of(targetMember));
        when(teamMemberRepository.save(any(TeamMember.class))).thenAnswer(inv -> inv.getArgument(0));
        when(teamRepository.findById(teamId)).thenReturn(Optional.of(testTeam));
        when(teamRepository.save(any(Team.class))).thenAnswer(inv -> inv.getArgument(0));

        TeamMemberResponse response = teamService.updateMemberRole(teamId, targetUserId, request);

        assertEquals(TeamRole.OWNER, response.role());
        // Verify old owner was demoted to ADMIN
        verify(teamMemberRepository).save(argThat(m ->
                m.getUser().equals(currentUser) && m.getRole() == TeamRole.ADMIN
        ));
        // Verify team owner was updated
        verify(teamRepository).save(argThat(t -> t.getOwner().equals(targetUser)));
    }

    @Test
    void updateMemberRole_nonAdminCannotTransferOwnership_throws() {
        setSecurityContext(currentUserId);
        UUID targetUserId = UUID.randomUUID();

        TeamMember memberRole = TeamMember.builder()
                .team(testTeam).user(currentUser).role(TeamRole.MEMBER).joinedAt(Instant.now()).build();
        memberRole.setId(UUID.randomUUID());

        UpdateMemberRoleRequest request = new UpdateMemberRoleRequest(TeamRole.ADMIN);

        when(teamMemberRepository.findByTeamIdAndUserId(teamId, currentUserId))
                .thenReturn(Optional.of(memberRole));

        assertThrows(AccessDeniedException.class,
                () -> teamService.updateMemberRole(teamId, targetUserId, request));
    }

    // --- removeMember ---

    @Test
    void removeMember_selfRemoval_success() {
        setSecurityContext(currentUserId);

        TeamMember selfMember = TeamMember.builder()
                .team(testTeam).user(currentUser).role(TeamRole.MEMBER).joinedAt(Instant.now()).build();
        selfMember.setId(UUID.randomUUID());

        when(teamMemberRepository.findByTeamIdAndUserId(teamId, currentUserId))
                .thenReturn(Optional.of(selfMember));

        teamService.removeMember(teamId, currentUserId);

        verify(teamMemberRepository).delete(selfMember);
    }

    @Test
    void removeMember_adminRemovesOther_success() {
        setSecurityContext(currentUserId);
        UUID targetUserId = UUID.randomUUID();

        User targetUser = User.builder()
                .email("target@codeops.dev").passwordHash("hash").displayName("Target")
                .isActive(true).build();
        targetUser.setId(targetUserId);
        targetUser.setCreatedAt(Instant.now());

        TeamMember targetMember = TeamMember.builder()
                .team(testTeam).user(targetUser).role(TeamRole.MEMBER).joinedAt(Instant.now()).build();
        targetMember.setId(UUID.randomUUID());

        // verifyTeamAdmin needs to find current user as OWNER/ADMIN
        when(teamMemberRepository.findByTeamIdAndUserId(teamId, currentUserId))
                .thenReturn(Optional.of(ownerMember));
        when(teamMemberRepository.findByTeamIdAndUserId(teamId, targetUserId))
                .thenReturn(Optional.of(targetMember));

        teamService.removeMember(teamId, targetUserId);

        verify(teamMemberRepository).delete(targetMember);
    }

    @Test
    void removeMember_cannotRemoveOwner_throws() {
        setSecurityContext(currentUserId);

        when(teamMemberRepository.findByTeamIdAndUserId(teamId, currentUserId))
                .thenReturn(Optional.of(ownerMember));

        assertThrows(IllegalArgumentException.class,
                () -> teamService.removeMember(teamId, currentUserId));
        verify(teamMemberRepository, never()).delete(any(TeamMember.class));
    }

    @Test
    void removeMember_memberNotFound_throws() {
        setSecurityContext(currentUserId);
        UUID targetUserId = UUID.randomUUID();

        when(teamMemberRepository.findByTeamIdAndUserId(teamId, currentUserId))
                .thenReturn(Optional.of(ownerMember));
        when(teamMemberRepository.findByTeamIdAndUserId(teamId, targetUserId))
                .thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> teamService.removeMember(teamId, targetUserId));
    }

    // --- inviteMember ---

    @Test
    void inviteMember_success() {
        setSecurityContext(currentUserId);
        InviteMemberRequest request = new InviteMemberRequest("newmember@codeops.dev", TeamRole.MEMBER);

        when(teamMemberRepository.findByTeamIdAndUserId(teamId, currentUserId))
                .thenReturn(Optional.of(ownerMember));
        when(teamMemberRepository.countByTeamId(teamId)).thenReturn(5L);
        when(userRepository.findByEmail("newmember@codeops.dev")).thenReturn(Optional.empty());
        when(invitationRepository.findByTeamIdAndEmailAndStatusForUpdate(teamId, "newmember@codeops.dev", InvitationStatus.PENDING))
                .thenReturn(List.of());
        when(teamRepository.findById(teamId)).thenReturn(Optional.of(testTeam));
        when(userRepository.findById(currentUserId)).thenReturn(Optional.of(currentUser));
        when(invitationRepository.save(any(Invitation.class))).thenAnswer(inv -> {
            Invitation i = inv.getArgument(0);
            i.setId(UUID.randomUUID());
            i.setCreatedAt(Instant.now());
            return i;
        });

        InvitationResponse response = teamService.inviteMember(teamId, request);

        assertNotNull(response);
        assertEquals("newmember@codeops.dev", response.email());
        assertEquals(TeamRole.MEMBER, response.role());
        assertEquals(InvitationStatus.PENDING, response.status());
        assertEquals("Team Owner", response.invitedByName());
    }

    @Test
    void inviteMember_teamAtMaxCapacity_throws() {
        setSecurityContext(currentUserId);
        InviteMemberRequest request = new InviteMemberRequest("newmember@codeops.dev", TeamRole.MEMBER);

        when(teamMemberRepository.findByTeamIdAndUserId(teamId, currentUserId))
                .thenReturn(Optional.of(ownerMember));
        when(teamMemberRepository.countByTeamId(teamId)).thenReturn(50L);

        assertThrows(IllegalArgumentException.class,
                () -> teamService.inviteMember(teamId, request));
        verify(invitationRepository, never()).save(any());
    }

    @Test
    void inviteMember_userAlreadyMember_throws() {
        setSecurityContext(currentUserId);
        UUID existingUserId = UUID.randomUUID();

        User existingUser = User.builder()
                .email("existing@codeops.dev").passwordHash("hash").displayName("Existing")
                .isActive(true).build();
        existingUser.setId(existingUserId);
        existingUser.setCreatedAt(Instant.now());

        InviteMemberRequest request = new InviteMemberRequest("existing@codeops.dev", TeamRole.MEMBER);

        when(teamMemberRepository.findByTeamIdAndUserId(teamId, currentUserId))
                .thenReturn(Optional.of(ownerMember));
        when(teamMemberRepository.countByTeamId(teamId)).thenReturn(5L);
        when(userRepository.findByEmail("existing@codeops.dev")).thenReturn(Optional.of(existingUser));
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, existingUserId)).thenReturn(true);

        assertThrows(IllegalArgumentException.class,
                () -> teamService.inviteMember(teamId, request));
        verify(invitationRepository, never()).save(any());
    }

    @Test
    void inviteMember_duplicatePendingInvitation_throws() {
        setSecurityContext(currentUserId);
        InviteMemberRequest request = new InviteMemberRequest("duplicate@codeops.dev", TeamRole.MEMBER);

        Invitation existingInvitation = Invitation.builder()
                .team(testTeam).email("duplicate@codeops.dev").invitedBy(currentUser)
                .role(TeamRole.MEMBER).token("existing-token").status(InvitationStatus.PENDING)
                .expiresAt(Instant.now().plus(7, ChronoUnit.DAYS)).build();
        existingInvitation.setId(UUID.randomUUID());

        when(teamMemberRepository.findByTeamIdAndUserId(teamId, currentUserId))
                .thenReturn(Optional.of(ownerMember));
        when(teamMemberRepository.countByTeamId(teamId)).thenReturn(5L);
        when(userRepository.findByEmail("duplicate@codeops.dev")).thenReturn(Optional.empty());
        when(invitationRepository.findByTeamIdAndEmailAndStatusForUpdate(teamId, "duplicate@codeops.dev", InvitationStatus.PENDING))
                .thenReturn(List.of(existingInvitation));

        assertThrows(IllegalArgumentException.class,
                () -> teamService.inviteMember(teamId, request));
        verify(invitationRepository, never()).save(any());
    }

    // --- acceptInvitation ---

    @Test
    void acceptInvitation_success() {
        setSecurityContext(currentUserId);
        String token = "valid-invitation-token";

        Invitation invitation = Invitation.builder()
                .team(testTeam).email("owner@codeops.dev").invitedBy(currentUser)
                .role(TeamRole.MEMBER).token(token).status(InvitationStatus.PENDING)
                .expiresAt(Instant.now().plus(7, ChronoUnit.DAYS)).build();
        invitation.setId(UUID.randomUUID());
        invitation.setCreatedAt(Instant.now());

        when(invitationRepository.findByToken(token)).thenReturn(Optional.of(invitation));
        when(userRepository.findById(currentUserId)).thenReturn(Optional.of(currentUser));
        when(teamMemberRepository.save(any(TeamMember.class))).thenAnswer(inv -> inv.getArgument(0));
        when(invitationRepository.save(any(Invitation.class))).thenAnswer(inv -> inv.getArgument(0));
        when(teamMemberRepository.countByTeamId(teamId)).thenReturn(2L);

        TeamResponse response = teamService.acceptInvitation(token);

        assertNotNull(response);
        assertEquals(teamId, response.id());
        assertEquals("Test Team", response.name());
        verify(teamMemberRepository).save(argThat(m ->
                m.getRole() == TeamRole.MEMBER && m.getUser().equals(currentUser)
        ));
        verify(invitationRepository).save(argThat(inv ->
                inv.getStatus() == InvitationStatus.ACCEPTED
        ));
    }

    @Test
    void acceptInvitation_expired_throws() {
        setSecurityContext(currentUserId);
        String token = "expired-token";

        Invitation invitation = Invitation.builder()
                .team(testTeam).email("owner@codeops.dev").invitedBy(currentUser)
                .role(TeamRole.MEMBER).token(token).status(InvitationStatus.PENDING)
                .expiresAt(Instant.now().minus(1, ChronoUnit.DAYS)).build();
        invitation.setId(UUID.randomUUID());

        when(invitationRepository.findByToken(token)).thenReturn(Optional.of(invitation));

        assertThrows(IllegalArgumentException.class,
                () -> teamService.acceptInvitation(token));
        verify(invitationRepository).save(argThat(inv ->
                inv.getStatus() == InvitationStatus.EXPIRED
        ));
        verify(teamMemberRepository, never()).save(any());
    }

    @Test
    void acceptInvitation_alreadyAccepted_throws() {
        setSecurityContext(currentUserId);
        String token = "used-token";

        Invitation invitation = Invitation.builder()
                .team(testTeam).email("owner@codeops.dev").invitedBy(currentUser)
                .role(TeamRole.MEMBER).token(token).status(InvitationStatus.ACCEPTED)
                .expiresAt(Instant.now().plus(7, ChronoUnit.DAYS)).build();
        invitation.setId(UUID.randomUUID());

        when(invitationRepository.findByToken(token)).thenReturn(Optional.of(invitation));

        assertThrows(IllegalArgumentException.class,
                () -> teamService.acceptInvitation(token));
        verify(teamMemberRepository, never()).save(any());
    }

    @Test
    void acceptInvitation_wrongEmail_throwsAccessDenied() {
        setSecurityContext(currentUserId);
        String token = "wrong-email-token";

        Invitation invitation = Invitation.builder()
                .team(testTeam).email("someone-else@codeops.dev").invitedBy(currentUser)
                .role(TeamRole.MEMBER).token(token).status(InvitationStatus.PENDING)
                .expiresAt(Instant.now().plus(7, ChronoUnit.DAYS)).build();
        invitation.setId(UUID.randomUUID());

        when(invitationRepository.findByToken(token)).thenReturn(Optional.of(invitation));
        when(userRepository.findById(currentUserId)).thenReturn(Optional.of(currentUser));

        assertThrows(AccessDeniedException.class,
                () -> teamService.acceptInvitation(token));
        verify(teamMemberRepository, never()).save(any());
    }

    @Test
    void acceptInvitation_tokenNotFound_throws() {
        setSecurityContext(currentUserId);

        when(invitationRepository.findByToken("nonexistent")).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> teamService.acceptInvitation("nonexistent"));
    }

    // --- cancelInvitation ---

    @Test
    void cancelInvitation_success() {
        setSecurityContext(currentUserId);
        UUID invitationId = UUID.randomUUID();

        Invitation invitation = Invitation.builder()
                .team(testTeam).email("cancel@codeops.dev").invitedBy(currentUser)
                .role(TeamRole.MEMBER).token("cancel-token").status(InvitationStatus.PENDING)
                .expiresAt(Instant.now().plus(7, ChronoUnit.DAYS)).build();
        invitation.setId(invitationId);

        when(invitationRepository.findById(invitationId)).thenReturn(Optional.of(invitation));
        when(teamMemberRepository.findByTeamIdAndUserId(teamId, currentUserId))
                .thenReturn(Optional.of(ownerMember));
        when(invitationRepository.save(any(Invitation.class))).thenAnswer(inv -> inv.getArgument(0));

        teamService.cancelInvitation(invitationId);

        verify(invitationRepository).save(argThat(inv ->
                inv.getStatus() == InvitationStatus.EXPIRED
        ));
    }

    @Test
    void cancelInvitation_notFound_throws() {
        setSecurityContext(currentUserId);
        UUID invitationId = UUID.randomUUID();

        when(invitationRepository.findById(invitationId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> teamService.cancelInvitation(invitationId));
    }

    @Test
    void cancelInvitation_notAdmin_throwsAccessDenied() {
        setSecurityContext(currentUserId);
        UUID invitationId = UUID.randomUUID();

        Invitation invitation = Invitation.builder()
                .team(testTeam).email("cancel@codeops.dev").invitedBy(currentUser)
                .role(TeamRole.MEMBER).token("cancel-token").status(InvitationStatus.PENDING)
                .expiresAt(Instant.now().plus(7, ChronoUnit.DAYS)).build();
        invitation.setId(invitationId);

        TeamMember viewerMember = TeamMember.builder()
                .team(testTeam).user(currentUser).role(TeamRole.VIEWER).joinedAt(Instant.now()).build();
        viewerMember.setId(UUID.randomUUID());

        when(invitationRepository.findById(invitationId)).thenReturn(Optional.of(invitation));
        when(teamMemberRepository.findByTeamIdAndUserId(teamId, currentUserId))
                .thenReturn(Optional.of(viewerMember));

        assertThrows(AccessDeniedException.class,
                () -> teamService.cancelInvitation(invitationId));
        verify(invitationRepository, never()).save(any());
    }

    private void setSecurityContext(UUID userId) {
        var auth = new UsernamePasswordAuthenticationToken(userId, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
