package com.codeops.notification;

import com.codeops.config.MailProperties;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.util.List;
import java.util.Map;

/**
 * Service for sending transactional emails via Spring {@link JavaMailSender} (SMTP).
 *
 * <p>When mail is disabled ({@code codeops.mail.enabled=false}, the default for local
 * development), all email sends are logged to the console at WARN level instead of being
 * dispatched. When enabled, emails are sent as UTF-8 HTML via the configured SMTP server.</p>
 *
 * <p>All SMTP send failures are caught and logged at ERROR level without propagating to callers,
 * ensuring email delivery issues do not disrupt application workflows.</p>
 *
 * @see MailProperties
 * @see NotificationDispatcher
 */
@Service
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final MailProperties mailProperties;

    /**
     * Constructs the email service with optional JavaMailSender injection.
     *
     * <p>The {@code mailSender} parameter is injected only when the Spring Mail auto-configuration
     * creates the bean (i.e., when {@code spring.mail.host} is configured); otherwise it is {@code null}.</p>
     *
     * @param mailSender     the Spring JavaMailSender, or {@code null} if SMTP is not configured
     * @param mailProperties the CodeOps mail configuration properties
     */
    public EmailService(
            @Autowired(required = false) JavaMailSender mailSender,
            MailProperties mailProperties) {
        this.mailSender = mailSender;
        this.mailProperties = mailProperties;
    }

    /**
     * Sends an HTML email to the specified recipient via SMTP, or logs the send attempt
     * if mail is disabled.
     *
     * <p>SMTP failures are caught and logged at ERROR level without re-throwing.</p>
     *
     * @param toEmail  the recipient email address
     * @param subject  the email subject line
     * @param htmlBody the HTML content of the email body
     */
    public void sendEmail(String toEmail, String subject, String htmlBody) {
        if (!mailProperties.isEnabled()) {
            log.warn("Mail disabled — email logged instead of sent: to={}, subject={}", toEmail, subject);
            return;
        }
        if (mailSender == null) {
            log.error("Mail enabled but JavaMailSender is not configured — cannot send email to={}", toEmail);
            return;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(mailProperties.getFromEmail());
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
            log.info("Email sent successfully: to={}, subject={}", toEmail, subject);
        } catch (MessagingException | org.springframework.mail.MailException e) {
            log.error("SMTP send failure: to={}, error={}", toEmail, e.getMessage(), e);
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

    /**
     * Sends an MFA verification code email to the specified recipient.
     *
     * <p>The code is displayed in a large, monospaced font for easy reading. The email
     * includes a note about the code's 10-minute expiration.</p>
     *
     * @param toEmail the recipient email address
     * @param code    the 6-digit MFA verification code
     */
    public void sendMfaCode(String toEmail, String code) {
        String htmlBody = "<h2>Your CodeOps Verification Code</h2>"
                + "<p>Your verification code is:</p>"
                + "<p style=\"font-size: 32px; font-family: monospace; font-weight: bold; letter-spacing: 8px;\">"
                + HtmlUtils.htmlEscape(code) + "</p>"
                + "<p>This code expires in 10 minutes. If you did not request this code, you can safely ignore this email.</p>";
        sendEmail(toEmail, "CodeOps — Verification Code", htmlBody);
    }
}
