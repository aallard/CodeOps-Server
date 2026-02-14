package com.codeops.integration;

import com.codeops.security.RateLimitFilter;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@SuppressWarnings("unchecked")
class SecurityIT extends BaseIntegrationTest {

    private static final String JWT_SECRET = "integration-test-secret-key-minimum-32-characters-long-for-hs256";

    @Test
    void unauthenticatedRequest_toProtectedEndpoint_returns401() {
        ResponseEntity<Map> response = restTemplate.getForEntity("/api/v1/teams", Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void expiredToken_returns401() {
        String expiredToken = Jwts.builder()
                .subject(UUID.randomUUID().toString())
                .claim("email", "test@test.com")
                .claim("roles", List.of())
                .claim("jti", UUID.randomUUID().toString())
                .issuedAt(Date.from(Instant.now().minus(2, ChronoUnit.HOURS)))
                .expiration(Date.from(Instant.now().minus(1, ChronoUnit.HOURS)))
                .signWith(Keys.hmacShaKeyFor(JWT_SECRET.getBytes()), Jwts.SIG.HS256)
                .compact();

        HttpEntity<?> entity = new HttpEntity<>(null, authHeaders(expiredToken));
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/teams", HttpMethod.GET, entity, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void malformedToken_returns401() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer not.a.valid.jwt");
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<?> entity = new HttpEntity<>(null, headers);
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/teams", HttpMethod.GET, entity, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void validToken_toProtectedEndpoint_returns200() {
        String email = uniqueEmail("secvalid");
        AuthResult reg = registerUser(email, "Test@123456", "Security Valid Test");

        HttpEntity<?> entity = new HttpEntity<>(null, authHeaders(reg.token()));
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/users/me", HttpMethod.GET, entity, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("email")).isEqualTo(email);
    }

    @Test
    void adminEndpoint_withNonAdminToken_returns403() {
        // Register a user but don't create a team (no roles)
        String email = uniqueEmail("nonadmin");
        AuthResult reg = registerUser(email, "Test@123456", "Non-Admin Test");

        HttpEntity<?> entity = new HttpEntity<>(null, authHeaders(reg.token()));
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/admin/usage", HttpMethod.GET, entity, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void adminEndpoint_withOwnerToken_returns200() {
        TestSetup owner = setupOwner();

        HttpEntity<?> entity = new HttpEntity<>(null, authHeaders(owner.token()));
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/admin/usage", HttpMethod.GET, entity, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void rateLimiting_exceedLimit_returns429() throws Exception {
        // The real RateLimitFilter is replaced by a no-op in BaseIntegrationTest,
        // so we instantiate a testable subclass that calls doFilterInternal directly,
        // bypassing OncePerRequestFilter's dispatch-type checking.
        var testFilter = new RateLimitFilter() {
            public void invokeFilter(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
                    throws ServletException, IOException {
                doFilterInternal(req, res, chain);
            }
        };
        FilterChain chain = mock(FilterChain.class);
        String clientIp = "192.168.99.99";

        // Make 10 requests that should all pass through to the chain
        for (int i = 0; i < 10; i++) {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
            request.addHeader("X-Forwarded-For", clientIp);
            MockHttpServletResponse response = new MockHttpServletResponse();
            testFilter.invokeFilter(request, response, chain);
            assertThat(response.getStatus()).isNotEqualTo(429);
        }

        // The 11th request should be rate-limited
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        request.addHeader("X-Forwarded-For", clientIp);
        MockHttpServletResponse response = new MockHttpServletResponse();
        testFilter.invokeFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(429);
    }

    @Test
    void healthEndpoint_noAuth_returns200() {
        ResponseEntity<Map> response = restTemplate.getForEntity("/api/v1/health", Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("status")).isEqualTo("UP");
    }
}
