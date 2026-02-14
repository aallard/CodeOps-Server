package com.codeops.notification;

import com.codeops.entity.Team;
import com.codeops.entity.TeamMember;
import com.codeops.entity.User;
import com.codeops.entity.enums.TeamRole;
import com.codeops.repository.TeamMemberRepository;
import com.codeops.repository.TeamRepository;
import com.codeops.repository.UserRepository;
import com.codeops.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationDispatcherTest {

    @Mock private NotificationService notificationService;
    @Mock private EmailService emailService;
    @Mock private TeamsWebhookService teamsWebhookService;
    @Mock private TeamMemberRepository teamMemberRepository;
    @Mock private TeamRepository teamRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private NotificationDispatcher notificationDispatcher;

    private UUID teamId;
    private UUID jobId;
    private UUID userId;
    private Team testTeam;
    private User testUser;

    @BeforeEach
    void setUp() {
        teamId = UUID.randomUUID();
        jobId = UUID.randomUUID();
        userId = UUID.randomUUID();

        testUser = User.builder()
                .email("test@codeops.dev")
                .passwordHash("hash")
                .displayName("Test User")
                .isActive(true)
                .build();
        testUser.setId(userId);

        testTeam = Team.builder()
                .name("Test Team")
                .owner(testUser)
                .teamsWebhookUrl("https://outlook.office.com/webhook/test")
                .build();
        testTeam.setId(teamId);
    }

    // --- dispatchJobCompleted ---

    @Test
    void dispatchJobCompleted_withWebhook_postsToTeams() {
        when(teamRepository.findById(teamId)).thenReturn(Optional.of(testTeam));

        notificationDispatcher.dispatchJobCompleted(teamId, jobId, "MyProject", "main", 85, 2, 5, "Adam");

        verify(teamsWebhookService).postJobCompleted(
                "https://outlook.office.com/webhook/test",
                "MyProject", "main", 85, 2, 5, "Adam"
        );
    }

    @Test
    void dispatchJobCompleted_teamNotFound_doesNotThrow() {
        when(teamRepository.findById(teamId)).thenReturn(Optional.empty());

        assertDoesNotThrow(() ->
                notificationDispatcher.dispatchJobCompleted(teamId, jobId, "Project", "main", 90, 0, 0, "User"));

        verifyNoInteractions(teamsWebhookService);
    }

    @Test
    void dispatchJobCompleted_noWebhookUrl_skipsNotification() {
        Team teamNoWebhook = Team.builder()
                .name("No Webhook Team")
                .owner(testUser)
                .teamsWebhookUrl(null)
                .build();
        teamNoWebhook.setId(teamId);

        when(teamRepository.findById(teamId)).thenReturn(Optional.of(teamNoWebhook));

        notificationDispatcher.dispatchJobCompleted(teamId, jobId, "Project", "main", 90, 0, 0, "User");

        verifyNoInteractions(teamsWebhookService);
    }

    @Test
    void dispatchJobCompleted_webhookThrowsException_doesNotPropagate() {
        when(teamRepository.findById(teamId)).thenReturn(Optional.of(testTeam));
        doThrow(new RuntimeException("Webhook error"))
                .when(teamsWebhookService).postJobCompleted(any(), any(), any(), anyInt(), anyInt(), anyInt(), any());

        assertDoesNotThrow(() ->
                notificationDispatcher.dispatchJobCompleted(teamId, jobId, "Project", "main", 90, 0, 0, "User"));
    }

    // --- dispatchCriticalFinding ---

    @Test
    void dispatchCriticalFinding_withWebhookAndEmailEnabled_sendsBoth() {
        when(teamRepository.findById(teamId)).thenReturn(Optional.of(testTeam));
        TeamMember member = TeamMember.builder()
                .team(testTeam).user(testUser).role(TeamRole.ADMIN).joinedAt(Instant.now()).build();
        member.setId(UUID.randomUUID());
        when(teamMemberRepository.findByTeamId(teamId)).thenReturn(List.of(member));
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(notificationService.shouldNotify(userId, "CRITICAL_FINDING", "email")).thenReturn(true);

        notificationDispatcher.dispatchCriticalFinding(teamId, UUID.randomUUID(), "MyProject", 3, "https://codeops.dev/jobs/123");

        verify(teamsWebhookService).postCriticalAlert("https://outlook.office.com/webhook/test", "MyProject", 3);
        verify(emailService).sendCriticalFindingAlert("test@codeops.dev", "MyProject", 3, "https://codeops.dev/jobs/123");
    }

    @Test
    void dispatchCriticalFinding_noWebhook_skipsTeamsButSendsEmail() {
        Team teamNoWebhook = Team.builder()
                .name("No Webhook Team")
                .owner(testUser)
                .teamsWebhookUrl(null)
                .build();
        teamNoWebhook.setId(teamId);

        when(teamRepository.findById(teamId)).thenReturn(Optional.of(teamNoWebhook));
        TeamMember member = TeamMember.builder()
                .team(teamNoWebhook).user(testUser).role(TeamRole.MEMBER).joinedAt(Instant.now()).build();
        member.setId(UUID.randomUUID());
        when(teamMemberRepository.findByTeamId(teamId)).thenReturn(List.of(member));
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(notificationService.shouldNotify(userId, "CRITICAL_FINDING", "email")).thenReturn(true);

        notificationDispatcher.dispatchCriticalFinding(teamId, UUID.randomUUID(), "MyProject", 5, "https://url");

        verifyNoInteractions(teamsWebhookService);
        verify(emailService).sendCriticalFindingAlert("test@codeops.dev", "MyProject", 5, "https://url");
    }

    @Test
    void dispatchCriticalFinding_emailNotificationsDisabled_skipsEmail() {
        when(teamRepository.findById(teamId)).thenReturn(Optional.of(testTeam));
        TeamMember member = TeamMember.builder()
                .team(testTeam).user(testUser).role(TeamRole.MEMBER).joinedAt(Instant.now()).build();
        member.setId(UUID.randomUUID());
        when(teamMemberRepository.findByTeamId(teamId)).thenReturn(List.of(member));
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(notificationService.shouldNotify(userId, "CRITICAL_FINDING", "email")).thenReturn(false);

        notificationDispatcher.dispatchCriticalFinding(teamId, UUID.randomUUID(), "MyProject", 2, "https://url");

        verify(teamsWebhookService).postCriticalAlert(any(), any(), anyInt());
        verify(emailService, never()).sendCriticalFindingAlert(any(), any(), anyInt(), any());
    }

    @Test
    void dispatchCriticalFinding_teamNotFound_doesNotThrow() {
        when(teamRepository.findById(teamId)).thenReturn(Optional.empty());

        assertDoesNotThrow(() ->
                notificationDispatcher.dispatchCriticalFinding(teamId, UUID.randomUUID(), "Project", 1, "https://url"));

        verifyNoInteractions(teamsWebhookService);
        verifyNoInteractions(emailService);
    }

    @Test
    void dispatchCriticalFinding_userNotFound_skipsEmail() {
        when(teamRepository.findById(teamId)).thenReturn(Optional.of(testTeam));
        TeamMember member = TeamMember.builder()
                .team(testTeam).user(testUser).role(TeamRole.MEMBER).joinedAt(Instant.now()).build();
        member.setId(UUID.randomUUID());
        when(teamMemberRepository.findByTeamId(teamId)).thenReturn(List.of(member));
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        notificationDispatcher.dispatchCriticalFinding(teamId, UUID.randomUUID(), "MyProject", 1, "https://url");

        verify(teamsWebhookService).postCriticalAlert(any(), any(), anyInt());
        verify(emailService, never()).sendCriticalFindingAlert(any(), any(), anyInt(), any());
    }

    @Test
    void dispatchCriticalFinding_multipleMembers_sendsEmailToEachEligible() {
        UUID userId2 = UUID.randomUUID();
        User user2 = User.builder().email("user2@codeops.dev").passwordHash("hash").displayName("User 2").isActive(true).build();
        user2.setId(userId2);

        when(teamRepository.findById(teamId)).thenReturn(Optional.of(testTeam));
        TeamMember member1 = TeamMember.builder().team(testTeam).user(testUser).role(TeamRole.ADMIN).joinedAt(Instant.now()).build();
        member1.setId(UUID.randomUUID());
        TeamMember member2 = TeamMember.builder().team(testTeam).user(user2).role(TeamRole.MEMBER).joinedAt(Instant.now()).build();
        member2.setId(UUID.randomUUID());

        when(teamMemberRepository.findByTeamId(teamId)).thenReturn(List.of(member1, member2));
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.findById(userId2)).thenReturn(Optional.of(user2));
        when(notificationService.shouldNotify(userId, "CRITICAL_FINDING", "email")).thenReturn(true);
        when(notificationService.shouldNotify(userId2, "CRITICAL_FINDING", "email")).thenReturn(false);

        notificationDispatcher.dispatchCriticalFinding(teamId, UUID.randomUUID(), "MyProject", 4, "https://url");

        verify(emailService).sendCriticalFindingAlert("test@codeops.dev", "MyProject", 4, "https://url");
        verify(emailService, never()).sendCriticalFindingAlert(eq("user2@codeops.dev"), any(), anyInt(), any());
    }

    // --- dispatchTaskAssigned ---

    @Test
    void dispatchTaskAssigned_emailEnabled_sendsEmail() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(notificationService.shouldNotify(userId, "TASK_ASSIGNED", "email")).thenReturn(true);

        notificationDispatcher.dispatchTaskAssigned(userId, "Fix login bug", "MyProject");

        verify(emailService).sendEmail(
                eq("test@codeops.dev"),
                eq("CodeOps — Task Assigned: Fix login bug"),
                eq("You have been assigned a task in MyProject: Fix login bug")
        );
    }

    @Test
    void dispatchTaskAssigned_emailDisabled_skipsEmail() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(notificationService.shouldNotify(userId, "TASK_ASSIGNED", "email")).thenReturn(false);

        notificationDispatcher.dispatchTaskAssigned(userId, "Fix login bug", "MyProject");

        verify(emailService, never()).sendEmail(any(), any(), any());
    }

    @Test
    void dispatchTaskAssigned_userNotFound_doesNotThrow() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertDoesNotThrow(() ->
                notificationDispatcher.dispatchTaskAssigned(userId, "Task", "Project"));

        verify(emailService, never()).sendEmail(any(), any(), any());
    }

    @Test
    void dispatchTaskAssigned_exceptionDuringNotification_doesNotPropagate() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(notificationService.shouldNotify(userId, "TASK_ASSIGNED", "email")).thenReturn(true);
        doThrow(new RuntimeException("Email failed")).when(emailService).sendEmail(any(), any(), any());

        assertDoesNotThrow(() ->
                notificationDispatcher.dispatchTaskAssigned(userId, "Task", "Project"));
    }

    // --- dispatchInvitation ---

    @Test
    void dispatchInvitation_success() {
        notificationDispatcher.dispatchInvitation("newuser@test.com", "Alpha Team", "Adam", "https://accept.url");

        verify(emailService).sendInvitationEmail("newuser@test.com", "Alpha Team", "Adam", "https://accept.url");
    }

    @Test
    void dispatchInvitation_emailServiceThrows_doesNotPropagate() {
        doThrow(new RuntimeException("SES error")).when(emailService)
                .sendInvitationEmail(any(), any(), any(), any());

        assertDoesNotThrow(() ->
                notificationDispatcher.dispatchInvitation("user@test.com", "Team", "Inviter", "https://url"));
    }
}
