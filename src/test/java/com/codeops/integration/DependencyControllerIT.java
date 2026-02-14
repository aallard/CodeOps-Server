package com.codeops.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("unchecked")
class DependencyControllerIT extends BaseIntegrationTest {

    @Test
    void createScan_createsInDB() {
        TestSetup owner = setupOwner();
        UUID projectId = createProject(owner.token(), owner.teamId(), "Dep Scan Project");

        var body = Map.of(
                "projectId", projectId,
                "manifestFile", "pom.xml",
                "totalDependencies", 45,
                "outdatedCount", 8,
                "vulnerableCount", 3
        );
        HttpEntity<?> entity = new HttpEntity<>(body, authHeaders(owner.token()));

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/dependencies/scans", HttpMethod.POST, entity, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Map<String, Object> scan = response.getBody();
        assertThat(scan).isNotNull();
        assertThat(scan.get("id")).isNotNull();
        assertThat(scan.get("projectId")).isEqualTo(projectId.toString());
        assertThat(scan.get("manifestFile")).isEqualTo("pom.xml");
        assertThat(((Number) scan.get("totalDependencies")).intValue()).isEqualTo(45);
        assertThat(((Number) scan.get("outdatedCount")).intValue()).isEqualTo(8);
        assertThat(((Number) scan.get("vulnerableCount")).intValue()).isEqualTo(3);
        assertThat(scan.get("createdAt")).isNotNull();

        // Verify in DB
        UUID scanId = UUID.fromString((String) scan.get("id"));
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM dependency_scans WHERE id = ?", Integer.class, scanId);
        assertThat(count).isEqualTo(1);
    }

    @Test
    void addVulnerability_toScan_createsLinked() {
        TestSetup owner = setupOwner();
        UUID projectId = createProject(owner.token(), owner.teamId(), "Vuln Add Project");
        UUID scanId = createDependencyScan(owner.token(), projectId);

        var body = Map.of(
                "scanId", scanId,
                "dependencyName", "log4j-core",
                "currentVersion", "2.14.0",
                "fixedVersion", "2.17.1",
                "cveId", "CVE-2021-44228",
                "severity", "CRITICAL",
                "description", "Log4Shell remote code execution"
        );
        HttpEntity<?> entity = new HttpEntity<>(body, authHeaders(owner.token()));

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/dependencies/vulnerabilities", HttpMethod.POST, entity, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Map<String, Object> vuln = response.getBody();
        assertThat(vuln).isNotNull();
        assertThat(vuln.get("id")).isNotNull();
        assertThat(vuln.get("scanId")).isEqualTo(scanId.toString());
        assertThat(vuln.get("dependencyName")).isEqualTo("log4j-core");
        assertThat(vuln.get("currentVersion")).isEqualTo("2.14.0");
        assertThat(vuln.get("fixedVersion")).isEqualTo("2.17.1");
        assertThat(vuln.get("cveId")).isEqualTo("CVE-2021-44228");
        assertThat(vuln.get("severity")).isEqualTo("CRITICAL");
        assertThat(vuln.get("description")).isEqualTo("Log4Shell remote code execution");
        assertThat(vuln.get("status")).isEqualTo("OPEN");
        assertThat(vuln.get("createdAt")).isNotNull();
    }

    @Test
    void getVulnerabilities_forScan_pagination_works() {
        TestSetup owner = setupOwner();
        UUID projectId = createProject(owner.token(), owner.teamId(), "Vuln Pagination Project");
        UUID scanId = createDependencyScan(owner.token(), projectId);

        // Create 3 vulnerabilities
        for (int i = 1; i <= 3; i++) {
            var body = Map.of(
                    "scanId", scanId,
                    "dependencyName", "dep-" + i,
                    "severity", "HIGH",
                    "description", "Vulnerability " + i
            );
            HttpEntity<?> entity = new HttpEntity<>(body, authHeaders(owner.token()));
            restTemplate.exchange("/api/v1/dependencies/vulnerabilities", HttpMethod.POST, entity, Map.class);
        }

        // Get page 0 with size 2
        HttpEntity<?> getEntity = new HttpEntity<>(null, authHeaders(owner.token()));
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/dependencies/vulnerabilities/scan/" + scanId + "?page=0&size=2",
                HttpMethod.GET, getEntity, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> pageBody = response.getBody();
        assertThat(pageBody).isNotNull();
        List<Map<String, Object>> content = (List<Map<String, Object>>) pageBody.get("content");
        assertThat(content).hasSize(2);
        assertThat(((Number) pageBody.get("totalElements")).longValue()).isEqualTo(3L);
        assertThat(((Number) pageBody.get("totalPages")).intValue()).isEqualTo(2);
        assertThat(pageBody.get("isLast")).isEqualTo(false);

        // Get page 1
        ResponseEntity<Map> response2 = restTemplate.exchange(
                "/api/v1/dependencies/vulnerabilities/scan/" + scanId + "?page=1&size=2",
                HttpMethod.GET, getEntity, Map.class);

        Map<String, Object> pageBody2 = response2.getBody();
        assertThat(pageBody2).isNotNull();
        List<Map<String, Object>> content2 = (List<Map<String, Object>>) pageBody2.get("content");
        assertThat(content2).hasSize(1);
        assertThat(pageBody2.get("isLast")).isEqualTo(true);
    }

    @Test
    void getScan_byId_includesAllFields() {
        TestSetup owner = setupOwner();
        UUID projectId = createProject(owner.token(), owner.teamId(), "Get Scan Project");
        UUID scanId = createDependencyScan(owner.token(), projectId);

        HttpEntity<?> entity = new HttpEntity<>(null, authHeaders(owner.token()));
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/dependencies/scans/" + scanId, HttpMethod.GET, entity, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> scan = response.getBody();
        assertThat(scan).isNotNull();
        assertThat(scan.get("id")).isEqualTo(scanId.toString());
        assertThat(scan.get("projectId")).isEqualTo(projectId.toString());
        assertThat(scan.get("manifestFile")).isEqualTo("pom.xml");
        assertThat(((Number) scan.get("totalDependencies")).intValue()).isEqualTo(10);
        assertThat(((Number) scan.get("outdatedCount")).intValue()).isEqualTo(2);
        assertThat(((Number) scan.get("vulnerableCount")).intValue()).isEqualTo(1);
        assertThat(scan.get("createdAt")).isNotNull();
    }

    @Test
    void getLatestScan_returnsNewest() {
        TestSetup owner = setupOwner();
        UUID projectId = createProject(owner.token(), owner.teamId(), "Latest Scan Project");

        // Create first scan
        UUID scanId1 = createDependencyScan(owner.token(), projectId);

        // Create second scan (should be latest)
        var body2 = Map.of(
                "projectId", projectId,
                "manifestFile", "package.json",
                "totalDependencies", 100,
                "outdatedCount", 15,
                "vulnerableCount", 5
        );
        HttpEntity<?> createEntity = new HttpEntity<>(body2, authHeaders(owner.token()));
        ResponseEntity<Map> createResponse = restTemplate.exchange(
                "/api/v1/dependencies/scans", HttpMethod.POST, createEntity, Map.class);
        UUID scanId2 = UUID.fromString((String) createResponse.getBody().get("id"));

        // Get latest scan
        HttpEntity<?> getEntity = new HttpEntity<>(null, authHeaders(owner.token()));
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/dependencies/scans/project/" + projectId + "/latest",
                HttpMethod.GET, getEntity, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> scan = response.getBody();
        assertThat(scan).isNotNull();
        assertThat(scan.get("id")).isEqualTo(scanId2.toString());
        assertThat(scan.get("manifestFile")).isEqualTo("package.json");
        assertThat(((Number) scan.get("totalDependencies")).intValue()).isEqualTo(100);
    }
}
