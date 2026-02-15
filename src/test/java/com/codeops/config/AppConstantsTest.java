package com.codeops.config;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static org.junit.jupiter.api.Assertions.*;

class AppConstantsTest {

    @Test
    void constantsHaveExpectedValues() {
        assertEquals(50, AppConstants.MAX_TEAM_MEMBERS);
        assertEquals(100, AppConstants.MAX_PROJECTS_PER_TEAM);
        assertEquals(50, AppConstants.MAX_PERSONAS_PER_TEAM);
        assertEquals(20, AppConstants.MAX_DIRECTIVES_PER_PROJECT);
        assertEquals(25, AppConstants.MAX_REPORT_SIZE_MB);
        assertEquals(100, AppConstants.MAX_PERSONA_SIZE_KB);
        assertEquals(200, AppConstants.MAX_DIRECTIVE_SIZE_KB);
        assertEquals(50, AppConstants.MAX_SPEC_FILE_SIZE_MB);
        assertEquals(24, AppConstants.JWT_EXPIRY_HOURS);
        assertEquals(30, AppConstants.REFRESH_TOKEN_EXPIRY_DAYS);
        assertEquals(7, AppConstants.INVITATION_EXPIRY_DAYS);
        assertEquals(1, AppConstants.MIN_PASSWORD_LENGTH);
        assertEquals(1, AppConstants.HEALTH_DIGEST_DAY);
        assertEquals(8, AppConstants.HEALTH_DIGEST_HOUR);
        assertEquals("reports/", AppConstants.S3_REPORTS);
        assertEquals("specs/", AppConstants.S3_SPECS);
        assertEquals("personas/", AppConstants.S3_PERSONAS);
        assertEquals("releases/", AppConstants.S3_RELEASES);
        assertEquals(5, AppConstants.MAX_CONCURRENT_AGENTS);
        assertEquals(15, AppConstants.AGENT_TIMEOUT_MINUTES);
        assertEquals(100, AppConstants.DEFAULT_HEALTH_SCORE);
        assertEquals(20, AppConstants.DEFAULT_PAGE_SIZE);
        assertEquals(100, AppConstants.MAX_PAGE_SIZE);
    }

    @Test
    void constructorIsPrivate() throws Exception {
        Constructor<AppConstants> constructor = AppConstants.class.getDeclaredConstructor();
        assertTrue(java.lang.reflect.Modifier.isPrivate(constructor.getModifiers()));
        constructor.setAccessible(true);
        assertNotNull(constructor.newInstance());
    }
}
