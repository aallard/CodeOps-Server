package com.codeops.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.*;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("unchecked")
class AdminControllerIT extends BaseIntegrationTest {

    @Test
    void getUsageStats_asOwner_returnsStats() {
        TestSetup owner = setupOwner();

        HttpEntity<?> entity = new HttpEntity<>(null, authHeaders(owner.token()));
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/admin/usage", HttpMethod.GET, entity, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> stats = response.getBody();
        assertThat(stats).isNotNull();
        assertThat(stats).containsKeys("totalUsers", "activeUsers", "totalTeams", "totalProjects", "totalJobs");
        assertThat(((Number) stats.get("totalUsers")).longValue()).isGreaterThanOrEqualTo(1L);
        assertThat(((Number) stats.get("activeUsers")).longValue()).isGreaterThanOrEqualTo(1L);
        assertThat(((Number) stats.get("totalTeams")).longValue()).isGreaterThanOrEqualTo(1L);
    }

    @Test
    void getUsageStats_asNonAdmin_returns403() {
        String email = uniqueEmail("nonadmin");
        AuthResult reg = registerUser(email, "Test@123456", "Non-Admin User");

        HttpEntity<?> entity = new HttpEntity<>(null, authHeaders(reg.token()));
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/admin/usage", HttpMethod.GET, entity, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void getUserById_asOwner_returnsUser() {
        TestSetup owner = setupOwner();

        HttpEntity<?> entity = new HttpEntity<>(null, authHeaders(owner.token()));
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/admin/users/" + owner.userId(), HttpMethod.GET, entity, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> user = response.getBody();
        assertThat(user).isNotNull();
        assertThat(user.get("id")).isEqualTo(owner.userId().toString());
        assertThat(user.get("email")).isEqualTo(owner.email());
        assertThat(user.get("isActive")).isEqualTo(true);
        assertThat(user.get("createdAt")).isNotNull();
    }

    @Test
    void getSystemSettings_asOwner_returnsList() {
        TestSetup owner = setupOwner();

        HttpEntity<?> entity = new HttpEntity<>(null, authHeaders(owner.token()));
        ResponseEntity<List> response = restTemplate.exchange(
                "/api/v1/admin/settings", HttpMethod.GET, entity, List.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        // Settings list may be empty initially, but the response should be a valid list
        assertThat(response.getBody()).isInstanceOf(List.class);
    }

    @Test
    void updateSystemSetting_asOwner_persists() {
        TestSetup owner = setupOwner();

        // Create/update a setting
        var settingBody = Map.of("key", "test.setting", "value", "test-value");
        HttpEntity<?> putEntity = new HttpEntity<>(settingBody, authHeaders(owner.token()));
        ResponseEntity<Map> putResponse = restTemplate.exchange(
                "/api/v1/admin/settings", HttpMethod.PUT, putEntity, Map.class);

        assertThat(putResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> setting = putResponse.getBody();
        assertThat(setting).isNotNull();
        assertThat(setting.get("key")).isEqualTo("test.setting");
        assertThat(setting.get("value")).isEqualTo("test-value");
        assertThat(setting.get("updatedBy")).isEqualTo(owner.userId().toString());
        assertThat(setting.get("updatedAt")).isNotNull();

        // Verify by fetching the setting by key
        HttpEntity<?> getEntity = new HttpEntity<>(null, authHeaders(owner.token()));
        ResponseEntity<Map> getResponse = restTemplate.exchange(
                "/api/v1/admin/settings/test.setting", HttpMethod.GET, getEntity, Map.class);

        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> fetched = getResponse.getBody();
        assertThat(fetched).isNotNull();
        assertThat(fetched.get("key")).isEqualTo("test.setting");
        assertThat(fetched.get("value")).isEqualTo("test-value");
    }

    @Test
    void getAuditLog_asOwner_returnsPaginatedResults() {
        TestSetup owner = setupOwner();

        // Perform some action that generates audit logs (create a project)
        createProject(owner.token(), owner.teamId(), "Audit Log Test Project");

        // Wait for async audit log writes
        waitForAsync();

        HttpEntity<?> entity = new HttpEntity<>(null, authHeaders(owner.token()));
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/admin/audit-log/user/" + owner.userId() + "?page=0&size=10",
                HttpMethod.GET, entity, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> pageBody = response.getBody();
        assertThat(pageBody).isNotNull();
        // Spring Data Page serialization
        assertThat(pageBody).containsKeys("content", "totalElements", "totalPages");
        List<Map<String, Object>> content = (List<Map<String, Object>>) pageBody.get("content");
        assertThat(content).isNotNull();
    }
}
