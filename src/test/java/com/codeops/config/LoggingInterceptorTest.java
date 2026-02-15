package com.codeops.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link LoggingInterceptor}.
 *
 * <p>Verifies that the interceptor records start time, computes duration,
 * enriches MDC with authenticated user context, and does not interfere
 * with the request lifecycle.</p>
 */
class LoggingInterceptorTest {

    private LoggingInterceptor interceptor;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        interceptor = new LoggingInterceptor();
        request = new MockHttpServletRequest("GET", "/api/v1/projects");
        response = new MockHttpServletResponse();
        SecurityContextHolder.clearContext();
        MDC.clear();
    }

    /**
     * Verifies that preHandle records start time as a request attribute.
     */
    @Test
    void preHandleShouldRecordStartTime() {
        interceptor.preHandle(request, response, new Object());

        Object startTime = request.getAttribute(LoggingInterceptor.START_TIME_ATTR);
        assertNotNull(startTime, "Start time should be set as request attribute");
        assertInstanceOf(Long.class, startTime);
        assertTrue((Long) startTime > 0);
    }

    /**
     * Verifies that afterCompletion computes a non-negative duration when start time
     * was recorded by preHandle.
     */
    @Test
    void afterCompletionShouldComputeDuration() {
        // Simulate preHandle recording start time
        request.setAttribute(LoggingInterceptor.START_TIME_ATTR, System.currentTimeMillis() - 50);
        response.setStatus(200);

        // Should not throw — duration is computed and logged
        assertDoesNotThrow(() ->
            interceptor.afterCompletion(request, response, new Object(), null)
        );
    }

    /**
     * Verifies that MDC is enriched with userId when SecurityContext has an
     * authenticated principal.
     */
    @Test
    void preHandleShouldEnrichMdcWithUserId() {
        UUID userId = UUID.randomUUID();
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(userId, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);

        interceptor.preHandle(request, response, new Object());

        assertEquals(userId.toString(), MDC.get("userId"));
    }

    /**
     * Verifies that MDC is not polluted when no authenticated user is present.
     */
    @Test
    void preHandleShouldNotSetUserIdWhenUnauthenticated() {
        interceptor.preHandle(request, response, new Object());

        assertNull(MDC.get("userId"));
    }

    /**
     * Verifies that MDC is enriched with teamId from X-Team-ID header.
     */
    @Test
    void preHandleShouldEnrichMdcWithTeamId() {
        String teamId = UUID.randomUUID().toString();
        request.addHeader("X-Team-ID", teamId);

        interceptor.preHandle(request, response, new Object());

        assertEquals(teamId, MDC.get("teamId"));
    }

    /**
     * Verifies that WARN-level logging occurs for 4xx responses
     * (by confirming afterCompletion completes without error for a 404 status).
     */
    @Test
    void afterCompletionShouldHandleClientErrorStatus() {
        request.setAttribute(LoggingInterceptor.START_TIME_ATTR, System.currentTimeMillis());
        response.setStatus(404);

        assertDoesNotThrow(() ->
            interceptor.afterCompletion(request, response, new Object(), null)
        );
    }

    /**
     * Verifies that ERROR-level logging occurs for 5xx responses
     * (by confirming afterCompletion completes without error for a 500 status).
     */
    @Test
    void afterCompletionShouldHandleServerErrorStatus() {
        request.setAttribute(LoggingInterceptor.START_TIME_ATTR, System.currentTimeMillis());
        response.setStatus(500);

        assertDoesNotThrow(() ->
            interceptor.afterCompletion(request, response, new Object(), null)
        );
    }

    /**
     * Verifies that afterCompletion handles missing start time gracefully.
     */
    @Test
    void afterCompletionShouldHandleMissingStartTime() {
        response.setStatus(200);

        assertDoesNotThrow(() ->
            interceptor.afterCompletion(request, response, new Object(), null)
        );
    }
}
