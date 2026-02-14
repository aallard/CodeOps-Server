package com.codeops.config;

import org.junit.jupiter.api.Test;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

class CorsConfigTest {

    @Test
    void corsConfigurationSource_createsValidConfig() throws Exception {
        CorsConfig corsConfig = new CorsConfig();
        Field field = CorsConfig.class.getDeclaredField("allowedOrigins");
        field.setAccessible(true);
        field.set(corsConfig, "http://localhost:3000");

        CorsConfigurationSource source = corsConfig.corsConfigurationSource();
        assertNotNull(source);

        CorsConfiguration config = ((org.springframework.web.cors.UrlBasedCorsConfigurationSource) source)
                .getCorsConfigurations().get("/**");
        assertNotNull(config);
        assertTrue(config.getAllowedOrigins().contains("http://localhost:3000"));
        assertTrue(config.getAllowedMethods().contains("GET"));
        assertTrue(config.getAllowedMethods().contains("POST"));
        assertTrue(config.getAllowedHeaders().contains("Authorization"));
        assertTrue(config.getExposedHeaders().contains("Authorization"));
        assertTrue(config.getAllowCredentials());
        assertEquals(3600L, config.getMaxAge());
    }

    @Test
    void corsConfigurationSource_multipleOrigins() throws Exception {
        CorsConfig corsConfig = new CorsConfig();
        Field field = CorsConfig.class.getDeclaredField("allowedOrigins");
        field.setAccessible(true);
        field.set(corsConfig, "http://localhost:3000,https://app.codeops.dev");

        CorsConfigurationSource source = corsConfig.corsConfigurationSource();
        CorsConfiguration config = ((org.springframework.web.cors.UrlBasedCorsConfigurationSource) source)
                .getCorsConfigurations().get("/**");
        assertEquals(2, config.getAllowedOrigins().size());
    }
}
