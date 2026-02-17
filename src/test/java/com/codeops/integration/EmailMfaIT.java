package com.codeops.integration;

import com.codeops.notification.EmailService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.*;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

/**
 * Integration tests for the email MFA setup, verification, login, resend, disable, and status flows.
 *
 * <p>Uses {@code @SpyBean} on {@link EmailService} to capture plaintext MFA codes that are
 * BCrypt-hashed before storage, enabling full end-to-end verification without actual SMTP delivery.</p>
 */
@SuppressWarnings("unchecked")
class EmailMfaIT extends BaseIntegrationTest {

    @SpyBean
    private EmailService emailService;

    private static final String EMAIL_MFA_SETUP = "/api/v1/auth/mfa/setup/email";
    private static final String EMAIL_MFA_VERIFY_SETUP = "/api/v1/auth/mfa/verify-setup/email";
    private static final String MFA_LOGIN = "/api/v1/auth/mfa/login";
    private static final String MFA_RESEND = "/api/v1/auth/mfa/resend";
    private static final String MFA_DISABLE = "/api/v1/auth/mfa/disable";
    private static final String MFA_STATUS = "/api/v1/auth/mfa/status";

    // ──────────────────────────────────────────────
    // Email MFA Setup
    // ──────────────────────────────────────────────

    @Test
    void emailMfaSetup_returnsRecoveryCodes() {
        String email = uniqueEmail("emfa-setup");
        String password = "Test@123456";
        AuthResult reg = registerUser(email, password, "Email MFA Setup");

        var body = Map.of("password", password);
        HttpEntity<?> entity = new HttpEntity<>(body, authHeaders(reg.token()));
        ResponseEntity<Map> response = restTemplate.exchange(EMAIL_MFA_SETUP, HttpMethod.POST, entity, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<String> codes = (List<String>) response.getBody().get("recoveryCodes");
        assertThat(codes).hasSize(8);
    }

    @Test
    void emailMfaSetup_wrongPassword_returns400() {
        String email = uniqueEmail("emfa-wrongpw");
        String password = "Test@123456";
        AuthResult reg = registerUser(email, password, "Email MFA WrongPW");

        var body = Map.of("password", "Wrong@123456");
        HttpEntity<?> entity = new HttpEntity<>(body, authHeaders(reg.token()));
        ResponseEntity<Map> response = restTemplate.exchange(EMAIL_MFA_SETUP, HttpMethod.POST, entity, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ──────────────────────────────────────────────
    // Email MFA Verify (Enable)
    // ──────────────────────────────────────────────

    @Test
    void emailMfaSetupAndVerify_enablesEmailMfa() {
        String email = uniqueEmail("emfa-verify");
        String password = "Test@123456";
        AuthResult reg = registerUser(email, password, "Email MFA Verify");

        // Setup email MFA
        var setupBody = Map.of("password", password);
        HttpEntity<?> setupEntity = new HttpEntity<>(setupBody, authHeaders(reg.token()));
        restTemplate.exchange(EMAIL_MFA_SETUP, HttpMethod.POST, setupEntity, Map.class);

        // Capture the code sent to email
        ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendMfaCode(eq(email), codeCaptor.capture());
        String emailCode = codeCaptor.getValue();

        // Verify with the email code
        var verifyBody = Map.of("code", emailCode);
        HttpEntity<?> verifyEntity = new HttpEntity<>(verifyBody, authHeaders(reg.token()));
        ResponseEntity<Map> verifyResponse = restTemplate.exchange(
                EMAIL_MFA_VERIFY_SETUP, HttpMethod.POST, verifyEntity, Map.class);

        assertThat(verifyResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(verifyResponse.getBody().get("mfaEnabled")).isEqualTo(true);
        assertThat(verifyResponse.getBody().get("mfaMethod")).isEqualTo("EMAIL");
    }

    @Test
    void emailMfaVerify_invalidCode_returns400() {
        String email = uniqueEmail("emfa-badcode");
        String password = "Test@123456";
        AuthResult reg = registerUser(email, password, "Email MFA BadCode");

        // Setup email MFA
        var setupBody = Map.of("password", password);
        HttpEntity<?> setupEntity = new HttpEntity<>(setupBody, authHeaders(reg.token()));
        restTemplate.exchange(EMAIL_MFA_SETUP, HttpMethod.POST, setupEntity, Map.class);

        // Verify with a wrong code
        var verifyBody = Map.of("code", "000000");
        HttpEntity<?> verifyEntity = new HttpEntity<>(verifyBody, authHeaders(reg.token()));
        ResponseEntity<Map> verifyResponse = restTemplate.exchange(
                EMAIL_MFA_VERIFY_SETUP, HttpMethod.POST, verifyEntity, Map.class);

        assertThat(verifyResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ──────────────────────────────────────────────
    // Email MFA Login Flow
    // ──────────────────────────────────────────────

    @Test
    void emailMfaLogin_fullFlow_returnsChallengeAndThenTokens() {
        String email = uniqueEmail("emfa-login");
        String password = "Test@123456";
        AuthResult reg = registerUser(email, password, "Email MFA Login");

        setupAndEnableEmailMfa(reg.token(), password, email);

        // Login → should get MFA challenge with masked email
        var loginBody = Map.of("email", email, "password", password);
        ResponseEntity<Map> loginResponse = restTemplate.postForEntity(
                "/api/v1/auth/login", loginBody, Map.class);

        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> loginResult = loginResponse.getBody();
        assertThat(loginResult.get("mfaRequired")).isEqualTo(true);
        assertThat(loginResult.get("mfaChallengeToken")).isNotNull();
        assertThat(loginResult.get("maskedEmail")).isNotNull();
        assertThat(loginResult.get("token")).isNull();

        String challengeToken = (String) loginResult.get("mfaChallengeToken");

        // Capture the login MFA code
        ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendMfaCode(eq(email), codeCaptor.capture());
        String loginCode = codeCaptor.getValue();

        // Submit challenge + email code → should get full tokens
        var mfaBody = Map.of("challengeToken", challengeToken, "code", loginCode);
        ResponseEntity<Map> mfaResponse = restTemplate.postForEntity(MFA_LOGIN, mfaBody, Map.class);

        assertThat(mfaResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(mfaResponse.getBody().get("token")).isNotNull();
        assertThat(mfaResponse.getBody().get("refreshToken")).isNotNull();
        assertThat(mfaResponse.getBody().get("user")).isNotNull();

        // Verify the new token works for authenticated requests
        HttpEntity<?> getEntity = new HttpEntity<>(null,
                authHeaders((String) mfaResponse.getBody().get("token")));
        ResponseEntity<Map> meResponse = restTemplate.exchange(
                "/api/v1/users/me", HttpMethod.GET, getEntity, Map.class);
        assertThat(meResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void emailMfaLogin_recoveryCode_succeeds() {
        String email = uniqueEmail("emfa-recovery");
        String password = "Test@123456";
        AuthResult reg = registerUser(email, password, "Email MFA Recovery");

        List<String> recoveryCodes = setupAndEnableEmailMfa(reg.token(), password, email);

        // Login → get challenge
        var loginBody = Map.of("email", email, "password", password);
        ResponseEntity<Map> loginResponse = restTemplate.postForEntity(
                "/api/v1/auth/login", loginBody, Map.class);
        String challengeToken = (String) loginResponse.getBody().get("mfaChallengeToken");

        // Use recovery code instead of email code
        var mfaBody = Map.of("challengeToken", challengeToken, "code", recoveryCodes.get(0));
        ResponseEntity<Map> mfaResponse = restTemplate.postForEntity(MFA_LOGIN, mfaBody, Map.class);

        assertThat(mfaResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(mfaResponse.getBody().get("token")).isNotNull();
    }

    @Test
    void emailMfaResend_sendsNewCodeAndCompletesLogin() {
        String email = uniqueEmail("emfa-resend");
        String password = "Test@123456";
        AuthResult reg = registerUser(email, password, "Email MFA Resend");

        setupAndEnableEmailMfa(reg.token(), password, email);

        // Login → get challenge token
        var loginBody = Map.of("email", email, "password", password);
        ResponseEntity<Map> loginResponse = restTemplate.postForEntity(
                "/api/v1/auth/login", loginBody, Map.class);
        String challengeToken = (String) loginResponse.getBody().get("mfaChallengeToken");

        // Clear invocations from the login call so we can capture the resend separately
        Mockito.clearInvocations(emailService);

        // Resend code
        var resendBody = Map.of("challengeToken", challengeToken);
        ResponseEntity<Void> resendResponse = restTemplate.postForEntity(
                MFA_RESEND, resendBody, Void.class);
        assertThat(resendResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Capture the resent code
        ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendMfaCode(eq(email), codeCaptor.capture());
        String resentCode = codeCaptor.getValue();

        // Complete login with the resent code
        var mfaBody = Map.of("challengeToken", challengeToken, "code", resentCode);
        ResponseEntity<Map> mfaResponse = restTemplate.postForEntity(MFA_LOGIN, mfaBody, Map.class);
        assertThat(mfaResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(mfaResponse.getBody().get("token")).isNotNull();
    }

    // ──────────────────────────────────────────────
    // Email MFA Disable
    // ──────────────────────────────────────────────

    @Test
    void emailMfaDisable_restoresNormalLogin() {
        String email = uniqueEmail("emfa-disable");
        String password = "Test@123456";
        AuthResult reg = registerUser(email, password, "Email MFA Disable");

        setupAndEnableEmailMfa(reg.token(), password, email);

        // Disable MFA
        var disableBody = Map.of("password", password);
        HttpEntity<?> disableEntity = new HttpEntity<>(disableBody, authHeaders(reg.token()));
        ResponseEntity<Map> disableResponse = restTemplate.exchange(
                MFA_DISABLE, HttpMethod.POST, disableEntity, Map.class);

        assertThat(disableResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(disableResponse.getBody().get("mfaEnabled")).isEqualTo(false);
        assertThat(disableResponse.getBody().get("mfaMethod")).isEqualTo("NONE");

        // Login should return tokens directly (no MFA challenge)
        var loginBody = Map.of("email", email, "password", password);
        ResponseEntity<Map> loginResponse = restTemplate.postForEntity(
                "/api/v1/auth/login", loginBody, Map.class);

        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(loginResponse.getBody().get("token")).isNotNull();
        assertThat(loginResponse.getBody().get("mfaRequired")).isNull();
    }

    // ──────────────────────────────────────────────
    // Email MFA Status
    // ──────────────────────────────────────────────

    @Test
    void emailMfaStatus_afterEnable_showsEmailMethodAndRecoveryCodes() {
        String email = uniqueEmail("emfa-status");
        String password = "Test@123456";
        AuthResult reg = registerUser(email, password, "Email MFA Status");

        setupAndEnableEmailMfa(reg.token(), password, email);

        HttpEntity<?> entity = new HttpEntity<>(null, authHeaders(reg.token()));
        ResponseEntity<Map> response = restTemplate.exchange(
                MFA_STATUS, HttpMethod.GET, entity, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("mfaEnabled")).isEqualTo(true);
        assertThat(response.getBody().get("mfaMethod")).isEqualTo("EMAIL");
        assertThat(response.getBody().get("recoveryCodesRemaining")).isEqualTo(8);
    }

    // ──────────────────────────────────────────────
    // Helper
    // ──────────────────────────────────────────────

    /**
     * Sets up and enables email MFA for a user, returning the recovery codes.
     * Clears spy invocations after setup so subsequent tests can capture fresh calls.
     */
    private List<String> setupAndEnableEmailMfa(String token, String password, String email) {
        // Step 1: Setup email MFA
        var setupBody = Map.of("password", password);
        HttpEntity<?> setupEntity = new HttpEntity<>(setupBody, authHeaders(token));
        ResponseEntity<Map> setupResponse = restTemplate.exchange(
                EMAIL_MFA_SETUP, HttpMethod.POST, setupEntity, Map.class);
        List<String> recoveryCodes = (List<String>) setupResponse.getBody().get("recoveryCodes");

        // Step 2: Capture the verification code sent via email
        ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendMfaCode(eq(email), codeCaptor.capture());
        String emailCode = codeCaptor.getValue();

        // Step 3: Verify with the code to enable MFA
        var verifyBody = Map.of("code", emailCode);
        HttpEntity<?> verifyEntity = new HttpEntity<>(verifyBody, authHeaders(token));
        ResponseEntity<Map> verifyResponse = restTemplate.exchange(
                EMAIL_MFA_VERIFY_SETUP, HttpMethod.POST, verifyEntity, Map.class);
        assertThat(verifyResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Clear invocations so subsequent captures in the test are clean
        Mockito.clearInvocations(emailService);

        return recoveryCodes;
    }
}
