package com.codeops.notification;

import com.codeops.config.MailProperties;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock private JavaMailSender mailSender;
    @Mock private MimeMessage mimeMessage;

    private MailProperties enabledProps() {
        MailProperties props = new MailProperties();
        props.setEnabled(true);
        props.setFromEmail("noreply@codeops.dev");
        return props;
    }

    private MailProperties disabledProps() {
        MailProperties props = new MailProperties();
        props.setEnabled(false);
        props.setFromEmail("noreply@codeops.dev");
        return props;
    }

    // --- sendEmail (mail disabled / dev mode) ---

    @Test
    void sendEmail_mailDisabled_logsAndReturns() {
        EmailService emailService = new EmailService(null, disabledProps());

        assertDoesNotThrow(() ->
                emailService.sendEmail("user@test.com", "Test Subject", "<p>Body</p>"));
    }

    @Test
    void sendEmail_mailDisabled_doesNotCallMailSender() {
        EmailService emailService = new EmailService(mailSender, disabledProps());

        emailService.sendEmail("user@test.com", "Test Subject", "<p>Body</p>");

        verifyNoInteractions(mailSender);
    }

    // --- sendEmail (mail enabled) ---

    @Test
    void sendEmail_mailEnabled_callsMailSender() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        EmailService emailService = new EmailService(mailSender, enabledProps());
        emailService.sendEmail("user@test.com", "Test Subject", "<h1>Hello</h1>");

        verify(mailSender).createMimeMessage();
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void sendEmail_mailEnabled_nullSender_doesNotThrow() {
        EmailService emailService = new EmailService(null, enabledProps());

        assertDoesNotThrow(() ->
                emailService.sendEmail("user@test.com", "Subject", "<p>Body</p>"));
    }

    @Test
    void sendEmail_mailEnabled_messagingException_doesNotThrow() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new org.springframework.mail.MailSendException("SMTP error"))
                .when(mailSender).send(any(MimeMessage.class));

        EmailService emailService = new EmailService(mailSender, enabledProps());

        assertDoesNotThrow(() ->
                emailService.sendEmail("user@test.com", "Subject", "<p>Body</p>"));
    }

    // --- sendInvitationEmail ---

    @Test
    void sendInvitationEmail_mailEnabled_callsMailSender() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        EmailService emailService = new EmailService(mailSender, enabledProps());
        emailService.sendInvitationEmail("newuser@test.com", "Alpha Team", "Adam Allard", "https://codeops.dev/accept?t=abc");

        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void sendInvitationEmail_mailDisabled_doesNotCallMailSender() {
        EmailService emailService = new EmailService(mailSender, disabledProps());

        emailService.sendInvitationEmail("newuser@test.com", "Team", "Inviter", "https://accept.url");

        verifyNoInteractions(mailSender);
    }

    // --- sendCriticalFindingAlert ---

    @Test
    void sendCriticalFindingAlert_mailEnabled_callsMailSender() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        EmailService emailService = new EmailService(mailSender, enabledProps());
        emailService.sendCriticalFindingAlert("admin@test.com", "MyProject", 5, "https://codeops.dev/jobs/123");

        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void sendCriticalFindingAlert_mailDisabled_doesNotCallMailSender() {
        EmailService emailService = new EmailService(mailSender, disabledProps());

        emailService.sendCriticalFindingAlert("admin@test.com", "Project", 3, "https://url");

        verifyNoInteractions(mailSender);
    }

    // --- sendHealthDigest ---

    @Test
    void sendHealthDigest_mailEnabled_callsMailSender() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        EmailService emailService = new EmailService(mailSender, enabledProps());
        List<Map<String, Object>> summaries = List.of(
                Map.of("name", "ProjectA", "healthScore", 92, "findings", 3));

        emailService.sendHealthDigest("team@test.com", "Alpha Team", summaries);

        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void sendHealthDigest_mailDisabled_doesNotCallMailSender() {
        EmailService emailService = new EmailService(mailSender, disabledProps());

        emailService.sendHealthDigest("team@test.com", "Team", List.of());

        verifyNoInteractions(mailSender);
    }

    // --- sendMfaCode ---

    @Test
    void sendMfaCode_mailEnabled_callsMailSender() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        EmailService emailService = new EmailService(mailSender, enabledProps());
        emailService.sendMfaCode("user@test.com", "123456");

        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void sendMfaCode_mailDisabled_doesNotCallMailSender() {
        EmailService emailService = new EmailService(mailSender, disabledProps());

        emailService.sendMfaCode("user@test.com", "123456");

        verifyNoInteractions(mailSender);
    }
}
