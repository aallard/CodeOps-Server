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

    @Async
    public void dispatchJobCompleted(UUID teamId, UUID jobId, String projectName, String branch, int healthScore, int criticalCount, int highCount, String runByName) {
        var team = teamRepository.findById(teamId).orElse(null);
        if (team != null && team.getTeamsWebhookUrl() != null) {
            teamsWebhookService.postJobCompleted(team.getTeamsWebhookUrl(), projectName, branch, healthScore, criticalCount, highCount, runByName);
        }
    }

    @Async
    public void dispatchCriticalFinding(UUID teamId, UUID projectId, String projectName, int criticalCount, String jobUrl) {
        var team = teamRepository.findById(teamId).orElse(null);
        if (team != null && team.getTeamsWebhookUrl() != null) {
            teamsWebhookService.postCriticalAlert(team.getTeamsWebhookUrl(), projectName, criticalCount);
        }
        List<TeamMember> members = teamMemberRepository.findByTeamId(teamId);
        for (TeamMember member : members) {
            User user = userRepository.findById(member.getUser().getId()).orElse(null);
            if (user != null && notificationService.shouldNotify(user.getId(), "CRITICAL_FINDING", "email")) {
                emailService.sendCriticalFindingAlert(user.getEmail(), projectName, criticalCount, jobUrl);
            }
        }
    }

    @Async
    public void dispatchTaskAssigned(UUID userId, String taskTitle, String projectName) {
        User user = userRepository.findById(userId).orElse(null);
        if (user != null && notificationService.shouldNotify(userId, "TASK_ASSIGNED", "email")) {
            emailService.sendEmail(user.getEmail(), "CodeOps — Task Assigned: " + taskTitle,
                    "You have been assigned a task in " + projectName + ": " + taskTitle);
        }
    }

    @Async
    public void dispatchInvitation(String toEmail, String teamName, String inviterName, String acceptUrl) {
        emailService.sendInvitationEmail(toEmail, teamName, inviterName, acceptUrl);
    }
}
