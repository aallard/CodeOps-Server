package com.codeops.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.*;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("unchecked")
class AuthControllerIT extends BaseIntegrationTest {

    // === Register ===

    @Test
    void register_validCredentials_returnsTokensAndCreatesUser() {
        String email = uniqueEmail("reg");
        var body = Map.of("email", email, "password", "Test@123456", "displayName", "Test User");

        ResponseEntity<Map> response = restTemplate.postForEntity("/api/v1/auth/register", body, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Map<String, Object> responseBody = response.getBody();
        assertThat(responseBody).isNotNull();
        assertThat(responseBody.get("token")).isNotNull();
        assertThat(responseBody.get("refreshToken")).isNotNull();

        Map<String, Object> user = (Map<String, Object>) responseBody.get("user");
        assertThat(user).isNotNull();
        assertThat(user.get("id")).isNotNull();
        assertThat(user.get("email")).isEqualTo(email);
        assertThat(user.get("displayName")).isEqualTo("Test User");
        assertThat(user.get("isActive")).isEqualTo(true);

        // Verify user in DB
        UUID userId = UUID.fromString((String) user.get("id"));
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE id = ? AND email = ?",
                Integer.class, userId, email);
        assertThat(count).isEqualTo(1);
    }

    @Test
    void register_duplicateEmail_returns400() {
        String email = uniqueEmail("dup");
        var body = Map.of("email", email, "password", "Test@123456", "displayName", "First User");
        restTemplate.postForEntity("/api/v1/auth/register", body, Map.class);

        var body2 = Map.of("email", email, "password", "Test@654321", "displayName", "Second User");
        ResponseEntity<Map> response = restTemplate.postForEntity("/api/v1/auth/register", body2, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("message")).isEqualTo("Invalid request");
    }

    @Test
    void register_weakPassword_noUppercase_returns400() {
        var body = Map.of("email", uniqueEmail("weak"), "password", "test@1234", "displayName", "Test");

        ResponseEntity<Map> response = restTemplate.postForEntity("/api/v1/auth/register", body, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("message")).isEqualTo("Invalid request");
    }

    @Test
    void register_weakPassword_noDigit_returns400() {
        var body = Map.of("email", uniqueEmail("weak"), "password", "Test@abcd", "displayName", "Test");

        ResponseEntity<Map> response = restTemplate.postForEntity("/api/v1/auth/register", body, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("message")).isEqualTo("Invalid request");
    }

    @Test
    void register_weakPassword_noSpecialChar_returns400() {
        var body = Map.of("email", uniqueEmail("weak"), "password", "Test12345", "displayName", "Test");

        ResponseEntity<Map> response = restTemplate.postForEntity("/api/v1/auth/register", body, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("message")).isEqualTo("Invalid request");
    }

    @Test
    void register_weakPassword_tooShort_returns400() {
        var body = Map.of("email", uniqueEmail("weak"), "password", "T@1a", "displayName", "Test");

        ResponseEntity<Map> response = restTemplate.postForEntity("/api/v1/auth/register", body, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void register_missingFields_returns400() {
        ResponseEntity<Map> response = restTemplate.postForEntity("/api/v1/auth/register", Map.of(), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // === Login ===

    @Test
    void login_validCredentials_returnsTokens() {
        String email = uniqueEmail("login");
        String password = "Test@123456";
        registerUser(email, password, "Login Test");

        var body = Map.of("email", email, "password", password);
        ResponseEntity<Map> response = restTemplate.postForEntity("/api/v1/auth/login", body, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> responseBody = response.getBody();
        assertThat(responseBody).isNotNull();
        assertThat(responseBody.get("token")).isNotNull();
        assertThat(responseBody.get("refreshToken")).isNotNull();

        Map<String, Object> user = (Map<String, Object>) responseBody.get("user");
        assertThat(user).isNotNull();
        assertThat(user.get("email")).isEqualTo(email);
    }

    @Test
    void login_wrongPassword_returns400() {
        String email = uniqueEmail("wrongpw");
        registerUser(email, "Test@123456", "Wrong PW Test");

        var body = Map.of("email", email, "password", "Wrong@123456");
        ResponseEntity<Map> response = restTemplate.postForEntity("/api/v1/auth/login", body, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("message")).isEqualTo("Invalid request");
    }

    @Test
    void login_nonExistentEmail_returns400() {
        var body = Map.of("email", "nobody@nowhere.com", "password", "Test@123456");
        ResponseEntity<Map> response = restTemplate.postForEntity("/api/v1/auth/login", body, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("message")).isEqualTo("Invalid request");
    }

    @Test
    void login_deactivatedAccount_returns400() {
        // Register the target user
        String targetEmail = uniqueEmail("deact");
        String targetPassword = "Test@123456";
        AuthResult targetReg = registerUser(targetEmail, targetPassword, "Deactivated User");

        // Set up an owner (register, create team => gets OWNER role)
        TestSetup owner = setupOwner();

        // Deactivate the target user using owner token
        HttpEntity<?> deactivateEntity = new HttpEntity<>(null, authHeaders(owner.token()));
        ResponseEntity<Void> deactivateResponse = restTemplate.exchange(
                "/api/v1/users/" + targetReg.userId() + "/deactivate",
                HttpMethod.PUT, deactivateEntity, Void.class);
        assertThat(deactivateResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // Try to login as the deactivated user
        var body = Map.of("email", targetEmail, "password", targetPassword);
        ResponseEntity<Map> response = restTemplate.postForEntity("/api/v1/auth/login", body, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("message")).isEqualTo("Invalid request");
    }

    // === Refresh Token ===

    @Test
    void refreshToken_validToken_returnsNewTokenPair() {
        String email = uniqueEmail("refresh");
        AuthResult reg = registerUser(email, "Test@123456", "Refresh Test");

        var body = Map.of("refreshToken", reg.refreshToken());
        ResponseEntity<Map> response = restTemplate.postForEntity("/api/v1/auth/refresh", body, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> responseBody = response.getBody();
        assertThat(responseBody).isNotNull();
        assertThat(responseBody.get("token")).isNotNull();
        assertThat(responseBody.get("refreshToken")).isNotNull();
        // New tokens should be different from the originals
        assertThat(responseBody.get("token")).isNotEqualTo(reg.token());
        assertThat(responseBody.get("refreshToken")).isNotEqualTo(reg.refreshToken());
    }

    @Test
    void refreshToken_invalidToken_returns400() {
        var body = Map.of("refreshToken", "garbage.invalid.token");
        ResponseEntity<Map> response = restTemplate.postForEntity("/api/v1/auth/refresh", body, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("message")).isEqualTo("Invalid request");
    }

    @Test
    void refreshToken_accessTokenInsteadOfRefresh_returns400() {
        String email = uniqueEmail("accref");
        AuthResult reg = registerUser(email, "Test@123456", "Access As Refresh Test");

        // Send the access token where a refresh token is expected
        var body = Map.of("refreshToken", reg.token());
        ResponseEntity<Map> response = restTemplate.postForEntity("/api/v1/auth/refresh", body, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("message")).isEqualTo("Invalid request");
    }

    // === Logout ===

    @Test
    void logout_blacklistsToken() {
        String email = uniqueEmail("logout");
        AuthResult reg = registerUser(email, "Test@123456", "Logout Test");

        // Logout
        HttpEntity<?> logoutEntity = new HttpEntity<>(null, authHeaders(reg.token()));
        ResponseEntity<Void> logoutResponse = restTemplate.exchange(
                "/api/v1/auth/logout", HttpMethod.POST, logoutEntity, Void.class);
        assertThat(logoutResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // Try to use the same token to access a protected endpoint
        HttpEntity<?> getEntity = new HttpEntity<>(null, authHeaders(reg.token()));
        ResponseEntity<Map> protectedResponse = restTemplate.exchange(
                "/api/v1/users/me", HttpMethod.GET, getEntity, Map.class);
        assertThat(protectedResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // === Change Password ===

    @Test
    void changePassword_validOldPassword_succeeds() {
        String email = uniqueEmail("chpw");
        String oldPassword = "Test@123456";
        String newPassword = "NewPass@789";
        AuthResult reg = registerUser(email, oldPassword, "Change PW Test");

        // Change password
        var changePwBody = Map.of("currentPassword", oldPassword, "newPassword", newPassword);
        HttpEntity<?> changeEntity = new HttpEntity<>(changePwBody, authHeaders(reg.token()));
        ResponseEntity<Void> changeResponse = restTemplate.exchange(
                "/api/v1/auth/change-password", HttpMethod.POST, changeEntity, Void.class);
        assertThat(changeResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Login with new password works
        AuthResult newLogin = loginUser(email, newPassword);
        assertThat(newLogin.token()).isNotNull();

        // Login with old password fails
        var oldPwBody = Map.of("email", email, "password", oldPassword);
        ResponseEntity<Map> oldPwResponse = restTemplate.postForEntity("/api/v1/auth/login", oldPwBody, Map.class);
        assertThat(oldPwResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void changePassword_wrongOldPassword_returns400() {
        String email = uniqueEmail("wrongold");
        AuthResult reg = registerUser(email, "Test@123456", "Wrong Old PW Test");

        var body = Map.of("currentPassword", "Wrong@123456", "newPassword", "NewPass@789");
        HttpEntity<?> entity = new HttpEntity<>(body, authHeaders(reg.token()));
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/auth/change-password", HttpMethod.POST, entity, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("message")).isEqualTo("Invalid request");
    }
}
