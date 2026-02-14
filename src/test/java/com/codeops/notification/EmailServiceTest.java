package com.codeops.notification;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.SendEmailRequest;
import software.amazon.awssdk.services.ses.model.SendEmailResponse;
import software.amazon.awssdk.services.ses.model.SesException;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock private SesClient sesClient;

    // --- sendEmail (SES disabled / dev mode) ---

    @Test
    void sendEmail_sesDisabled_logsAndReturns() {
        EmailService emailService = new EmailService(null, false, "noreply@codeops.dev");

        // Should not throw; in dev mode it just logs
        assertDoesNotThrow(() ->
                emailService.sendEmail("user@test.com", "Test Subject", "<p>Body</p>"));
    }

    @Test
    void sendEmail_sesDisabled_doesNotCallSesClient() {
        EmailService emailService = new EmailService(sesClient, false, "noreply@codeops.dev");

        emailService.sendEmail("user@test.com", "Test Subject", "<p>Body</p>");

        verifyNoInteractions(sesClient);
    }

    // --- sendEmail (SES enabled) ---

    @Test
    void sendEmail_sesEnabled_callsSesClient() {
        EmailService emailService = new EmailService(sesClient, true, "noreply@codeops.dev");
        when(sesClient.sendEmail(any(SendEmailRequest.class)))
                .thenReturn(SendEmailResponse.builder().messageId("msg-123").build());

        emailService.sendEmail("user@test.com", "Test Subject", "<h1>Hello</h1>");

        ArgumentCaptor<SendEmailRequest> captor = ArgumentCaptor.forClass(SendEmailRequest.class);
        verify(sesClient).sendEmail(captor.capture());

        SendEmailRequest request = captor.getValue();
        assertEquals("noreply@codeops.dev", request.source());
        assertEquals(List.of("user@test.com"), request.destination().toAddresses());
        assertEquals("Test Subject", request.message().subject().data());
        assertEquals("UTF-8", request.message().subject().charset());
        assertEquals("<h1>Hello</h1>", request.message().body().html().data());
    }

    @Test
    void sendEmail_sesEnabled_sesExceptionCaught_doesNotThrow() {
        EmailService emailService = new EmailService(sesClient, true, "noreply@codeops.dev");
        when(sesClient.sendEmail(any(SendEmailRequest.class)))
                .thenThrow(SesException.builder().message("Rate exceeded").build());

        assertDoesNotThrow(() ->
                emailService.sendEmail("user@test.com", "Subject", "<p>Body</p>"));
    }

    // --- sendInvitationEmail ---

    @Test
    void sendInvitationEmail_sesEnabled_sendsCorrectContent() {
        EmailService emailService = new EmailService(sesClient, true, "noreply@codeops.dev");
        when(sesClient.sendEmail(any(SendEmailRequest.class)))
                .thenReturn(SendEmailResponse.builder().messageId("msg-456").build());

        emailService.sendInvitationEmail("newuser@test.com", "Alpha Team", "Adam Allard", "https://codeops.dev/accept?t=abc");

        ArgumentCaptor<SendEmailRequest> captor = ArgumentCaptor.forClass(SendEmailRequest.class);
        verify(sesClient).sendEmail(captor.capture());

        SendEmailRequest request = captor.getValue();
        assertEquals(List.of("newuser@test.com"), request.destination().toAddresses());
        assertTrue(request.message().subject().data().contains("Team Invitation"));
        String body = request.message().body().html().data();
        assertTrue(body.contains("Alpha Team"));
        assertTrue(body.contains("Adam Allard"));
        assertTrue(body.contains("https://codeops.dev/accept?t=abc"));
    }

    @Test
    void sendInvitationEmail_sesDisabled_doesNotCallSes() {
        EmailService emailService = new EmailService(sesClient, false, "noreply@codeops.dev");

        emailService.sendInvitationEmail("newuser@test.com", "Team", "Inviter", "https://accept.url");

        verifyNoInteractions(sesClient);
    }

    @Test
    void sendInvitationEmail_htmlEscapesInputs() {
        EmailService emailService = new EmailService(sesClient, true, "noreply@codeops.dev");
        when(sesClient.sendEmail(any(SendEmailRequest.class)))
                .thenReturn(SendEmailResponse.builder().messageId("msg").build());

        emailService.sendInvitationEmail("user@test.com", "<script>alert('xss')</script>", "Attacker", "https://evil.com");

        ArgumentCaptor<SendEmailRequest> captor = ArgumentCaptor.forClass(SendEmailRequest.class);
        verify(sesClient).sendEmail(captor.capture());
        String body = captor.getValue().message().body().html().data();
        assertFalse(body.contains("<script>"));
        assertTrue(body.contains("&lt;script&gt;"));
    }

    // --- sendCriticalFindingAlert ---

    @Test
    void sendCriticalFindingAlert_sesEnabled_sendsCorrectContent() {
        EmailService emailService = new EmailService(sesClient, true, "noreply@codeops.dev");
        when(sesClient.sendEmail(any(SendEmailRequest.class)))
                .thenReturn(SendEmailResponse.builder().messageId("msg-789").build());

        emailService.sendCriticalFindingAlert("admin@test.com", "MyProject", 5, "https://codeops.dev/jobs/123");

        ArgumentCaptor<SendEmailRequest> captor = ArgumentCaptor.forClass(SendEmailRequest.class);
        verify(sesClient).sendEmail(captor.capture());

        SendEmailRequest request = captor.getValue();
        assertEquals(List.of("admin@test.com"), request.destination().toAddresses());
        assertTrue(request.message().subject().data().contains("Critical Findings Alert"));
        assertTrue(request.message().subject().data().contains("MyProject"));
        String body = request.message().body().html().data();
        assertTrue(body.contains("5"));
        assertTrue(body.contains("MyProject"));
        assertTrue(body.contains("https://codeops.dev/jobs/123"));
    }

    @Test
    void sendCriticalFindingAlert_sesDisabled_doesNotCallSes() {
        EmailService emailService = new EmailService(sesClient, false, "noreply@codeops.dev");

        emailService.sendCriticalFindingAlert("admin@test.com", "Project", 3, "https://url");

        verifyNoInteractions(sesClient);
    }

    // --- sendHealthDigest ---

    @Test
    void sendHealthDigest_sesEnabled_sendsCorrectContent() {
        EmailService emailService = new EmailService(sesClient, true, "noreply@codeops.dev");
        when(sesClient.sendEmail(any(SendEmailRequest.class)))
                .thenReturn(SendEmailResponse.builder().messageId("msg").build());

        List<Map<String, Object>> summaries = List.of(
                Map.of("name", "ProjectA", "healthScore", 92, "findings", 3),
                Map.of("name", "ProjectB", "healthScore", 78, "findings", 12)
        );

        emailService.sendHealthDigest("team@test.com", "Alpha Team", summaries);

        ArgumentCaptor<SendEmailRequest> captor = ArgumentCaptor.forClass(SendEmailRequest.class);
        verify(sesClient).sendEmail(captor.capture());

        SendEmailRequest request = captor.getValue();
        assertEquals(List.of("team@test.com"), request.destination().toAddresses());
        assertTrue(request.message().subject().data().contains("Weekly Health Digest"));
        assertTrue(request.message().subject().data().contains("Alpha Team"));
        String body = request.message().body().html().data();
        assertTrue(body.contains("ProjectA"));
        assertTrue(body.contains("92"));
        assertTrue(body.contains("ProjectB"));
        assertTrue(body.contains("78"));
        assertTrue(body.contains("12"));
    }

    @Test
    void sendHealthDigest_emptyProjectSummaries() {
        EmailService emailService = new EmailService(sesClient, true, "noreply@codeops.dev");
        when(sesClient.sendEmail(any(SendEmailRequest.class)))
                .thenReturn(SendEmailResponse.builder().messageId("msg").build());

        emailService.sendHealthDigest("team@test.com", "Team", List.of());

        verify(sesClient).sendEmail(any(SendEmailRequest.class));
    }

    @Test
    void sendHealthDigest_sesDisabled_doesNotCallSes() {
        EmailService emailService = new EmailService(sesClient, false, "noreply@codeops.dev");

        emailService.sendHealthDigest("team@test.com", "Team", List.of());

        verifyNoInteractions(sesClient);
    }

    @Test
    void sendHealthDigest_htmlEscapesProjectName() {
        EmailService emailService = new EmailService(sesClient, true, "noreply@codeops.dev");
        when(sesClient.sendEmail(any(SendEmailRequest.class)))
                .thenReturn(SendEmailResponse.builder().messageId("msg").build());

        List<Map<String, Object>> summaries = List.of(
                Map.of("name", "<img src=x onerror=alert(1)>", "healthScore", 50, "findings", 0)
        );

        emailService.sendHealthDigest("team@test.com", "Team", summaries);

        ArgumentCaptor<SendEmailRequest> captor = ArgumentCaptor.forClass(SendEmailRequest.class);
        verify(sesClient).sendEmail(captor.capture());
        String body = captor.getValue().message().body().html().data();
        assertFalse(body.contains("<img src=x"));
        assertTrue(body.contains("&lt;img"));
    }
}
