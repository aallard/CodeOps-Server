package com.codeops.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "codeops.jwt")
@Getter
@Setter
public class JwtProperties {
    private String secret;
    private int expirationHours = 24;
    private int refreshExpirationDays = 30;
}
