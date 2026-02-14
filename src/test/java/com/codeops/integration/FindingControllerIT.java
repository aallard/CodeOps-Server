package com.codeops.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("unchecked")
class FindingControllerIT extends BaseIntegrationTest {

    @Test
    void createFinding_withJob_createsInDB() {
        TestSetup owner = setupOwner();
        UUID projectId = createProject(owner.token(), owner.teamId(), "Finding Test Project");
        UUID jobId = createJob(owner.token(), projectId);

        var body = Map.of(
                "jobId", jobId,
                "agentType", "SECURITY",
                "severity", "HIGH",
                "title", "SQL Injection Vulnerability",
                "description", "Unsanitized input in query",
                "filePath", "src/main/java/Dao.java",
                "lineNumber", 42,
                "recommendation", "Use parameterized queries"
        );
        HttpEntity<?> entity = new HttpEntity<>(body, authHeaders(owner.token()));

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/findings", HttpMethod.POST, entity, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Map<String, Object> finding = response.getBody();
        assertThat(finding).isNotNull();
        assertThat(finding.get("id")).isNotNull();
        assertThat(finding.get("jobId")).isEqualTo(jobId.toString());
        assertThat(finding.get("agentType")).isEqualTo("SECURITY");
        assertThat(finding.get("severity")).isEqualTo("HIGH");
        assertThat(finding.get("title")).isEqualTo("SQL Injection Vulnerability");
        assertThat(finding.get("description")).isEqualTo("Unsanitized input in query");
        assertThat(finding.get("filePath")).isEqualTo("src/main/java/Dao.java");
        assertThat(finding.get("lineNumber")).isEqualTo(42);
        assertThat(finding.get("recommendation")).isEqualTo("Use parameterized queries");
        assertThat(finding.get("status")).isEqualTo("OPEN");
        assertThat(finding.get("createdAt")).isNotNull();

        // Verify in DB
        UUID findingId = UUID.fromString((String) finding.get("id"));
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM findings WHERE id = ?", Integer.class, findingId);
        assertThat(count).isEqualTo(1);
    }

    @Test
    void getFinding_byId_returnsFullDetail() {
        TestSetup owner = setupOwner();
        UUID projectId = createProject(owner.token(), owner.teamId(), "Get Finding Project");
        UUID jobId = createJob(owner.token(), projectId);
        UUID findingId = createFinding(owner.token(), jobId);

        HttpEntity<?> entity = new HttpEntity<>(null, authHeaders(owner.token()));
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/findings/" + findingId, HttpMethod.GET, entity, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> finding = response.getBody();
        assertThat(finding).isNotNull();
        assertThat(finding.get("id")).isEqualTo(findingId.toString());
        assertThat(finding.get("jobId")).isEqualTo(jobId.toString());
        assertThat(finding.get("agentType")).isEqualTo("SECURITY");
        assertThat(finding.get("severity")).isEqualTo("HIGH");
        assertThat(finding.get("title")).isNotNull();
        assertThat(finding.get("status")).isEqualTo("OPEN");
        assertThat(finding.get("createdAt")).isNotNull();
    }

    @Test
    void getFindingsForJob_pagination_works() {
        TestSetup owner = setupOwner();
        UUID projectId = createProject(owner.token(), owner.teamId(), "Pagination Project");
        UUID jobId = createJob(owner.token(), projectId);

        // Create 3 findings
        createFinding(owner.token(), jobId);
        createFinding(owner.token(), jobId);
        createFinding(owner.token(), jobId);

        // Request page 0 with size 2
        HttpEntity<?> entity = new HttpEntity<>(null, authHeaders(owner.token()));
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/findings/job/" + jobId + "?page=0&size=2",
                HttpMethod.GET, entity, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> pageBody = response.getBody();
        assertThat(pageBody).isNotNull();
        List<Map<String, Object>> content = (List<Map<String, Object>>) pageBody.get("content");
        assertThat(content).hasSize(2);
        assertThat(((Number) pageBody.get("totalElements")).longValue()).isEqualTo(3L);
        assertThat(((Number) pageBody.get("totalPages")).intValue()).isEqualTo(2);
        assertThat(pageBody.get("page")).isEqualTo(0);
        assertThat(pageBody.get("size")).isEqualTo(2);
        assertThat(pageBody.get("isLast")).isEqualTo(false);

        // Request page 1
        ResponseEntity<Map> response2 = restTemplate.exchange(
                "/api/v1/findings/job/" + jobId + "?page=1&size=2",
                HttpMethod.GET, entity, Map.class);

        Map<String, Object> pageBody2 = response2.getBody();
        assertThat(pageBody2).isNotNull();
        List<Map<String, Object>> content2 = (List<Map<String, Object>>) pageBody2.get("content");
        assertThat(content2).hasSize(1);
        assertThat(pageBody2.get("isLast")).isEqualTo(true);
    }

    @Test
    void bulkUpdateFindings_updatesAllSpecifiedIds() {
        TestSetup owner = setupOwner();
        UUID projectId = createProject(owner.token(), owner.teamId(), "Bulk Update Project");
        UUID jobId = createJob(owner.token(), projectId);

        UUID findingId1 = createFinding(owner.token(), jobId);
        UUID findingId2 = createFinding(owner.token(), jobId);

        var bulkBody = Map.of(
                "findingIds", List.of(findingId1, findingId2),
                "status", "FIXED"
        );
        HttpEntity<?> entity = new HttpEntity<>(bulkBody, authHeaders(owner.token()));

        ResponseEntity<List> response = restTemplate.exchange(
                "/api/v1/findings/bulk-status", HttpMethod.PUT, entity, List.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<Map<String, Object>> findings = response.getBody();
        assertThat(findings).isNotNull();
        assertThat(findings).hasSize(2);
        assertThat(findings).allSatisfy(f -> {
            assertThat(f.get("status")).isEqualTo("FIXED");
            assertThat(f.get("statusChangedBy")).isNotNull();
            assertThat(f.get("statusChangedAt")).isNotNull();
        });

        // Verify in DB
        Integer fixedCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM findings WHERE id IN (?, ?) AND status = 'FIXED'",
                Integer.class, findingId1, findingId2);
        assertThat(fixedCount).isEqualTo(2);
    }

    @Test
    void getFindingsBySeverity_filtersCorrectly() {
        TestSetup owner = setupOwner();
        UUID projectId = createProject(owner.token(), owner.teamId(), "Severity Filter Project");
        UUID jobId = createJob(owner.token(), projectId);

        // Create a HIGH severity finding (default from helper)
        createFinding(owner.token(), jobId);

        // Create a LOW severity finding
        var lowBody = Map.of(
                "jobId", jobId,
                "agentType", "CODE_QUALITY",
                "severity", "LOW",
                "title", "Minor code smell",
                "description", "Unused variable"
        );
        HttpEntity<?> createEntity = new HttpEntity<>(lowBody, authHeaders(owner.token()));
        restTemplate.exchange("/api/v1/findings", HttpMethod.POST, createEntity, Map.class);

        // Filter by HIGH
        HttpEntity<?> getEntity = new HttpEntity<>(null, authHeaders(owner.token()));
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/findings/job/" + jobId + "/severity/HIGH",
                HttpMethod.GET, getEntity, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> pageBody = response.getBody();
        assertThat(pageBody).isNotNull();
        List<Map<String, Object>> content = (List<Map<String, Object>>) pageBody.get("content");
        assertThat(content).hasSize(1);
        assertThat(content.get(0).get("severity")).isEqualTo("HIGH");
        assertThat(((Number) pageBody.get("totalElements")).longValue()).isEqualTo(1L);
    }
}
