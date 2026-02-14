package com.codeops.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ses.SesClient;

@Configuration
public class SesConfig {

    @Bean
    @ConditionalOnProperty(name = "codeops.aws.ses.enabled", havingValue = "true")
    public SesClient sesClient(@Value("${codeops.aws.ses.region}") String region) {
        return SesClient.builder()
                .region(Region.of(region))
                .build();
    }
}
