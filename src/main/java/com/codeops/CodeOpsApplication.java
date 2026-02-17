package com.codeops;

import com.codeops.config.JwtProperties;
import com.codeops.config.MailProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main entry point for the CodeOps Server application.
 *
 * <p>Bootstraps the Spring Boot application context with auto-configuration and
 * enables binding of {@link JwtProperties} and {@link MailProperties} from their
 * respective configuration property prefixes. Scheduling is enabled for periodic
 * tasks such as expired MFA email code cleanup.</p>
 *
 * @see JwtProperties
 * @see MailProperties
 */
@SpringBootApplication
@EnableConfigurationProperties({JwtProperties.class, MailProperties.class})
@EnableScheduling
public class CodeOpsApplication {
    /**
     * Application entry point. Launches the Spring Boot embedded server and initializes
     * the application context.
     *
     * @param args command-line arguments passed to the application
     */
    public static void main(String[] args) {
        SpringApplication.run(CodeOpsApplication.class, args);
    }
}
