package com.codeops.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.*;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class EmailService {

    private final SesClient sesClient;
    private final boolean sesEnabled;
    private final String fromEmail;

    public EmailService(
            @Autowired(required = false) SesClient sesClient,
            @Value("${codeops.aws.ses.enabled:false}") boolean sesEnabled,
            @Value("${codeops.aws.ses.from-email:noreply@codeops.dev}") String fromEmail) {
        this.sesClient = sesClient;
        this.sesEnabled = sesEnabled;
        this.fromEmail = fromEmail;
    }

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

    public void sendInvitationEmail(String toEmail, String teamName, String inviterName, String acceptUrl) {
        String htmlBody = "<h2>Team Invitation</h2>"
                + "<p>You've been invited to join <strong>" + teamName + "</strong> on CodeOps by " + inviterName + ".</p>"
                + "<p><a href=\"" + acceptUrl + "\">Click here to accept</a></p>";
        sendEmail(toEmail, "CodeOps — Team Invitation", htmlBody);
    }

    public void sendCriticalFindingAlert(String toEmail, String projectName, int criticalCount, String jobUrl) {
        String htmlBody = "<h2>Critical Findings Alert</h2>"
                + "<p><strong>" + criticalCount + "</strong> critical findings detected in <strong>" + projectName + "</strong>.</p>"
                + "<p><a href=\"" + jobUrl + "\">Review findings</a></p>";
        sendEmail(toEmail, "CodeOps — Critical Findings Alert: " + projectName, htmlBody);
    }

    public void sendHealthDigest(String toEmail, String teamName, List<Map<String, Object>> projectSummaries) {
        StringBuilder html = new StringBuilder();
        html.append("<h2>Weekly Health Digest: ").append(teamName).append("</h2>");
        html.append("<table border='1' cellpadding='8'><tr><th>Project</th><th>Health Score</th><th>Findings</th></tr>");
        for (Map<String, Object> summary : projectSummaries) {
            html.append("<tr>");
            html.append("<td>").append(summary.getOrDefault("name", "")).append("</td>");
            html.append("<td>").append(summary.getOrDefault("healthScore", "")).append("</td>");
            html.append("<td>").append(summary.getOrDefault("findings", "")).append("</td>");
            html.append("</tr>");
        }
        html.append("</table>");
        sendEmail(toEmail, "CodeOps — Weekly Health Digest: " + teamName, html.toString());
    }
}
