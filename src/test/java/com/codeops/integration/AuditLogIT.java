package com.codeops.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AuditLogIT extends BaseIntegrationTest {

    @Test
    void register_writesAuditLog() {
        String email = uniqueEmail("audit-reg");
        registerUser(email, "Test@123456", "Audit Reg User");
        waitForAsync();

        UUID userId = jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE email = ?", UUID.class, email);

        List<Map<String, Object>> logs = jdbcTemplate.queryForList(
                "SELECT * FROM audit_log WHERE entity_type = ? AND action = ? AND entity_id = ? ORDER BY created_at DESC",
                "USER", "USER_REGISTERED", userId);

        assertThat(logs).isNotEmpty();
    }

    @Test
    void login_writesAuditLog() {
        String email = uniqueEmail("audit-login");
        registerUser(email, "Test@123456", "Audit Login User");
        waitForAsync();

        loginUser(email, "Test@123456");
        waitForAsync();

        UUID userId = jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE email = ?", UUID.class, email);

        List<Map<String, Object>> logs = jdbcTemplate.queryForList(
                "SELECT * FROM audit_log WHERE entity_type = ? AND action = ? AND entity_id = ? ORDER BY created_at DESC",
                "USER", "USER_LOGIN", userId);

        assertThat(logs).isNotEmpty();
    }

    @Test
    void createTeam_writesAuditLog() {
        String email = uniqueEmail("audit-team");
        AuthResult auth = registerUser(email, "Test@123456", "Audit Team User");
        waitForAsync();

        UUID teamId = createTeam(auth.token(), "Audit Team Test");
        waitForAsync();

        List<Map<String, Object>> logs = jdbcTemplate.queryForList(
                "SELECT * FROM audit_log WHERE entity_type = ? AND action = ? AND entity_id = ? ORDER BY created_at DESC",
                "TEAM", "TEAM_CREATED", teamId);

        assertThat(logs).isNotEmpty();
        Map<String, Object> entry = logs.get(0);
        assertThat(entry.get("team_id")).isEqualTo(teamId);
    }

    @Test
    void createProject_writesAuditLog() {
        TestSetup setup = setupOwner();
        UUID projectId = createProject(setup.token(), setup.teamId(), "Audit Project Test");
        waitForAsync();

        List<Map<String, Object>> logs = jdbcTemplate.queryForList(
                "SELECT * FROM audit_log WHERE entity_type = ? AND action = ? AND entity_id = ? ORDER BY created_at DESC",
                "PROJECT", "PROJECT_CREATED", projectId);

        assertThat(logs).isNotEmpty();
    }

    @Test
    void deleteProject_writesAuditLog() {
        TestSetup setup = setupOwner();
        UUID projectId = createProject(setup.token(), setup.teamId(), "Project To Delete For Audit");
        waitForAsync();

        HttpEntity<?> entity = new HttpEntity<>(authHeaders(setup.token()));
        ResponseEntity<Void> response = restTemplate.exchange(
                "/api/v1/projects/" + projectId, HttpMethod.DELETE, entity, Void.class);
        assertThat(response.getStatusCode().value()).isEqualTo(204);
        waitForAsync();

        List<Map<String, Object>> logs = jdbcTemplate.queryForList(
                "SELECT * FROM audit_log WHERE entity_type = ? AND action = ? AND entity_id = ? ORDER BY created_at DESC",
                "PROJECT", "PROJECT_DELETED", projectId);

        assertThat(logs).isNotEmpty();
    }

    @Test
    void auditLogEntries_haveCorrectFields() {
        String email = uniqueEmail("audit-fields");
        AuthResult auth = registerUser(email, "Test@123456", "Audit Fields User");
        waitForAsync();

        UUID userId = auth.userId();

        List<Map<String, Object>> logs = jdbcTemplate.queryForList(
                "SELECT * FROM audit_log WHERE entity_type = ? AND action = ? AND entity_id = ? ORDER BY created_at DESC",
                "USER", "USER_REGISTERED", userId);

        assertThat(logs).isNotEmpty();
        Map<String, Object> entry = logs.get(0);
        assertThat(entry.get("user_id")).isEqualTo(userId);
        assertThat(entry.get("action")).isEqualTo("USER_REGISTERED");
        assertThat(entry.get("entity_type")).isEqualTo("USER");
        assertThat(entry.get("entity_id")).isEqualTo(userId);
        assertThat(entry.get("created_at")).isNotNull();
    }
}
