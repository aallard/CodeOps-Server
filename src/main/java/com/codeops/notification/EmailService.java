package com.codeops.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.*;

import org.springframework.web.util.HtmlUtils;

import java.util.List;
import java.util.Map;

/**
 * Service for sending transactional emails via AWS Simple Email Service (SES).
 *
 * <p>When SES is disabled ({@code codeops.aws.ses.enabled=false}, the default for local
 * development), all email sends are logged to the console at INFO level instead of being
 * dispatched. When enabled, emails are sent as UTF-8 HTML via the configured SES client.</p>
 *
 * <p>All SES send failures are caught and logged at ERROR level without propagating to callers,
 * ensuring email delivery issues do not disrupt application workflows.</p>
 *
 * @see com.codeops.config.SesConfig
 * @see NotificationDispatcher
 */
@Service
@Slf4j
public class EmailService {

    private final SesClient sesClient;
    private final boolean sesEnabled;
    private final String fromEmail;

    /**
     * Constructs the email service with optional SES client injection.
     *
     * <p>The {@code sesClient} parameter is injected only when the SES bean is available
     * (i.e., when {@code codeops.aws.ses.enabled=true}); otherwise it is {@code null}.</p>
     *
     * @param sesClient  the AWS SES client, or {@code null} if SES is not enabled
     * @param sesEnabled whether SES email sending is enabled
     * @param fromEmail  the sender email address for outbound emails
     */
    public EmailService(
            @Autowired(required = false) SesClient sesClient,
            @Value("${codeops.aws.ses.enabled:false}") boolean sesEnabled,
            @Value("${codeops.aws.ses.from-email:noreply@codeops.dev}") String fromEmail) {
        this.sesClient = sesClient;
        this.sesEnabled = sesEnabled;
        this.fromEmail = fromEmail;
    }

    /**
     * Sends an HTML email to the specified recipient via SES, or logs the send attempt
     * if SES is disabled.
     *
     * <p>SES failures are caught and logged at ERROR level without re-throwing.</p>
     *
     * @param toEmail  the recipient email address
     * @param subject  the email subject line
     * @param htmlBody the HTML content of the email body
     */
    public void sendEmail(String toEmail, String subject, String htmlBody) {
        if (!sesEnabled) {
            log.info("Email (dev mode): to={}, subject={}", toEmail, subject);
            return;
        }
        try {
            SendEmailRequest request = SendEmailRequest.builder()
                    .destination(Destination.builder().toAddresses(List.of(toEmail)).build())
                    .message(Message.builder()
                            .subject(Content.builder().data(subject).charset("UTF-8").build())
                            .body(Body.builder()
                                    .html(Content.builder().data(htmlBody).charset("UTF-8").build())
                                    .build())
                            .build())
                    .source(fromEmail)
                    .build();
            sesClient.sendEmail(request);
        } catch (SesException e) {
            log.error("Failed to send email to {}: {}", toEmail, e.getMessage());
        }
    }

    /**
     * Sends a team invitation email containing the team name, inviter name, and an acceptance link.
     *
     * <p>All user-provided values are HTML-escaped to prevent XSS in email clients.</p>
     *
     * @param toEmail     the recipient email address
     * @param teamName    the name of the team the user is being invited to
     * @param inviterName the display name of the user who sent the invitation
     * @param acceptUrl   the URL the recipient should click to accept the invitation
     */
    public void sendInvitationEmail(String toEmail, String teamName, String inviterName, String acceptUrl) {
        String htmlBody = "<h2>Team Invitation</h2>"
                + "<p>You've been invited to join <strong>" + HtmlUtils.htmlEscape(teamName) + "</strong> on CodeOps by " + HtmlUtils.htmlEscape(inviterName) + ".</p>"
                + "<p><a href=\"" + HtmlUtils.htmlEscape(acceptUrl) + "\">Click here to accept</a></p>";
        sendEmail(toEmail, "CodeOps — Team Invitation", htmlBody);
    }

    /**
     * Sends a critical findings alert email notifying the recipient of critical audit findings
     * detected in a project.
     *
     * <p>All user-provided values are HTML-escaped to prevent XSS in email clients.</p>
     *
     * @param toEmail       the recipient email address
     * @param projectName   the name of the project with critical findings
     * @param criticalCount the number of critical findings detected
     * @param jobUrl        the URL to the job details page for reviewing findings
     */
    public void sendCriticalFindingAlert(String toEmail, String projectName, int criticalCount, String jobUrl) {
        String htmlBody = "<h2>Critical Findings Alert</h2>"
                + "<p><strong>" + criticalCount + "</strong> critical findings detected in <strong>" + HtmlUtils.htmlEscape(projectName) + "</strong>.</p>"
                + "<p><a href=\"" + HtmlUtils.htmlEscape(jobUrl) + "\">Review findings</a></p>";
        sendEmail(toEmail, "CodeOps — Critical Findings Alert: " + HtmlUtils.htmlEscape(projectName), htmlBody);
    }

    /**
     * Sends a weekly health digest email containing an HTML table of project health summaries
     * for the specified team.
     *
     * <p>Each project summary map is expected to contain {@code "name"}, {@code "healthScore"},
     * and {@code "findings"} keys. All values are HTML-escaped to prevent XSS in email clients.</p>
     *
     * @param toEmail          the recipient email address
     * @param teamName         the name of the team for the digest
     * @param projectSummaries a list of maps, each containing project name, health score, and finding count
     */
    public void sendHealthDigest(String toEmail, String teamName, List<Map<String, Object>> projectSummaries) {
        StringBuilder html = new StringBuilder();
        html.append("<h2>Weekly Health Digest: ").append(HtmlUtils.htmlEscape(teamName)).append("</h2>");
        html.append("<table border='1' cellpadding='8'><tr><th>Project</th><th>Health Score</th><th>Findings</th></tr>");
        for (Map<String, Object> summary : projectSummaries) {
            html.append("<tr>");
            html.append("<td>").append(HtmlUtils.htmlEscape(String.valueOf(summary.getOrDefault("name", "")))).append("</td>");
            html.append("<td>").append(HtmlUtils.htmlEscape(String.valueOf(summary.getOrDefault("healthScore", "")))).append("</td>");
            html.append("<td>").append(HtmlUtils.htmlEscape(String.valueOf(summary.getOrDefault("findings", "")))).append("</td>");
            html.append("</tr>");
        }
        html.append("</table>");
        sendEmail(toEmail, "CodeOps — Weekly Health Digest: " + HtmlUtils.htmlEscape(teamName), html.toString());
    }
}
