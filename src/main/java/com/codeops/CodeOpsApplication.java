package com.codeops;

import com.codeops.config.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Main entry point for the CodeOps Server application.
 *
 * <p>Bootstraps the Spring Boot application context with auto-configuration and
 * enables binding of {@link JwtProperties} from the {@code codeops.jwt} configuration
 * properties prefix.</p>
 *
 * @see JwtProperties
 */
@SpringBootApplication
@EnableConfigurationProperties(JwtProperties.class)
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
