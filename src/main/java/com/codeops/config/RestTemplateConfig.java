package com.codeops.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Provides a default {@link RestTemplate} bean for making outbound HTTP requests
 * throughout the CodeOps application (e.g., webhook notifications, external API calls).
 *
 * @see com.codeops.notification.TeamsWebhookService
 */
@Configuration
public class RestTemplateConfig {

    /**
     * Creates a default {@link RestTemplate} instance with no custom configuration.
     *
     * @return a new {@link RestTemplate}
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
