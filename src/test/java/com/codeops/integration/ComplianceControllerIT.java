package com.codeops.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("unchecked")
class ComplianceControllerIT extends BaseIntegrationTest {

    @Test
    void createSpecification_createsInDB() {
        TestSetup owner = setupOwner();
        UUID projectId = createProject(owner.token(), owner.teamId(), "Spec Test Project");
        UUID jobId = createJob(owner.token(), projectId);

        var body = Map.of(
                "jobId", jobId,
                "name", "OpenAPI Spec v3",
                "specType", "OPENAPI",
                "s3Key", "specs/openapi-v3.yaml"
        );
        HttpEntity<?> entity = new HttpEntity<>(body, authHeaders(owner.token()));

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/compliance/specs", HttpMethod.POST, entity, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Map<String, Object> spec = response.getBody();
        assertThat(spec).isNotNull();
        assertThat(spec.get("id")).isNotNull();
        assertThat(spec.get("jobId")).isEqualTo(jobId.toString());
        assertThat(spec.get("name")).isEqualTo("OpenAPI Spec v3");
        assertThat(spec.get("specType")).isEqualTo("OPENAPI");
        assertThat(spec.get("s3Key")).isEqualTo("specs/openapi-v3.yaml");
        assertThat(spec.get("createdAt")).isNotNull();

        // Verify in DB
        UUID specId = UUID.fromString((String) spec.get("id"));
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM specifications WHERE id = ?", Integer.class, specId);
        assertThat(count).isEqualTo(1);
    }

    @Test
    void createComplianceItem_linkedToSpec_createsInDB() {
        TestSetup owner = setupOwner();
        UUID projectId = createProject(owner.token(), owner.teamId(), "Compliance Item Project");
        UUID jobId = createJob(owner.token(), projectId);

        // Create a specification first
        var specBody = Map.of(
                "jobId", jobId,
                "name", "API Contract Spec",
                "specType", "OPENAPI",
                "s3Key", "specs/api-contract.yaml"
        );
        HttpEntity<?> specEntity = new HttpEntity<>(specBody, authHeaders(owner.token()));
        ResponseEntity<Map> specResponse = restTemplate.exchange(
                "/api/v1/compliance/specs", HttpMethod.POST, specEntity, Map.class);
        UUID specId = UUID.fromString((String) specResponse.getBody().get("id"));

        // Create a compliance item linked to the spec
        var itemBody = Map.of(
                "jobId", jobId,
                "requirement", "All endpoints must return proper HTTP status codes",
                "specId", specId,
                "status", "MET",
                "evidence", "All 15 endpoints verified",
                "agentType", "API_CONTRACT",
                "notes", "Checked via automated testing"
        );
        HttpEntity<?> itemEntity = new HttpEntity<>(itemBody, authHeaders(owner.token()));

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/compliance/items", HttpMethod.POST, itemEntity, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Map<String, Object> item = response.getBody();
        assertThat(item).isNotNull();
        assertThat(item.get("id")).isNotNull();
        assertThat(item.get("jobId")).isEqualTo(jobId.toString());
        assertThat(item.get("requirement")).isEqualTo("All endpoints must return proper HTTP status codes");
        assertThat(item.get("specId")).isEqualTo(specId.toString());
        assertThat(item.get("specName")).isEqualTo("API Contract Spec");
        assertThat(item.get("status")).isEqualTo("MET");
        assertThat(item.get("evidence")).isEqualTo("All 15 endpoints verified");
        assertThat(item.get("agentType")).isEqualTo("API_CONTRACT");
        assertThat(item.get("notes")).isEqualTo("Checked via automated testing");
        assertThat(item.get("createdAt")).isNotNull();
    }

    @Test
    void batchCreateItems_createsAll() {
        TestSetup owner = setupOwner();
        UUID projectId = createProject(owner.token(), owner.teamId(), "Batch Compliance Project");
        UUID jobId = createJob(owner.token(), projectId);

        var items = List.of(
                Map.of("jobId", jobId, "requirement", "HTTPS required", "status", "MET",
                        "agentType", "SECURITY"),
                Map.of("jobId", jobId, "requirement", "Input validation on all forms", "status", "PARTIAL",
                        "agentType", "SECURITY"),
                Map.of("jobId", jobId, "requirement", "CORS properly configured", "status", "MISSING",
                        "agentType", "SECURITY")
        );
        HttpEntity<?> entity = new HttpEntity<>(items, authHeaders(owner.token()));

        ResponseEntity<List> response = restTemplate.exchange(
                "/api/v1/compliance/items/batch", HttpMethod.POST, entity, List.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        List<Map<String, Object>> created = response.getBody();
        assertThat(created).isNotNull();
        assertThat(created).hasSize(3);
        assertThat(created.get(0).get("requirement")).isEqualTo("HTTPS required");
        assertThat(created.get(1).get("requirement")).isEqualTo("Input validation on all forms");
        assertThat(created.get(2).get("requirement")).isEqualTo("CORS properly configured");
    }

    @Test
    void getItemsForJob_pagination_works() {
        TestSetup owner = setupOwner();
        UUID projectId = createProject(owner.token(), owner.teamId(), "Pagination Compliance Project");
        UUID jobId = createJob(owner.token(), projectId);

        // Create 3 items via batch
        var items = List.of(
                Map.of("jobId", jobId, "requirement", "Req 1", "status", "MET", "agentType", "SECURITY"),
                Map.of("jobId", jobId, "requirement", "Req 2", "status", "MET", "agentType", "SECURITY"),
                Map.of("jobId", jobId, "requirement", "Req 3", "status", "PARTIAL", "agentType", "SECURITY")
        );
        HttpEntity<?> batchEntity = new HttpEntity<>(items, authHeaders(owner.token()));
        restTemplate.exchange("/api/v1/compliance/items/batch", HttpMethod.POST, batchEntity, List.class);

        // Get page 0 with size 2
        HttpEntity<?> getEntity = new HttpEntity<>(null, authHeaders(owner.token()));
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/compliance/items/job/" + jobId + "?page=0&size=2",
                HttpMethod.GET, getEntity, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> pageBody = response.getBody();
        assertThat(pageBody).isNotNull();
        List<Map<String, Object>> content = (List<Map<String, Object>>) pageBody.get("content");
        assertThat(content).hasSize(2);
        assertThat(((Number) pageBody.get("totalElements")).longValue()).isEqualTo(3L);
        assertThat(((Number) pageBody.get("totalPages")).intValue()).isEqualTo(2);
        assertThat(pageBody.get("isLast")).isEqualTo(false);
    }

    @Test
    void getComplianceSummary_returnsCorrectCounts() {
        TestSetup owner = setupOwner();
        UUID projectId = createProject(owner.token(), owner.teamId(), "Summary Project");
        UUID jobId = createJob(owner.token(), projectId);

        // Create items with different statuses: 2 MET, 1 PARTIAL, 1 MISSING
        var items = List.of(
                Map.of("jobId", jobId, "requirement", "Req MET 1", "status", "MET", "agentType", "SECURITY"),
                Map.of("jobId", jobId, "requirement", "Req MET 2", "status", "MET", "agentType", "SECURITY"),
                Map.of("jobId", jobId, "requirement", "Req PARTIAL", "status", "PARTIAL", "agentType", "SECURITY"),
                Map.of("jobId", jobId, "requirement", "Req MISSING", "status", "MISSING", "agentType", "SECURITY")
        );
        HttpEntity<?> batchEntity = new HttpEntity<>(items, authHeaders(owner.token()));
        restTemplate.exchange("/api/v1/compliance/items/batch", HttpMethod.POST, batchEntity, List.class);

        HttpEntity<?> getEntity = new HttpEntity<>(null, authHeaders(owner.token()));
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/compliance/summary/job/" + jobId, HttpMethod.GET, getEntity, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> summary = response.getBody();
        assertThat(summary).isNotNull();
        assertThat(((Number) summary.get("met")).intValue()).isEqualTo(2);
        assertThat(((Number) summary.get("partial")).intValue()).isEqualTo(1);
        assertThat(((Number) summary.get("missing")).intValue()).isEqualTo(1);
        assertThat(((Number) summary.get("notApplicable")).intValue()).isEqualTo(0);
        assertThat(((Number) summary.get("total")).intValue()).isEqualTo(4);
        // Score: (2*100 + 1*50) / (4*100) * 100 = 250/400 * 100 = 62.5 -> rounds to 63
        assertThat(((Number) summary.get("complianceScore")).longValue()).isEqualTo(63L);
    }
}
