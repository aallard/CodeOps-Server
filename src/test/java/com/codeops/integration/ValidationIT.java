package com.codeops.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.*;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ValidationIT extends BaseIntegrationTest {

    @SuppressWarnings("unchecked")
    private Map<String, Object> post(String url, Object body, String token) {
        HttpHeaders headers = (token != null) ? authHeaders(token) : new HttpHeaders();
        if (token == null) {
            headers.setContentType(MediaType.APPLICATION_JSON);
        }
        HttpEntity<?> entity = new HttpEntity<>(body, headers);
        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
        return Map.of("status", response.getStatusCode().value(), "body", response.getBody() != null ? response.getBody() : Map.of());
    }

    @Test
    void createTeam_blankName_returns400() {
        AuthResult auth = registerUser(uniqueEmail("val-team1"), "Test@123456", "Val User");

        Map<String, Object> result = post("/api/v1/teams", Map.of("name", ""), auth.token());

        assertThat((Integer) result.get("status")).isEqualTo(400);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) result.get("body");
        assertThat((String) body.get("message")).containsIgnoringCase("name");
    }

    @Test
    void createTeam_missingName_returns400() {
        AuthResult auth = registerUser(uniqueEmail("val-team2"), "Test@123456", "Val User");

        Map<String, Object> result = post("/api/v1/teams", Map.of(), auth.token());

        assertThat((Integer) result.get("status")).isEqualTo(400);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) result.get("body");
        assertThat((String) body.get("message")).containsIgnoringCase("name");
    }

    @Test
    void createProject_blankName_returns400() {
        TestSetup setup = setupOwner();

        Map<String, Object> result = post("/api/v1/projects/" + setup.teamId(),
                Map.of("name", ""), setup.token());

        assertThat((Integer) result.get("status")).isEqualTo(400);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) result.get("body");
        assertThat((String) body.get("message")).containsIgnoringCase("name");
    }

    @Test
    void register_invalidEmail_returns400() {
        Map<String, Object> result = post("/api/v1/auth/register",
                Map.of("email", "notanemail", "password", "Test@123456", "displayName", "Invalid Email User"),
                null);

        assertThat((Integer) result.get("status")).isEqualTo(400);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) result.get("body");
        assertThat((String) body.get("message")).containsIgnoringCase("email");
    }

    @Test
    void register_shortPassword_returns400() {
        Map<String, Object> result = post("/api/v1/auth/register",
                Map.of("email", uniqueEmail("val-short"), "password", "Ab1!", "displayName", "Short Pass User"),
                null);

        assertThat((Integer) result.get("status")).isEqualTo(400);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) result.get("body");
        assertThat((String) body.get("message")).containsIgnoringCase("password");
    }

    @Test
    void register_blankDisplayName_returns400() {
        Map<String, Object> result = post("/api/v1/auth/register",
                Map.of("email", uniqueEmail("val-name"), "password", "Test@123456", "displayName", ""),
                null);

        assertThat((Integer) result.get("status")).isEqualTo(400);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) result.get("body");
        assertThat((String) body.get("message")).containsIgnoringCase("displayName");
    }

    @Test
    void createFinding_missingTitle_returns400() {
        TestSetup setup = setupOwner();
        UUID projectId = createProject(setup.token(), setup.teamId(), "Val Finding Project");
        UUID jobId = createJob(setup.token(), projectId);

        // Missing title field entirely
        Map<String, Object> result = post("/api/v1/findings",
                Map.of("jobId", jobId, "agentType", "SECURITY", "severity", "HIGH",
                        "description", "No title finding"),
                setup.token());

        assertThat((Integer) result.get("status")).isEqualTo(400);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) result.get("body");
        assertThat((String) body.get("message")).containsIgnoringCase("title");
    }
}
