package com.codeops.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TeamsWebhookServiceTest {

    @Mock private RestTemplate restTemplate;

    private TeamsWebhookService teamsWebhookService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        teamsWebhookService = new TeamsWebhookService(restTemplate, objectMapper);
    }

    // --- postMessage ---

    @Test
    void postMessage_validUrl_sendsPayload() {
        // Use a real public HTTPS URL that won't resolve to internal address
        String webhookUrl = "https://outlook.office.com/webhook/test-id";
        Map<String, String> facts = new LinkedHashMap<>();
        facts.put("Project", "MyProject");
        facts.put("Score", "85");

        when(restTemplate.postForEntity(eq(webhookUrl), any(HttpEntity.class), eq(String.class)))
                .thenReturn(null);

        teamsWebhookService.postMessage(webhookUrl, "Test Title", "Subtitle", facts, null);

        ArgumentCaptor<HttpEntity<String>> captor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForEntity(eq(webhookUrl), captor.capture(), eq(String.class));

        HttpEntity<String> entity = captor.getValue();
        assertEquals(MediaType.APPLICATION_JSON, entity.getHeaders().getContentType());

        String jsonBody = entity.getBody();
        assertNotNull(jsonBody);
        assertTrue(jsonBody.contains("MessageCard"));
        assertTrue(jsonBody.contains("Test Title"));
        assertTrue(jsonBody.contains("Subtitle"));
        assertTrue(jsonBody.contains("MyProject"));
        assertTrue(jsonBody.contains("85"));
    }

    @Test
    void postMessage_withActionUrl_includesAction() {
        String webhookUrl = "https://outlook.office.com/webhook/test-id";
        Map<String, String> facts = new LinkedHashMap<>();
        facts.put("Key", "Value");

        when(restTemplate.postForEntity(eq(webhookUrl), any(HttpEntity.class), eq(String.class)))
                .thenReturn(null);

        teamsWebhookService.postMessage(webhookUrl, "Title", "Sub", facts, "https://codeops.dev/jobs/123");

        ArgumentCaptor<HttpEntity<String>> captor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForEntity(eq(webhookUrl), captor.capture(), eq(String.class));

        String jsonBody = captor.getValue().getBody();
        assertNotNull(jsonBody);
        assertTrue(jsonBody.contains("potentialAction"));
        assertTrue(jsonBody.contains("OpenUri"));
        assertTrue(jsonBody.contains("View in CodeOps"));
        assertTrue(jsonBody.contains("https://codeops.dev/jobs/123"));
    }

    @Test
    void postMessage_nullActionUrl_noActionInPayload() {
        String webhookUrl = "https://outlook.office.com/webhook/test-id";
        Map<String, String> facts = new LinkedHashMap<>();
        facts.put("Key", "Value");

        when(restTemplate.postForEntity(eq(webhookUrl), any(HttpEntity.class), eq(String.class)))
                .thenReturn(null);

        teamsWebhookService.postMessage(webhookUrl, "Title", "Sub", facts, null);

        ArgumentCaptor<HttpEntity<String>> captor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForEntity(eq(webhookUrl), captor.capture(), eq(String.class));

        String jsonBody = captor.getValue().getBody();
        assertNotNull(jsonBody);
        assertFalse(jsonBody.contains("potentialAction"));
    }

    @Test
    void postMessage_nullWebhookUrl_doesNotSend() {
        teamsWebhookService.postMessage(null, "Title", "Sub", Map.of(), null);

        verifyNoInteractions(restTemplate);
    }

    @Test
    void postMessage_blankWebhookUrl_doesNotSend() {
        teamsWebhookService.postMessage("  ", "Title", "Sub", Map.of(), null);

        verifyNoInteractions(restTemplate);
    }

    @Test
    void postMessage_emptyWebhookUrl_doesNotSend() {
        teamsWebhookService.postMessage("", "Title", "Sub", Map.of(), null);

        verifyNoInteractions(restTemplate);
    }

    @Test
    void postMessage_httpUrl_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () ->
                teamsWebhookService.postMessage("http://example.com/webhook", "Title", "Sub", Map.of(), null));
        verifyNoInteractions(restTemplate);
    }

    @Test
    void postMessage_restTemplateThrows_doesNotPropagate() {
        String webhookUrl = "https://outlook.office.com/webhook/test-id";
        when(restTemplate.postForEntity(eq(webhookUrl), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new RuntimeException("Connection refused"));

        assertDoesNotThrow(() ->
                teamsWebhookService.postMessage(webhookUrl, "Title", "Sub", Map.of("K", "V"), null));
    }

    // --- validateWebhookUrl (tested through postMessage) ---

    @Test
    void postMessage_invalidUrl_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () ->
                teamsWebhookService.postMessage("not-a-url", "Title", "Sub", Map.of(), null));
        verifyNoInteractions(restTemplate);
    }

    @Test
    void postMessage_localhostUrl_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () ->
                teamsWebhookService.postMessage("https://localhost/webhook", "Title", "Sub", Map.of(), null));
        verifyNoInteractions(restTemplate);
    }

    @Test
    void postMessage_loopbackUrl_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () ->
                teamsWebhookService.postMessage("https://127.0.0.1/webhook", "Title", "Sub", Map.of(), null));
        verifyNoInteractions(restTemplate);
    }

    // --- postJobCompleted ---

    @Test
    void postJobCompleted_sendsCorrectFacts() {
        String webhookUrl = "https://outlook.office.com/webhook/test-id";
        when(restTemplate.postForEntity(eq(webhookUrl), any(HttpEntity.class), eq(String.class)))
                .thenReturn(null);

        teamsWebhookService.postJobCompleted(webhookUrl, "MyProject", "main", 85, 2, 5, "Adam");

        ArgumentCaptor<HttpEntity<String>> captor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForEntity(eq(webhookUrl), captor.capture(), eq(String.class));

        String jsonBody = captor.getValue().getBody();
        assertNotNull(jsonBody);
        assertTrue(jsonBody.contains("Audit Complete"));
        assertTrue(jsonBody.contains("MyProject"));
        assertTrue(jsonBody.contains("main"));
        assertTrue(jsonBody.contains("85/100"));
        assertTrue(jsonBody.contains("2"));
        assertTrue(jsonBody.contains("5"));
        assertTrue(jsonBody.contains("Adam"));
    }

    @Test
    void postJobCompleted_titleContainsProjectAndBranch() {
        String webhookUrl = "https://outlook.office.com/webhook/test-id";
        when(restTemplate.postForEntity(eq(webhookUrl), any(HttpEntity.class), eq(String.class)))
                .thenReturn(null);

        teamsWebhookService.postJobCompleted(webhookUrl, "Backend-API", "develop", 92, 0, 1, "CI");

        ArgumentCaptor<HttpEntity<String>> captor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForEntity(eq(webhookUrl), captor.capture(), eq(String.class));

        String jsonBody = captor.getValue().getBody();
        assertTrue(jsonBody.contains("Backend-API | develop"));
    }

    // --- postCriticalAlert ---

    @Test
    void postCriticalAlert_sendsCorrectFacts() {
        String webhookUrl = "https://outlook.office.com/webhook/test-id";
        when(restTemplate.postForEntity(eq(webhookUrl), any(HttpEntity.class), eq(String.class)))
                .thenReturn(null);

        teamsWebhookService.postCriticalAlert(webhookUrl, "MyProject", 7);

        ArgumentCaptor<HttpEntity<String>> captor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForEntity(eq(webhookUrl), captor.capture(), eq(String.class));

        String jsonBody = captor.getValue().getBody();
        assertNotNull(jsonBody);
        assertTrue(jsonBody.contains("Critical Alert"));
        assertTrue(jsonBody.contains("MyProject"));
        assertTrue(jsonBody.contains("7"));
        assertTrue(jsonBody.contains("Immediate review recommended"));
    }
}
