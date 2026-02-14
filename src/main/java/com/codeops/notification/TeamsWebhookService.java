package com.codeops.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.*;

/**
 * Service for sending notifications to Microsoft Teams channels via incoming webhook connectors.
 *
 * <p>Constructs MessageCard-format JSON payloads and posts them to configured webhook URLs
 * using {@link RestTemplate}. All webhook URLs are validated before use to enforce HTTPS
 * and reject internal/loopback network addresses (SSRF protection).</p>
 *
 * <p>Posting failures are caught and logged at ERROR level without propagating to callers.</p>
 *
 * @see NotificationDispatcher
 * @see com.codeops.config.RestTemplateConfig
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TeamsWebhookService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    private void validateWebhookUrl(String url) {
        try {
            URI uri = new URI(url);
            String host = uri.getHost();
            if (host == null) throw new IllegalArgumentException("Invalid webhook URL");

            InetAddress address = InetAddress.getByName(host);
            if (address.isLoopbackAddress() || address.isSiteLocalAddress() || address.isLinkLocalAddress()) {
                throw new IllegalArgumentException("Webhook URL must not point to internal network addresses");
            }

            String scheme = uri.getScheme();
            if (!"https".equalsIgnoreCase(scheme)) {
                throw new IllegalArgumentException("Webhook URL must use HTTPS");
            }
        } catch (URISyntaxException | UnknownHostException e) {
            throw new IllegalArgumentException("Invalid webhook URL: " + e.getMessage());
        }
    }

    /**
     * Posts a MessageCard-format notification to the specified Teams webhook URL.
     *
     * <p>If the webhook URL is {@code null} or blank, the method returns immediately.
     * The URL is validated for HTTPS and non-internal address before posting.
     * An optional action URL adds a "View in CodeOps" button to the card.</p>
     *
     * <p>Any exception during serialization or HTTP posting is caught and logged at ERROR level.</p>
     *
     * @param webhookUrl the Teams incoming webhook URL (must be HTTPS, non-internal)
     * @param title      the card title displayed prominently in the notification
     * @param subtitle   the activity title displayed below the main title
     * @param facts      a map of key-value pairs displayed as facts in the card body
     * @param actionUrl  optional URL for a "View in CodeOps" action button, or {@code null} to omit
     * @throws IllegalArgumentException if the webhook URL is not HTTPS or resolves to an internal address
     */
    public void postMessage(String webhookUrl, String title, String subtitle, Map<String, String> facts, String actionUrl) {
        if (webhookUrl == null || webhookUrl.isBlank()) return;
        validateWebhookUrl(webhookUrl);

        try {
            List<Map<String, String>> factsList = new ArrayList<>();
            facts.forEach((key, value) -> factsList.add(Map.of("name", key, "value", value)));

            Map<String, Object> section = new LinkedHashMap<>();
            section.put("activityTitle", subtitle);
            section.put("facts", factsList);
            section.put("markdown", true);

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("@type", "MessageCard");
            payload.put("@context", "http://schema.org/extensions");
            payload.put("summary", title);
            payload.put("themeColor", "0076D7");
            payload.put("title", title);
            payload.put("sections", List.of(section));

            if (actionUrl != null) {
                Map<String, Object> action = new LinkedHashMap<>();
                action.put("@type", "OpenUri");
                action.put("name", "View in CodeOps");
                action.put("targets", List.of(Map.of("os", "default", "uri", actionUrl)));
                payload.put("potentialAction", List.of(action));
            }

            String jsonPayload = objectMapper.writeValueAsString(payload);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(jsonPayload, headers);
            restTemplate.postForEntity(webhookUrl, entity, String.class);
        } catch (Exception e) {
            log.error("Failed to post to Teams webhook: {}", e.getMessage());
        }
    }

    /**
     * Posts a job-completed notification card to Teams with audit results including
     * project name, branch, health score, critical/high finding counts, and who ran the job.
     *
     * @param webhookUrl    the Teams incoming webhook URL
     * @param projectName   the name of the audited project
     * @param branch        the branch that was audited
     * @param healthScore   the resulting health score (0-100)
     * @param criticalCount the number of critical findings
     * @param highCount     the number of high-severity findings
     * @param runBy         the display name of the user who triggered the job
     */
    public void postJobCompleted(String webhookUrl, String projectName, String branch, int healthScore, int criticalCount, int highCount, String runBy) {
        LinkedHashMap<String, String> facts = new LinkedHashMap<>();
        facts.put("Project", projectName);
        facts.put("Branch", branch);
        facts.put("Health Score", healthScore + "/100");
        facts.put("Critical", String.valueOf(criticalCount));
        facts.put("High", String.valueOf(highCount));
        facts.put("Run By", runBy);
        postMessage(webhookUrl, "CodeOps — Audit Complete", projectName + " | " + branch, facts, null);
    }

    /**
     * Posts a critical alert notification card to Teams indicating that critical findings
     * require immediate review.
     *
     * @param webhookUrl    the Teams incoming webhook URL
     * @param projectName   the name of the project with critical findings
     * @param criticalCount the number of critical findings detected
     */
    public void postCriticalAlert(String webhookUrl, String projectName, int criticalCount) {
        LinkedHashMap<String, String> facts = new LinkedHashMap<>();
        facts.put("Project", projectName);
        facts.put("Critical Findings", String.valueOf(criticalCount));
        facts.put("Action Required", "Immediate review recommended");
        postMessage(webhookUrl, "CodeOps — Critical Alert", projectName, facts, null);
    }
}
