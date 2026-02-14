package com.codeops.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PaginationIT extends BaseIntegrationTest {

    private String token;
    private UUID teamId;

    @BeforeEach
    void setUp() {
        TestSetup setup = setupOwner();
        this.token = setup.token();
        this.teamId = setup.teamId();
    }

    private void createProjects(int count) {
        for (int i = 0; i < count; i++) {
            createProject(token, teamId, "Pagination Project " + i);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getProjects(String url) {
        HttpEntity<?> entity = new HttpEntity<>(authHeaders(token));
        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getBody();
    }

    @Test
    void listProjects_with25Records_page0Size20_returns20() {
        createProjects(25);

        Map<String, Object> body = getProjects(
                "/api/v1/projects/team/" + teamId + "?page=0&size=20");

        List<?> content = (List<?>) body.get("content");
        assertThat(content).hasSize(20);
        assertThat(((Number) body.get("totalElements")).longValue()).isEqualTo(25L);
        assertThat(((Number) body.get("totalPages")).intValue()).isEqualTo(2);
        assertThat((Boolean) body.get("isLast")).isFalse();
    }

    @Test
    void listProjects_page1Size20_returns5Remaining() {
        createProjects(25);

        Map<String, Object> body = getProjects(
                "/api/v1/projects/team/" + teamId + "?page=1&size=20");

        List<?> content = (List<?>) body.get("content");
        assertThat(content).hasSize(5);
        assertThat(((Number) body.get("totalElements")).longValue()).isEqualTo(25L);
        assertThat((Boolean) body.get("isLast")).isTrue();
    }

    @Test
    void listProjects_beyondLastPage_returnsEmptyContent() {
        createProjects(25);

        Map<String, Object> body = getProjects(
                "/api/v1/projects/team/" + teamId + "?page=5&size=20");

        List<?> content = (List<?>) body.get("content");
        assertThat(content).isEmpty();
        assertThat(((Number) body.get("totalElements")).longValue()).isEqualTo(25L);
    }

    @Test
    void listProjects_defaultSize_returns20() {
        createProjects(25);

        Map<String, Object> body = getProjects(
                "/api/v1/projects/team/" + teamId + "?page=0");

        List<?> content = (List<?>) body.get("content");
        assertThat(content).hasSize(20);
        assertThat(((Number) body.get("size")).intValue()).isEqualTo(20);
    }

    @Test
    void listProjects_sizeExceedsMax_clampedTo100() {
        // MAX_PROJECTS_PER_TEAM is 100, create exactly that many
        createProjects(100);

        Map<String, Object> body = getProjects(
                "/api/v1/projects/team/" + teamId + "?page=0&size=200");

        List<?> content = (List<?>) body.get("content");
        // MAX_PAGE_SIZE is 100, so even with size=200 we should get at most 100
        assertThat(content).hasSize(100);
        assertThat(((Number) body.get("size")).intValue()).isEqualTo(100);
    }
}
