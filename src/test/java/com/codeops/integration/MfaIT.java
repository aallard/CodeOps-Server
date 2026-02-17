package com.codeops.integration;

import dev.samstevens.totp.code.CodeGenerator;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("unchecked")
class MfaIT extends BaseIntegrationTest {

    private static final String MFA_SETUP = "/api/v1/auth/mfa/setup";
    private static final String MFA_VERIFY = "/api/v1/auth/mfa/verify";
    private static final String MFA_LOGIN = "/api/v1/auth/mfa/login";
    private static final String MFA_DISABLE = "/api/v1/auth/mfa/disable";
    private static final String MFA_STATUS = "/api/v1/auth/mfa/status";
    private static final String MFA_RECOVERY = "/api/v1/auth/mfa/recovery-codes";

    // ──────────────────────────────────────────────
    // MFA Setup
    // ──────────────────────────────────────────────

    @Test
    void mfaSetup_returnsSecretAndQrAndRecoveryCodes() {
        String email = uniqueEmail("mfa-setup");
        String password = "Test@123456";
        AuthResult reg = registerUser(email, password, "MFA Setup Test");

        var body = Map.of("password", password);
        HttpEntity<?> entity = new HttpEntity<>(body, authHeaders(reg.token()));
        ResponseEntity<Map> response = restTemplate.exchange(MFA_SETUP, HttpMethod.POST, entity, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> responseBody = response.getBody();
        assertThat(responseBody).isNotNull();
        assertThat(responseBody.get("secret")).isNotNull();
        assertThat(responseBody.get("qrCodeUri")).isNotNull();
        List<String> codes = (List<String>) responseBody.get("recoveryCodes");
        assertThat(codes).hasSize(8);
    }

    @Test
    void mfaSetup_wrongPassword_returns400() {
        String email = uniqueEmail("mfa-wrongpw");
        String password = "Test@123456";
        AuthResult reg = registerUser(email, password, "MFA Wrong PW");

        var body = Map.of("password", "Wrong@123456");
        HttpEntity<?> entity = new HttpEntity<>(body, authHeaders(reg.token()));
        ResponseEntity<Map> response = restTemplate.exchange(MFA_SETUP, HttpMethod.POST, entity, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void mfaSetup_unauthenticated_returns403() {
        var body = Map.of("password", "Test@123456");
        ResponseEntity<Map> response = restTemplate.postForEntity(MFA_SETUP, body, Map.class);

        // @PreAuthorize("isAuthenticated()") on a permitAll path returns 403 (Access Denied)
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // ──────────────────────────────────────────────
    // MFA Verify (Enable)
    // ──────────────────────────────────────────────

    @Test
    void mfaVerify_validCode_enablesMfa() throws Exception {
        String email = uniqueEmail("mfa-verify");
        String password = "Test@123456";
        AuthResult reg = registerUser(email, password, "MFA Verify Test");

        // Step 1: Setup MFA
        var setupBody = Map.of("password", password);
        HttpEntity<?> setupEntity = new HttpEntity<>(setupBody, authHeaders(reg.token()));
        ResponseEntity<Map> setupResponse = restTemplate.exchange(MFA_SETUP, HttpMethod.POST, setupEntity, Map.class);
        String secret = (String) setupResponse.getBody().get("secret");

        // Step 2: Generate a valid TOTP code
        TimeProvider timeProvider = new SystemTimeProvider();
        CodeGenerator codeGenerator = new DefaultCodeGenerator();
        String totpCode = codeGenerator.generate(secret, Math.floorDiv(timeProvider.getTime(), 30));

        // Step 3: Verify with valid code
        var verifyBody = Map.of("code", totpCode);
        HttpEntity<?> verifyEntity = new HttpEntity<>(verifyBody, authHeaders(reg.token()));
        ResponseEntity<Map> verifyResponse = restTemplate.exchange(MFA_VERIFY, HttpMethod.POST, verifyEntity, Map.class);

        assertThat(verifyResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(verifyResponse.getBody().get("mfaEnabled")).isEqualTo(true);
    }

    @Test
    void mfaVerify_invalidCode_returns400() {
        String email = uniqueEmail("mfa-badcode");
        String password = "Test@123456";
        AuthResult reg = registerUser(email, password, "MFA Bad Code");

        // Setup MFA first
        var setupBody = Map.of("password", password);
        HttpEntity<?> setupEntity = new HttpEntity<>(setupBody, authHeaders(reg.token()));
        restTemplate.exchange(MFA_SETUP, HttpMethod.POST, setupEntity, Map.class);

        // Verify with bad code
        var verifyBody = Map.of("code", "000000");
        HttpEntity<?> verifyEntity = new HttpEntity<>(verifyBody, authHeaders(reg.token()));
        ResponseEntity<Map> verifyResponse = restTemplate.exchange(MFA_VERIFY, HttpMethod.POST, verifyEntity, Map.class);

        assertThat(verifyResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ──────────────────────────────────────────────
    // MFA Login Flow (Two-Phase)
    // ──────────────────────────────────────────────

    @Test
    void mfaLogin_fullFlow_returnsChallengeAndThenTokens() throws Exception {
        String email = uniqueEmail("mfa-login");
        String password = "Test@123456";
        AuthResult reg = registerUser(email, password, "MFA Login Test");

        // Setup and enable MFA
        String secret = setupAndEnableMfa(reg.token(), password);

        // Step 1: Login with email/password → should get MFA challenge
        var loginBody = Map.of("email", email, "password", password);
        ResponseEntity<Map> loginResponse = restTemplate.postForEntity("/api/v1/auth/login", loginBody, Map.class);

        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> loginResult = loginResponse.getBody();
        assertThat(loginResult.get("mfaRequired")).isEqualTo(true);
        assertThat(loginResult.get("mfaChallengeToken")).isNotNull();
        assertThat(loginResult.get("token")).isNull();
        assertThat(loginResult.get("refreshToken")).isNull();

        String challengeToken = (String) loginResult.get("mfaChallengeToken");

        // Step 2: Submit MFA challenge + TOTP code → should get full tokens
        TimeProvider timeProvider = new SystemTimeProvider();
        CodeGenerator codeGenerator = new DefaultCodeGenerator();
        String totpCode = codeGenerator.generate(secret, Math.floorDiv(timeProvider.getTime(), 30));

        var mfaLoginBody = Map.of("challengeToken", challengeToken, "code", totpCode);
        ResponseEntity<Map> mfaLoginResponse = restTemplate.postForEntity(MFA_LOGIN, mfaLoginBody, Map.class);

        assertThat(mfaLoginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> mfaResult = mfaLoginResponse.getBody();
        assertThat(mfaResult.get("token")).isNotNull();
        assertThat(mfaResult.get("refreshToken")).isNotNull();
        assertThat(mfaResult.get("user")).isNotNull();

        // Verify the new token works for authenticated requests
        HttpEntity<?> getEntity = new HttpEntity<>(null, authHeaders((String) mfaResult.get("token")));
        ResponseEntity<Map> meResponse = restTemplate.exchange("/api/v1/users/me", HttpMethod.GET, getEntity, Map.class);
        assertThat(meResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void mfaLogin_challengeTokenRejectedForApiAccess() throws Exception {
        String email = uniqueEmail("mfa-reject");
        String password = "Test@123456";
        AuthResult reg = registerUser(email, password, "MFA Reject Test");

        // Setup and enable MFA
        setupAndEnableMfa(reg.token(), password);

        // Login → get challenge token
        var loginBody = Map.of("email", email, "password", password);
        ResponseEntity<Map> loginResponse = restTemplate.postForEntity("/api/v1/auth/login", loginBody, Map.class);
        String challengeToken = (String) loginResponse.getBody().get("mfaChallengeToken");

        // Try to use challenge token for normal API access → should be rejected
        HttpEntity<?> entity = new HttpEntity<>(null, authHeaders(challengeToken));
        ResponseEntity<Map> meResponse = restTemplate.exchange("/api/v1/users/me", HttpMethod.GET, entity, Map.class);

        assertThat(meResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void mfaLogin_recoveryCode_succeeds() throws Exception {
        String email = uniqueEmail("mfa-recovery");
        String password = "Test@123456";
        AuthResult reg = registerUser(email, password, "MFA Recovery Test");

        // Setup MFA and get recovery codes
        var setupBody = Map.of("password", password);
        HttpEntity<?> setupEntity = new HttpEntity<>(setupBody, authHeaders(reg.token()));
        ResponseEntity<Map> setupResponse = restTemplate.exchange(MFA_SETUP, HttpMethod.POST, setupEntity, Map.class);
        String secret = (String) setupResponse.getBody().get("secret");
        List<String> recoveryCodes = (List<String>) setupResponse.getBody().get("recoveryCodes");

        // Enable MFA
        TimeProvider timeProvider = new SystemTimeProvider();
        CodeGenerator codeGenerator = new DefaultCodeGenerator();
        String totpCode = codeGenerator.generate(secret, Math.floorDiv(timeProvider.getTime(), 30));
        var verifyBody = Map.of("code", totpCode);
        HttpEntity<?> verifyEntity = new HttpEntity<>(verifyBody, authHeaders(reg.token()));
        restTemplate.exchange(MFA_VERIFY, HttpMethod.POST, verifyEntity, Map.class);

        // Login → get challenge
        var loginBody = Map.of("email", email, "password", password);
        ResponseEntity<Map> loginResponse = restTemplate.postForEntity("/api/v1/auth/login", loginBody, Map.class);
        String challengeToken = (String) loginResponse.getBody().get("mfaChallengeToken");

        // Use recovery code
        String recoveryCode = recoveryCodes.get(0);
        var mfaLoginBody = Map.of("challengeToken", challengeToken, "code", recoveryCode);
        ResponseEntity<Map> mfaLoginResponse = restTemplate.postForEntity(MFA_LOGIN, mfaLoginBody, Map.class);

        assertThat(mfaLoginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(mfaLoginResponse.getBody().get("token")).isNotNull();
    }

    @Test
    void mfaLogin_invalidCode_returns400() throws Exception {
        String email = uniqueEmail("mfa-badlogin");
        String password = "Test@123456";
        AuthResult reg = registerUser(email, password, "MFA Bad Login");

        setupAndEnableMfa(reg.token(), password);

        // Login → get challenge
        var loginBody = Map.of("email", email, "password", password);
        ResponseEntity<Map> loginResponse = restTemplate.postForEntity("/api/v1/auth/login", loginBody, Map.class);
        String challengeToken = (String) loginResponse.getBody().get("mfaChallengeToken");

        // Submit bad code
        var mfaLoginBody = Map.of("challengeToken", challengeToken, "code", "000000");
        ResponseEntity<Map> mfaLoginResponse = restTemplate.postForEntity(MFA_LOGIN, mfaLoginBody, Map.class);

        assertThat(mfaLoginResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ──────────────────────────────────────────────
    // MFA Disable
    // ──────────────────────────────────────────────

    @Test
    void mfaDisable_afterEnable_restoresNormalLogin() throws Exception {
        String email = uniqueEmail("mfa-disable");
        String password = "Test@123456";
        AuthResult reg = registerUser(email, password, "MFA Disable Test");

        // Setup and enable MFA
        setupAndEnableMfa(reg.token(), password);

        // Disable MFA
        var disableBody = Map.of("password", password);
        HttpEntity<?> disableEntity = new HttpEntity<>(disableBody, authHeaders(reg.token()));
        ResponseEntity<Map> disableResponse = restTemplate.exchange(MFA_DISABLE, HttpMethod.POST, disableEntity, Map.class);

        assertThat(disableResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(disableResponse.getBody().get("mfaEnabled")).isEqualTo(false);

        // Login should now return tokens directly (no MFA challenge)
        var loginBody = Map.of("email", email, "password", password);
        ResponseEntity<Map> loginResponse = restTemplate.postForEntity("/api/v1/auth/login", loginBody, Map.class);

        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(loginResponse.getBody().get("token")).isNotNull();
        assertThat(loginResponse.getBody().get("refreshToken")).isNotNull();
        assertThat(loginResponse.getBody().get("mfaRequired")).isNull();
    }

    // ──────────────────────────────────────────────
    // MFA Status
    // ──────────────────────────────────────────────

    @Test
    void mfaStatus_beforeSetup_returnsFalse() {
        String email = uniqueEmail("mfa-status");
        String password = "Test@123456";
        AuthResult reg = registerUser(email, password, "MFA Status Test");

        HttpEntity<?> entity = new HttpEntity<>(null, authHeaders(reg.token()));
        ResponseEntity<Map> response = restTemplate.exchange(MFA_STATUS, HttpMethod.GET, entity, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("mfaEnabled")).isEqualTo(false);
    }

    @Test
    void mfaStatus_afterEnable_returnsTrue() throws Exception {
        String email = uniqueEmail("mfa-enabled-status");
        String password = "Test@123456";
        AuthResult reg = registerUser(email, password, "MFA Enabled Status");

        setupAndEnableMfa(reg.token(), password);

        HttpEntity<?> entity = new HttpEntity<>(null, authHeaders(reg.token()));
        ResponseEntity<Map> response = restTemplate.exchange(MFA_STATUS, HttpMethod.GET, entity, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("mfaEnabled")).isEqualTo(true);
    }

    // ──────────────────────────────────────────────
    // Helper
    // ──────────────────────────────────────────────

    private String setupAndEnableMfa(String token, String password) throws Exception {
        // Setup
        var setupBody = Map.of("password", password);
        HttpEntity<?> setupEntity = new HttpEntity<>(setupBody, authHeaders(token));
        ResponseEntity<Map> setupResponse = restTemplate.exchange(MFA_SETUP, HttpMethod.POST, setupEntity, Map.class);
        String secret = (String) setupResponse.getBody().get("secret");

        // Generate valid TOTP code and verify
        TimeProvider timeProvider = new SystemTimeProvider();
        CodeGenerator codeGenerator = new DefaultCodeGenerator();
        String totpCode = codeGenerator.generate(secret, Math.floorDiv(timeProvider.getTime(), 30));

        var verifyBody = Map.of("code", totpCode);
        HttpEntity<?> verifyEntity = new HttpEntity<>(verifyBody, authHeaders(token));
        ResponseEntity<Map> verifyResponse = restTemplate.exchange(MFA_VERIFY, HttpMethod.POST, verifyEntity, Map.class);
        assertThat(verifyResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        return secret;
    }
}
