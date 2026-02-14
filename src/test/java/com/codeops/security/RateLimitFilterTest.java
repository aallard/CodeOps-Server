package com.codeops.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateLimitFilterTest {

    @Mock
    private FilterChain filterChain;

    private RateLimitFilter rateLimitFilter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        rateLimitFilter = new RateLimitFilter();
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @Test
    void nonAuthRequest_passesThrough() throws Exception {
        request.setRequestURI("/api/v1/projects");
        rateLimitFilter.doFilterInternal(request, response, filterChain);
        verify(filterChain).doFilter(request, response);
        assertEquals(200, response.getStatus());
    }

    @Test
    void authRequest_underLimit_passesThrough() throws Exception {
        request.setRequestURI("/api/v1/auth/login");
        request.setRemoteAddr("192.168.1.1");
        rateLimitFilter.doFilterInternal(request, response, filterChain);
        verify(filterChain).doFilter(request, response);
        assertEquals(200, response.getStatus());
    }

    @Test
    void authRequest_overLimit_returns429() throws Exception {
        for (int i = 0; i < 11; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest();
            req.setRequestURI("/api/v1/auth/login");
            req.setRemoteAddr("10.0.0.1");
            MockHttpServletResponse resp = new MockHttpServletResponse();
            rateLimitFilter.doFilterInternal(req, resp, filterChain);
            if (i < 10) {
                assertNotEquals(429, resp.getStatus(), "Request " + i + " should pass");
            } else {
                assertEquals(429, resp.getStatus(), "Request " + i + " should be rate limited");
                assertTrue(resp.getContentAsString().contains("Rate limit exceeded"));
            }
        }
    }

    @Test
    void xForwardedFor_usesFirstIp() throws Exception {
        request.setRequestURI("/api/v1/auth/login");
        request.addHeader("X-Forwarded-For", "203.0.113.1, 10.0.0.1");
        rateLimitFilter.doFilterInternal(request, response, filterChain);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void differentIps_haveSeparateLimits() throws Exception {
        for (int i = 0; i < 10; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest();
            req.setRequestURI("/api/v1/auth/login");
            req.setRemoteAddr("172.16.0.1");
            MockHttpServletResponse resp = new MockHttpServletResponse();
            rateLimitFilter.doFilterInternal(req, resp, filterChain);
        }

        MockHttpServletRequest req2 = new MockHttpServletRequest();
        req2.setRequestURI("/api/v1/auth/login");
        req2.setRemoteAddr("172.16.0.2");
        MockHttpServletResponse resp2 = new MockHttpServletResponse();
        rateLimitFilter.doFilterInternal(req2, resp2, filterChain);
        assertNotEquals(429, resp2.getStatus());
    }
}
