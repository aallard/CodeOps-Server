package com.codeops.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class TokenBlacklistServiceTest {

    private TokenBlacklistService service;

    @BeforeEach
    void setUp() {
        service = new TokenBlacklistService();
    }

    @Test
    void blacklist_addsToken() {
        String jti = "test-jti-123";
        service.blacklist(jti, Instant.now().plusSeconds(3600));
        assertTrue(service.isBlacklisted(jti));
    }

    @Test
    void isBlacklisted_returnsFalseForUnknownToken() {
        assertFalse(service.isBlacklisted("unknown-jti"));
    }

    @Test
    void blacklist_nullJti_noOp() {
        service.blacklist(null, Instant.now());
        assertFalse(service.isBlacklisted(null));
    }

    @Test
    void isBlacklisted_nullJti_returnsFalse() {
        assertFalse(service.isBlacklisted(null));
    }

    @Test
    void multipleTokensCanBeBlacklisted() {
        service.blacklist("jti-1", Instant.now().plusSeconds(3600));
        service.blacklist("jti-2", Instant.now().plusSeconds(3600));
        assertTrue(service.isBlacklisted("jti-1"));
        assertTrue(service.isBlacklisted("jti-2"));
        assertFalse(service.isBlacklisted("jti-3"));
    }
}
