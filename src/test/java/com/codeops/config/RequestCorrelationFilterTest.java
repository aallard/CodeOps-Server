package com.codeops.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link RequestCorrelationFilter}.
 *
 * <p>Verifies that correlation IDs are generated or preserved from incoming headers,
 * that MDC is populated during request processing, and that MDC is always cleaned up
 * after request completion (even on exception).</p>
 */
class RequestCorrelationFilterTest {

    private RequestCorrelationFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        filter = new RequestCorrelationFilter();
        request = new MockHttpServletRequest("GET", "/api/v1/projects");
        response = new MockHttpServletResponse();
    }

    /**
     * Verifies that a UUID correlation ID is generated when no X-Correlation-ID header
     * is present on the incoming request.
     */
    @Test
    void shouldGenerateCorrelationIdWhenHeaderAbsent() throws ServletException, IOException {
        filter.doFilterInternal(request, response, (req, res) -> {
            String correlationId = MDC.get(RequestCorrelationFilter.MDC_CORRELATION_ID);
            assertNotNull(correlationId, "correlationId should be generated");
            assertFalse(correlationId.isBlank(), "correlationId should not be blank");
        });

        String responseHeader = response.getHeader(RequestCorrelationFilter.CORRELATION_ID_HEADER);
        assertNotNull(responseHeader, "Response should contain X-Correlation-ID header");
    }

    /**
     * Verifies that an incoming X-Correlation-ID header value is preserved and used
     * instead of generating a new one.
     */
    @Test
    void shouldPreserveIncomingCorrelationId() throws ServletException, IOException {
        String incomingId = "test-correlation-id-12345";
        request.addHeader(RequestCorrelationFilter.CORRELATION_ID_HEADER, incomingId);

        filter.doFilterInternal(request, response, (req, res) -> {
            assertEquals(incomingId, MDC.get(RequestCorrelationFilter.MDC_CORRELATION_ID));
        });

        assertEquals(incomingId, response.getHeader(RequestCorrelationFilter.CORRELATION_ID_HEADER));
    }

    /**
     * Verifies that the correlation ID is set on the response header.
     */
    @Test
    void shouldSetCorrelationIdOnResponseHeader() throws ServletException, IOException {
        filter.doFilterInternal(request, response, (req, res) -> {});

        String responseHeader = response.getHeader(RequestCorrelationFilter.CORRELATION_ID_HEADER);
        assertNotNull(responseHeader, "Response should have X-Correlation-ID header");
        assertFalse(responseHeader.isBlank());
    }

    /**
     * Verifies that MDC is populated with correlationId, requestPath, and requestMethod
     * during request processing.
     */
    @Test
    void shouldPopulateMdcDuringProcessing() throws ServletException, IOException {
        filter.doFilterInternal(request, response, (req, res) -> {
            assertNotNull(MDC.get(RequestCorrelationFilter.MDC_CORRELATION_ID));
            assertEquals("/api/v1/projects", MDC.get(RequestCorrelationFilter.MDC_REQUEST_PATH));
            assertEquals("GET", MDC.get(RequestCorrelationFilter.MDC_REQUEST_METHOD));
        });
    }

    /**
     * Verifies that MDC is cleared after request processing completes normally.
     */
    @Test
    void shouldClearMdcAfterProcessing() throws ServletException, IOException {
        filter.doFilterInternal(request, response, (req, res) -> {
            // MDC should be populated here
            assertNotNull(MDC.get(RequestCorrelationFilter.MDC_CORRELATION_ID));
        });

        // After filter completes, MDC should be clean
        assertNull(MDC.get(RequestCorrelationFilter.MDC_CORRELATION_ID));
        assertNull(MDC.get(RequestCorrelationFilter.MDC_REQUEST_PATH));
        assertNull(MDC.get(RequestCorrelationFilter.MDC_REQUEST_METHOD));
        assertNull(MDC.get("userId"));
        assertNull(MDC.get("teamId"));
    }

    /**
     * Verifies that MDC is cleared even when the filter chain throws an exception.
     */
    @Test
    void shouldClearMdcEvenOnException() {
        FilterChain throwingChain = (req, res) -> {
            throw new ServletException("Test exception");
        };

        assertThrows(ServletException.class, () ->
            filter.doFilterInternal(request, response, throwingChain)
        );

        // MDC must still be cleared
        assertNull(MDC.get(RequestCorrelationFilter.MDC_CORRELATION_ID));
        assertNull(MDC.get(RequestCorrelationFilter.MDC_REQUEST_PATH));
        assertNull(MDC.get(RequestCorrelationFilter.MDC_REQUEST_METHOD));
    }
}
