package com.codeops.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("unchecked")
class JobControllerIT extends BaseIntegrationTest {

    @Test
    void createJob_withValidProject_createsJob() {
        TestSetup owner = setupOwner();
        UUID projectId = createProject(owner.token(), owner.teamId(), "Job Test Project");

        var body = Map.of("projectId", projectId, "mode", "AUDIT", "name", "Test Job");
        HttpEntity<?> entity = new HttpEntity<>(body, authHeaders(owner.token()));
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/jobs", HttpMethod.POST, entity, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Map<String, Object> responseBody = response.getBody();
        assertThat(responseBody).isNotNull();
        assertThat(responseBody.get("id")).isNotNull();
        assertThat(responseBody.get("projectId")).isEqualTo(projectId.toString());
        assertThat(responseBody.get("mode")).isEqualTo("AUDIT");
        assertThat(responseBody.get("status")).isEqualTo("PENDING");
        assertThat(responseBody.get("name")).isEqualTo("Test Job");
        assertThat(responseBody.get("startedBy")).isEqualTo(owner.userId().toString());
    }

    @Test
    void getJob_asTeamMember_returnsJob() {
        TestSetup owner = setupOwner();
        TestSetup member = setupMember(owner.teamId(), owner.token());
        UUID projectId = createProject(owner.token(), owner.teamId(), "Member Job Project");
        UUID jobId = createJob(owner.token(), projectId);

        HttpEntity<?> entity = new HttpEntity<>(authHeaders(member.token()));
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/jobs/" + jobId, HttpMethod.GET, entity, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("id")).isEqualTo(jobId.toString());
    }

    @Test
    void getJobsForProject_pagination_returnsCorrectPage() {
        TestSetup owner = setupOwner();
        UUID projectId = createProject(owner.token(), owner.teamId(), "Pagination Project");
        UUID job1 = createJob(owner.token(), projectId);
        UUID job2 = createJob(owner.token(), projectId);
        UUID job3 = createJob(owner.token(), projectId);

        HttpEntity<?> entity = new HttpEntity<>(authHeaders(owner.token()));
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/jobs/project/" + projectId + "?page=0&size=2",
                HttpMethod.GET, entity, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> responseBody = response.getBody();
        assertThat(responseBody).isNotNull();
        List<Map<String, Object>> content = (List<Map<String, Object>>) responseBody.get("content");
        assertThat(content).hasSize(2);
        assertThat(((Number) responseBody.get("totalElements")).longValue()).isEqualTo(3L);
        assertThat((Integer) responseBody.get("totalPages")).isEqualTo(2);
    }

    @Test
    void updateJob_validStatus_updatesStatus() {
        TestSetup owner = setupOwner();
        UUID projectId = createProject(owner.token(), owner.teamId(), "Update Job Project");
        UUID jobId = createJob(owner.token(), projectId);

        var body = Map.of("status", "RUNNING");
        HttpEntity<?> entity = new HttpEntity<>(body, authHeaders(owner.token()));
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/jobs/" + jobId, HttpMethod.PUT, entity, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("status")).isEqualTo("RUNNING");
    }

    @Test
    void deleteJob_asOwner_returns204() {
        TestSetup owner = setupOwner();
        UUID projectId = createProject(owner.token(), owner.teamId(), "Delete Job Project");
        UUID jobId = createJob(owner.token(), projectId);

        HttpEntity<?> entity = new HttpEntity<>(authHeaders(owner.token()));
        ResponseEntity<Void> response = restTemplate.exchange(
                "/api/v1/jobs/" + jobId, HttpMethod.DELETE, entity, Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // Verify job is gone
        ResponseEntity<Map> getResponse = restTemplate.exchange(
                "/api/v1/jobs/" + jobId, HttpMethod.GET, entity, Map.class);
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void deleteJob_asMember_returns403() {
        TestSetup owner = setupOwner();
        TestSetup member = setupMember(owner.teamId(), owner.token());
        UUID projectId = createProject(owner.token(), owner.teamId(), "Member Delete Job Project");
        UUID jobId = createJob(owner.token(), projectId);

        HttpEntity<?> entity = new HttpEntity<>(authHeaders(member.token()));
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/jobs/" + jobId, HttpMethod.DELETE, entity, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void createAgentRun_underJob_createsRun() {
        TestSetup owner = setupOwner();
        UUID projectId = createProject(owner.token(), owner.teamId(), "Agent Run Project");
        UUID jobId = createJob(owner.token(), projectId);

        var body = Map.of("jobId", jobId, "agentType", "SECURITY");
        HttpEntity<?> entity = new HttpEntity<>(body, authHeaders(owner.token()));
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/jobs/" + jobId + "/agents", HttpMethod.POST, entity, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Map<String, Object> responseBody = response.getBody();
        assertThat(responseBody).isNotNull();
        assertThat(responseBody.get("id")).isNotNull();
        assertThat(responseBody.get("jobId")).isEqualTo(jobId.toString());
        assertThat(responseBody.get("agentType")).isEqualTo("SECURITY");
        assertThat(responseBody.get("status")).isEqualTo("PENDING");
    }

    @Test
    void getAgentRuns_forJob_returnsList() {
        TestSetup owner = setupOwner();
        UUID projectId = createProject(owner.token(), owner.teamId(), "List Agent Runs Project");
        UUID jobId = createJob(owner.token(), projectId);

        // Create two agent runs
        var body1 = Map.of("jobId", jobId, "agentType", "SECURITY");
        HttpEntity<?> entity1 = new HttpEntity<>(body1, authHeaders(owner.token()));
        restTemplate.exchange("/api/v1/jobs/" + jobId + "/agents", HttpMethod.POST, entity1, Map.class);

        var body2 = Map.of("jobId", jobId, "agentType", "CODE_QUALITY");
        HttpEntity<?> entity2 = new HttpEntity<>(body2, authHeaders(owner.token()));
        restTemplate.exchange("/api/v1/jobs/" + jobId + "/agents", HttpMethod.POST, entity2, Map.class);

        // Get agent runs
        HttpEntity<?> getEntity = new HttpEntity<>(authHeaders(owner.token()));
        ResponseEntity<List> response = restTemplate.exchange(
                "/api/v1/jobs/" + jobId + "/agents", HttpMethod.GET, getEntity, List.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<Map<String, Object>> runs = response.getBody();
        assertThat(runs).isNotNull();
        assertThat(runs).hasSize(2);
        assertThat(runs).extracting(r -> r.get("agentType"))
                .containsExactlyInAnyOrder("SECURITY", "CODE_QUALITY");
    }
}
