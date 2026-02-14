package com.codeops.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ses.SesClient;

/**
 * Configures the AWS Simple Email Service (SES) client bean for sending transactional emails
 * (invitations, critical finding alerts, health digests).
 *
 * <p>The SES client is only created when the {@code codeops.aws.ses.enabled} property is set to
 * {@code "true"}. When disabled (the default for local development), emails are logged to the
 * console instead of being sent.</p>
 *
 * @see com.codeops.notification.EmailService
 */
@Configuration
public class SesConfig {

    /**
     * Creates an AWS {@link SesClient} configured with the specified region.
     *
     * <p>This bean is only instantiated when {@code codeops.aws.ses.enabled=true}.
     * AWS credentials are resolved by the default AWS SDK credential chain
     * (environment variables, instance profile, etc.).</p>
     *
     * @param region the AWS region identifier (e.g., {@code "us-east-1"}) from the
     *               {@code codeops.aws.ses.region} property
     * @return the configured SES client
     */
    @Bean
    @ConditionalOnProperty(name = "codeops.aws.ses.enabled", havingValue = "true")
    public SesClient sesClient(@Value("${codeops.aws.ses.region}") String region) {
        return SesClient.builder()
                .region(Region.of(region))
                .build();
    }
}
