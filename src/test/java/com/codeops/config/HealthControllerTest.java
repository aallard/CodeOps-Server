package com.codeops.config;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class HealthControllerTest {

    @Test
    void health_returnsUpStatus() {
        HealthController controller = new HealthController();
        ResponseEntity<Map<String, Object>> response = controller.health();
        assertEquals(200, response.getStatusCode().value());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals("UP", body.get("status"));
        assertEquals("codeops-server", body.get("service"));
        assertNotNull(body.get("timestamp"));
    }
}
