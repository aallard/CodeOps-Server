package com.codeops.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the CodeOps email subsystem, bound from the
 * {@code codeops.mail} prefix in application YAML/properties files.
 *
 * <p>When {@code enabled} is {@code false} (the default for local development),
 * all email operations are logged to the console instead of being sent via SMTP.
 * When {@code enabled} is {@code true}, emails are dispatched via the Spring
 * {@link org.springframework.mail.javamail.JavaMailSender} auto-configured from
 * {@code spring.mail.*} properties.</p>
 *
 * @see com.codeops.notification.EmailService
 */
@ConfigurationProperties(prefix = "codeops.mail")
public class MailProperties {

    private boolean enabled = false;
    private String fromEmail = "noreply@codeops.dev";

    /**
     * Returns whether SMTP email sending is enabled.
     *
     * @return {@code true} if emails should be sent via SMTP; {@code false} to log only
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Sets whether SMTP email sending is enabled.
     *
     * @param enabled {@code true} to enable SMTP sending; {@code false} to log only
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Returns the sender ("From") email address used for all outbound emails.
     *
     * @return the sender email address
     */
    public String getFromEmail() {
        return fromEmail;
    }

    /**
     * Sets the sender ("From") email address used for all outbound emails.
     *
     * @param fromEmail the sender email address
     */
    public void setFromEmail(String fromEmail) {
        this.fromEmail = fromEmail;
    }
}
