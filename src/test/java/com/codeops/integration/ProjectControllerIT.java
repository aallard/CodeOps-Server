package com.codeops.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("unchecked")
class ProjectControllerIT extends BaseIntegrationTest {

    @Test
    void createProject_withTeam_createsProjectLinkedToTeam() {
        TestSetup owner = setupOwner();

        var body = Map.of("name", "Test Project", "description", "A test project");
        HttpEntity<?> entity = new HttpEntity<>(body, authHeaders(owner.token()));
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/projects/" + owner.teamId(), HttpMethod.POST, entity, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Map<String, Object> responseBody = response.getBody();
        assertThat(responseBody).isNotNull();
        assertThat(responseBody.get("id")).isNotNull();
        assertThat(responseBody.get("name")).isEqualTo("Test Project");
        assertThat(responseBody.get("description")).isEqualTo("A test project");
        assertThat(responseBody.get("teamId")).isEqualTo(owner.teamId().toString());
        assertThat(responseBody.get("isArchived")).isEqualTo(false);
    }

    @Test
    void getProjects_returnsProjectsForTeam() {
        TestSetup owner = setupOwner();
        UUID project1Id = createProject(owner.token(), owner.teamId(), "Project One");
        UUID project2Id = createProject(owner.token(), owner.teamId(), "Project Two");

        HttpEntity<?> entity = new HttpEntity<>(authHeaders(owner.token()));
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/projects/team/" + owner.teamId() + "?page=0&size=20",
                HttpMethod.GET, entity, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> responseBody = response.getBody();
        assertThat(responseBody).isNotNull();
        List<Map<String, Object>> content = (List<Map<String, Object>>) responseBody.get("content");
        assertThat(content).hasSizeGreaterThanOrEqualTo(2);
        assertThat(content).extracting(p -> p.get("id"))
                .contains(project1Id.toString(), project2Id.toString());
        assertThat(responseBody.get("totalElements")).isNotNull();
    }

    @Test
    void getProject_asTeamMember_returnsProject() {
        TestSetup owner = setupOwner();
        TestSetup member = setupMember(owner.teamId(), owner.token());
        UUID projectId = createProject(owner.token(), owner.teamId(), "Member Project");

        HttpEntity<?> entity = new HttpEntity<>(authHeaders(member.token()));
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/projects/" + projectId, HttpMethod.GET, entity, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("id")).isEqualTo(projectId.toString());
        assertThat(response.getBody().get("name")).isEqualTo("Member Project");
    }

    @Test
    void getProject_asNonMember_returns403() {
        TestSetup owner = setupOwner();
        UUID projectId = createProject(owner.token(), owner.teamId(), "Secret Project");

        String nonMemberEmail = uniqueEmail("nonmember");
        AuthResult nonMember = registerUser(nonMemberEmail, "Test@123456", "Non Member");

        HttpEntity<?> entity = new HttpEntity<>(authHeaders(nonMember.token()));
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/projects/" + projectId, HttpMethod.GET, entity, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void updateProject_asAdmin_succeeds() {
        TestSetup owner = setupOwner();
        TestSetup member = setupMember(owner.teamId(), owner.token());

        // Promote member to ADMIN
        var roleBody = Map.of("role", "ADMIN");
        HttpEntity<?> roleEntity = new HttpEntity<>(roleBody, authHeaders(owner.token()));
        restTemplate.exchange(
                "/api/v1/teams/" + owner.teamId() + "/members/" + member.userId() + "/role",
                HttpMethod.PUT, roleEntity, Map.class);

        // Re-login to get updated JWT
        AuthResult adminAuth = loginUser(member.email(), member.password());

        UUID projectId = createProject(owner.token(), owner.teamId(), "Original Name");

        var updateBody = Map.of("name", "Updated Name");
        HttpEntity<?> entity = new HttpEntity<>(updateBody, authHeaders(adminAuth.token()));
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/projects/" + projectId, HttpMethod.PUT, entity, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("name")).isEqualTo("Updated Name");
    }

    @Test
    void updateProject_asMember_returns403() {
        TestSetup owner = setupOwner();
        TestSetup member = setupMember(owner.teamId(), owner.token());
        UUID projectId = createProject(owner.token(), owner.teamId(), "Locked Project");

        var updateBody = Map.of("name", "Hacked Name");
        HttpEntity<?> entity = new HttpEntity<>(updateBody, authHeaders(member.token()));
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/projects/" + projectId, HttpMethod.PUT, entity, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void archiveProject_asOwner_succeeds() {
        TestSetup owner = setupOwner();
        UUID projectId = createProject(owner.token(), owner.teamId(), "To Archive");

        HttpEntity<?> entity = new HttpEntity<>(authHeaders(owner.token()));
        ResponseEntity<Void> response = restTemplate.exchange(
                "/api/v1/projects/" + projectId + "/archive", HttpMethod.PUT, entity, Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Verify project is archived
        ResponseEntity<Map> getResponse = restTemplate.exchange(
                "/api/v1/projects/" + projectId, HttpMethod.GET, entity, Map.class);
        assertThat(getResponse.getBody().get("isArchived")).isEqualTo(true);
    }

    @Test
    void unarchiveProject_asOwner_succeeds() {
        TestSetup owner = setupOwner();
        UUID projectId = createProject(owner.token(), owner.teamId(), "To Unarchive");

        HttpEntity<?> entity = new HttpEntity<>(authHeaders(owner.token()));
        // Archive first
        restTemplate.exchange(
                "/api/v1/projects/" + projectId + "/archive", HttpMethod.PUT, entity, Void.class);

        // Now unarchive
        ResponseEntity<Void> response = restTemplate.exchange(
                "/api/v1/projects/" + projectId + "/unarchive", HttpMethod.PUT, entity, Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Verify project is unarchived
        ResponseEntity<Map> getResponse = restTemplate.exchange(
                "/api/v1/projects/" + projectId, HttpMethod.GET, entity, Map.class);
        assertThat(getResponse.getBody().get("isArchived")).isEqualTo(false);
    }

    @Test
    void deleteProject_asOwner_returns204() {
        TestSetup owner = setupOwner();
        UUID projectId = createProject(owner.token(), owner.teamId(), "To Delete");

        HttpEntity<?> entity = new HttpEntity<>(authHeaders(owner.token()));
        ResponseEntity<Void> response = restTemplate.exchange(
                "/api/v1/projects/" + projectId, HttpMethod.DELETE, entity, Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // Verify project is gone
        ResponseEntity<Map> getResponse = restTemplate.exchange(
                "/api/v1/projects/" + projectId, HttpMethod.GET, entity, Map.class);
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void deleteProject_asMember_returns403() {
        TestSetup owner = setupOwner();
        TestSetup member = setupMember(owner.teamId(), owner.token());
        UUID projectId = createProject(owner.token(), owner.teamId(), "Protected Project");

        HttpEntity<?> entity = new HttpEntity<>(authHeaders(member.token()));
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/projects/" + projectId, HttpMethod.DELETE, entity, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
