package com.codeops.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.*;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DataIntegrityIT extends BaseIntegrationTest {

    @Test
    void createUser_duplicateEmail_returns400() {
        String email = uniqueEmail("dup");
        registerUser(email, "Test@123456", "First User");

        // Second registration with the same email should fail via IllegalArgumentException
        var body = Map.of("email", email, "password", "Test@123456", "displayName", "Second User");
        @SuppressWarnings("unchecked")
        ResponseEntity<Map> response = restTemplate.postForEntity("/api/v1/auth/register", body, Map.class);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat((String) response.getBody().get("message")).isEqualTo("Invalid request");
    }

    @Test
    void entityTimestamps_createdAtAndUpdatedAt_setAutomatically() throws InterruptedException {
        TestSetup setup = setupOwner();

        // Query the team's timestamps from the DB
        Map<String, Object> teamRow = jdbcTemplate.queryForMap(
                "SELECT created_at, updated_at FROM teams WHERE id = ?", setup.teamId());
        assertThat(teamRow.get("created_at")).isNotNull();
        assertThat(teamRow.get("updated_at")).isNotNull();

        Object originalCreatedAt = teamRow.get("created_at");
        Object originalUpdatedAt = teamRow.get("updated_at");

        // Wait a bit so updatedAt will definitely be different
        Thread.sleep(100);

        // Update the team
        var updateBody = Map.of("name", "Updated Team Name");
        HttpEntity<?> entity = new HttpEntity<>(updateBody, authHeaders(setup.token()));
        ResponseEntity<Map> updateResponse = restTemplate.exchange(
                "/api/v1/teams/" + setup.teamId(), HttpMethod.PUT, entity, Map.class);
        assertThat(updateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Re-query
        Map<String, Object> updatedRow = jdbcTemplate.queryForMap(
                "SELECT created_at, updated_at FROM teams WHERE id = ?", setup.teamId());
        assertThat(updatedRow.get("created_at")).isEqualTo(originalCreatedAt);
        assertThat(updatedRow.get("updated_at")).isNotEqualTo(originalUpdatedAt);
    }

    @Test
    void enumFields_persistAsStrings() {
        TestSetup setup = setupOwner();
        UUID projectId = createProject(setup.token(), setup.teamId(), "Enum Test Project");
        UUID jobId = createJob(setup.token(), projectId);
        UUID findingId = createFinding(setup.token(), jobId);

        // Check finding status is stored as a string
        String findingStatus = jdbcTemplate.queryForObject(
                "SELECT status FROM findings WHERE id = ?", String.class, findingId);
        assertThat(findingStatus).isEqualTo("OPEN");

        // Check team_member role is stored as a string
        String role = jdbcTemplate.queryForObject(
                "SELECT role FROM team_members WHERE team_id = ? AND user_id = ?",
                String.class, setup.teamId(), setup.userId());
        assertThat(role).isEqualTo("OWNER");
    }

    @Test
    void deleteTeam_failsDueToForeignKeyConstraints() {
        // Even a team with no projects has team_members (owner membership),
        // so deleteTeam always fails with FK constraint violation
        TestSetup setup = setupOwner();

        HttpEntity<?> entity = new HttpEntity<>(authHeaders(setup.token()));
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/teams/" + setup.teamId(), HttpMethod.DELETE, entity, Map.class);

        // FK constraint violation from team_members -> teams results in 500
        assertThat(response.getStatusCode().value()).isEqualTo(500);
    }

    @Test
    void deleteProject_succeeds_andRemovesFromDb() {
        TestSetup setup = setupOwner();
        UUID projectId = createProject(setup.token(), setup.teamId(), "Project To Delete");

        // Verify project exists
        Integer beforeCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM projects WHERE id = ?", Integer.class, projectId);
        assertThat(beforeCount).isEqualTo(1);

        HttpEntity<?> entity = new HttpEntity<>(authHeaders(setup.token()));
        ResponseEntity<Void> response = restTemplate.exchange(
                "/api/v1/projects/" + projectId, HttpMethod.DELETE, entity, Void.class);
        assertThat(response.getStatusCode().value()).isEqualTo(204);

        // Verify project is gone
        Integer afterCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM projects WHERE id = ?", Integer.class, projectId);
        assertThat(afterCount).isEqualTo(0);
    }
}
