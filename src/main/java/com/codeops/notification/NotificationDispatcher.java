package com.codeops.notification;

import com.codeops.entity.TeamMember;
import com.codeops.entity.User;
import com.codeops.repository.TeamMemberRepository;
import com.codeops.repository.TeamRepository;
import com.codeops.repository.UserRepository;
import com.codeops.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Asynchronous notification dispatcher that coordinates sending notifications across
 * multiple channels (email via SES, Microsoft Teams via webhook).
 *
 * <p>All dispatch methods are annotated with {@code @Async} and execute on the application's
 * async thread pool. Exceptions are caught and logged at ERROR level to prevent notification
 * failures from propagating to calling services.</p>
 *
 * <p>Notification routing logic:</p>
 * <ul>
 *   <li>Teams webhooks are sent when a team has a configured {@code teamsWebhookUrl}</li>
 *   <li>Emails are sent to individual users based on their notification preferences,
 *       checked via {@link com.codeops.service.NotificationService#shouldNotify}</li>
 * </ul>
 *
 * @see EmailService
 * @see TeamsWebhookService
 * @see com.codeops.service.NotificationService
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationDispatcher {

    private final NotificationService notificationService;
    private final EmailService emailService;
    private final TeamsWebhookService teamsWebhookService;
    private final TeamMemberRepository teamMemberRepository;
    private final TeamRepository teamRepository;
    private final UserRepository userRepository;

    /**
     * Dispatches a job-completed notification to the team's Microsoft Teams channel via webhook.
     *
     * <p>Executes asynchronously. If the team is not found or has no webhook URL configured,
     * the notification is silently skipped (with a log message). Any exceptions are caught and
     * logged at ERROR level.</p>
     *
     * @param teamId        the ID of the team that owns the project
     * @param jobId         the ID of the completed job
     * @param projectName   the name of the project that was audited
     * @param branch        the branch that was audited
     * @param healthScore   the resulting health score (0-100)
     * @param criticalCount the number of critical findings
     * @param highCount     the number of high-severity findings
     * @param runByName     the display name of the user who triggered the job
     */
    @Async
    public void dispatchJobCompleted(UUID teamId, UUID jobId, String projectName, String branch, int healthScore, int criticalCount, int highCount, String runByName) {
        try {
            var team = teamRepository.findById(teamId).orElse(null);
            if (team == null) {
                log.warn("Cannot dispatch job completed notification: team not found, teamId={}", teamId);
                return;
            }
            if (team.getTeamsWebhookUrl() == null) {
                log.debug("No webhook URL configured for teamId={}, skipping notification", teamId);
                return;
            }
            teamsWebhookService.postJobCompleted(team.getTeamsWebhookUrl(), projectName, branch, healthScore, criticalCount, highCount, runByName);
        } catch (Exception e) {
            log.error("Failed to dispatch job completed notification: {}", e.getMessage(), e);
        }
    }

    /**
     * Dispatches critical finding notifications to the team's Teams channel and to individual
     * team members via email based on their notification preferences.
     *
     * <p>Executes asynchronously. For each team member, checks the user's notification
     * preferences via {@link com.codeops.service.NotificationService#shouldNotify} before sending
     * an email alert. Teams webhook is sent if the team has a configured webhook URL.</p>
     *
     * @param teamId        the ID of the team that owns the project
     * @param projectId     the ID of the project with critical findings
     * @param projectName   the name of the project with critical findings
     * @param criticalCount the number of critical findings detected
     * @param jobUrl        the URL to the job details page for reviewing findings
     */
    @Async
    public void dispatchCriticalFinding(UUID teamId, UUID projectId, String projectName, int criticalCount, String jobUrl) {
        try {
            var team = teamRepository.findById(teamId).orElse(null);
            if (team == null) {
                log.warn("Cannot dispatch critical finding notification: team not found, teamId={}", teamId);
                return;
            }
            if (team.getTeamsWebhookUrl() != null) {
                teamsWebhookService.postCriticalAlert(team.getTeamsWebhookUrl(), projectName, criticalCount);
            }
            List<TeamMember> members = teamMemberRepository.findByTeamId(teamId);
            for (TeamMember member : members) {
                User user = userRepository.findById(member.getUser().getId()).orElse(null);
                if (user != null && notificationService.shouldNotify(user.getId(), "CRITICAL_FINDING", "email")) {
                    emailService.sendCriticalFindingAlert(user.getEmail(), projectName, criticalCount, jobUrl);
                }
            }
        } catch (Exception e) {
            log.error("Failed to dispatch critical finding notification: {}", e.getMessage(), e);
        }
    }

    /**
     * Dispatches a task assignment email notification to the assigned user, if their
     * notification preferences allow it.
     *
     * <p>Executes asynchronously. Checks the user's notification preferences for the
     * {@code "TASK_ASSIGNED"} event type before sending the email.</p>
     *
     * @param userId      the ID of the user being assigned the task
     * @param taskTitle   the title of the assigned task
     * @param projectName the name of the project containing the task
     */
    @Async
    public void dispatchTaskAssigned(UUID userId, String taskTitle, String projectName) {
        try {
            User user = userRepository.findById(userId).orElse(null);
            if (user != null && notificationService.shouldNotify(userId, "TASK_ASSIGNED", "email")) {
                emailService.sendEmail(user.getEmail(), "CodeOps — Task Assigned: " + taskTitle,
                        "You have been assigned a task in " + projectName + ": " + taskTitle);
            }
        } catch (Exception e) {
            log.error("Failed to dispatch task assigned notification: {}", e.getMessage(), e);
        }
    }

    /**
     * Dispatches a team invitation email to the specified recipient.
     *
     * <p>Executes asynchronously. Delegates directly to {@link EmailService#sendInvitationEmail}
     * without checking notification preferences (invitations are always sent).</p>
     *
     * @param toEmail     the recipient's email address
     * @param teamName    the name of the team the user is being invited to
     * @param inviterName the display name of the user who sent the invitation
     * @param acceptUrl   the URL the recipient should click to accept the invitation
     */
    @Async
    public void dispatchInvitation(String toEmail, String teamName, String inviterName, String acceptUrl) {
        try {
            emailService.sendInvitationEmail(toEmail, teamName, inviterName, acceptUrl);
        } catch (Exception e) {
            log.error("Failed to dispatch invitation notification: {}", e.getMessage(), e);
        }
    }
}
