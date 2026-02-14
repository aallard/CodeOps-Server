package com.codeops.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * Configures the AWS S3 client bean for file storage operations (reports, specs, personas, releases).
 *
 * <p>The S3 client is only created when the {@code codeops.aws.s3.enabled} property is set to
 * {@code "true"}. When disabled (the default for local development), file storage falls back to
 * the local filesystem at {@code ~/.codeops/storage/}.</p>
 *
 * @see AppConstants
 */
@Configuration
public class S3Config {

    /**
     * Creates an AWS {@link S3Client} configured with the specified region.
     *
     * <p>This bean is only instantiated when {@code codeops.aws.s3.enabled=true}.
     * AWS credentials are resolved by the default AWS SDK credential chain
     * (environment variables, instance profile, etc.).</p>
     *
     * @param region the AWS region identifier (e.g., {@code "us-east-1"}) from the
     *               {@code codeops.aws.s3.region} property
     * @return the configured S3 client
     */
    @Bean
    @ConditionalOnProperty(name = "codeops.aws.s3.enabled", havingValue = "true")
    public S3Client s3Client(@Value("${codeops.aws.s3.region}") String region) {
        return S3Client.builder()
                .region(Region.of(region))
                .build();
    }
}
