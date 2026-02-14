package com.codeops.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EncryptionServiceTest {

    private EncryptionService service;

    @BeforeEach
    void setUp() {
        service = new EncryptionService("test-encryption-key-minimum-32chars-ok");
    }

    @Test
    void encryptAndDecrypt_roundTrip() {
        String plaintext = "my-secret-api-token";
        String encrypted = service.encrypt(plaintext);
        assertNotEquals(plaintext, encrypted);
        assertEquals(plaintext, service.decrypt(encrypted));
    }

    @Test
    void encrypt_producesDifferentCiphertextEachTime() {
        String plaintext = "same-input";
        String encrypted1 = service.encrypt(plaintext);
        String encrypted2 = service.encrypt(plaintext);
        assertNotEquals(encrypted1, encrypted2);
    }

    @Test
    void decrypt_withWrongKey_throws() {
        String encrypted = service.encrypt("secret");
        EncryptionService otherService = new EncryptionService("different-key-that-is-at-least-32chars");
        assertThrows(RuntimeException.class, () -> otherService.decrypt(encrypted));
    }

    @Test
    void encrypt_emptyString() {
        String encrypted = service.encrypt("");
        assertEquals("", service.decrypt(encrypted));
    }

    @Test
    void encrypt_longString() {
        String longText = "A".repeat(10000);
        String encrypted = service.encrypt(longText);
        assertEquals(longText, service.decrypt(encrypted));
    }

    @Test
    void decrypt_invalidBase64_throws() {
        assertThrows(RuntimeException.class, () -> service.decrypt("not-valid-base64!!!"));
    }
}
