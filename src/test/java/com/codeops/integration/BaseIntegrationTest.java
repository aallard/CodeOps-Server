package com.codeops.integration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("integration")
@Import(TestRateLimitConfig.class)
public abstract class BaseIntegrationTest {

    static final PostgreSQLContainer<?> POSTGRES;

    static {
        POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
                .withDatabaseName("codeops_test")
                .withUsername("test")
                .withPassword("test");
        POSTGRES.start();
    }

    private static final AtomicInteger COUNTER = new AtomicInteger(0);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    protected TestRestTemplate restTemplate;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    // === Records ===

    protected record AuthResult(String token, String refreshToken, UUID userId) {}

    protected record TestSetup(String token, UUID userId, UUID teamId, String email, String password) {}

    // === Auth Helpers ===

    @SuppressWarnings("unchecked")
    protected AuthResult registerUser(String email, String password, String displayName) {
        var body = Map.of("email", email, "password", password, "displayName", displayName);
        ResponseEntity<Map> response = restTemplate.postForEntity("/api/v1/auth/register", body, Map.class);
        return parseAuthResult(response.getBody());
    }

    @SuppressWarnings("unchecked")
    protected AuthResult loginUser(String email, String password) {
        var body = Map.of("email", email, "password", password);
        ResponseEntity<Map> response = restTemplate.postForEntity("/api/v1/auth/login", body, Map.class);
        return parseAuthResult(response.getBody());
    }

    protected HttpHeaders authHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    // === Entity Creation Helpers ===

    @SuppressWarnings("unchecked")
    protected UUID createTeam(String token, String name) {
        var body = Map.of("name", name);
        HttpEntity<?> entity = new HttpEntity<>(body, authHeaders(token));
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/teams", HttpMethod.POST, entity, Map.class);
        return UUID.fromString((String) response.getBody().get("id"));
    }

    @SuppressWarnings("unchecked")
    protected UUID createProject(String token, UUID teamId, String name) {
        var body = Map.of("name", name);
        HttpEntity<?> entity = new HttpEntity<>(body, authHeaders(token));
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/projects/" + teamId, HttpMethod.POST, entity, Map.class);
        return UUID.fromString((String) response.getBody().get("id"));
    }

    @SuppressWarnings("unchecked")
    protected UUID createJob(String token, UUID projectId) {
        var body = Map.of("projectId", projectId, "mode", "AUDIT",
                "name", "Test Job " + COUNTER.incrementAndGet());
        HttpEntity<?> entity = new HttpEntity<>(body, authHeaders(token));
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/jobs", HttpMethod.POST, entity, Map.class);
        return UUID.fromString((String) response.getBody().get("id"));
    }

    @SuppressWarnings("unchecked")
    protected UUID createFinding(String token, UUID jobId) {
        var body = Map.of(
                "jobId", jobId, "agentType", "SECURITY", "severity", "HIGH",
                "title", "Test Finding " + COUNTER.incrementAndGet(),
                "description", "Test finding description");
        HttpEntity<?> entity = new HttpEntity<>(body, authHeaders(token));
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/findings", HttpMethod.POST, entity, Map.class);
        return UUID.fromString((String) response.getBody().get("id"));
    }

    @SuppressWarnings("unchecked")
    protected UUID createDependencyScan(String token, UUID projectId) {
        var body = Map.of("projectId", projectId, "manifestFile", "pom.xml",
                "totalDependencies", 10, "outdatedCount", 2, "vulnerableCount", 1);
        HttpEntity<?> entity = new HttpEntity<>(body, authHeaders(token));
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/dependencies/scans", HttpMethod.POST, entity, Map.class);
        return UUID.fromString((String) response.getBody().get("id"));
    }

    // === Setup Helpers ===

    protected TestSetup setupOwner() {
        String email = uniqueEmail("owner");
        String password = "Test@123456";
        AuthResult reg = registerUser(email, password, "Test Owner " + COUNTER.get());
        UUID teamId = createTeam(reg.token(), "Team " + COUNTER.incrementAndGet());
        AuthResult login = loginUser(email, password);
        return new TestSetup(login.token(), reg.userId(), teamId, email, password);
    }

    protected TestSetup setupMember(UUID teamId, String ownerToken) {
        String email = uniqueEmail("member");
        String password = "Test@123456";
        AuthResult reg = registerUser(email, password, "Test Member " + COUNTER.get());

        // Owner invites member
        var inviteBody = Map.of("email", email, "role", "MEMBER");
        HttpEntity<?> inviteEntity = new HttpEntity<>(inviteBody, authHeaders(ownerToken));
        restTemplate.exchange("/api/v1/teams/" + teamId + "/invitations",
                HttpMethod.POST, inviteEntity, Map.class);

        // Get invitation token from DB
        String invToken = jdbcTemplate.queryForObject(
                "SELECT token FROM invitations WHERE team_id = ? AND email = ? AND status = 'PENDING'",
                String.class, teamId, email);

        // Accept invitation as the new member
        HttpEntity<?> acceptEntity = new HttpEntity<>(null, authHeaders(reg.token()));
        restTemplate.exchange("/api/v1/teams/invitations/" + invToken + "/accept",
                HttpMethod.POST, acceptEntity, Map.class);

        // Re-login to get MEMBER role in JWT
        AuthResult login = loginUser(email, password);
        return new TestSetup(login.token(), reg.userId(), teamId, email, password);
    }

    // === Utility ===

    protected String uniqueEmail(String prefix) {
        return prefix + COUNTER.incrementAndGet() + "@test.com";
    }

    protected void waitForAsync() {
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @SuppressWarnings("unchecked")
    private AuthResult parseAuthResult(Map body) {
        String token = (String) body.get("token");
        String refreshToken = (String) body.get("refreshToken");
        Map<String, Object> userMap = (Map<String, Object>) body.get("user");
        UUID userId = UUID.fromString((String) userMap.get("id"));
        return new AuthResult(token, refreshToken, userId);
    }
}
