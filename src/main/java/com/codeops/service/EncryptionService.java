package com.codeops.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Arrays;
import java.util.Base64;

/**
 * Provides AES-256-GCM symmetric encryption and decryption for sensitive data such as
 * credentials (GitHub PATs, Jira API tokens).
 *
 * <p>The encryption key is derived from the configured {@code codeops.encryption.key} property
 * using PBKDF2WithHmacSHA256 with 100,000 iterations and a static salt. Each encryption
 * operation generates a unique 12-byte IV (initialization vector) using {@link SecureRandom},
 * which is prepended to the ciphertext before Base64 encoding. The 128-bit GCM authentication
 * tag provides integrity verification on decryption.</p>
 *
 * <p><strong>Warning:</strong> Changing the key derivation parameters will invalidate all
 * existing encrypted data and requires a re-encryption migration.</p>
 *
 * @see javax.crypto.Cipher
 */
@Service
public class EncryptionService {

    private final SecretKey secretKey;

    /**
     * Initializes the encryption service by deriving an AES-256 secret key from the provided
     * configuration key using PBKDF2WithHmacSHA256.
     *
     * @param key the raw encryption key from the {@code codeops.encryption.key} application property
     * @throws RuntimeException if the key derivation algorithm is unavailable or key generation fails
     */
    public EncryptionService(@Value("${codeops.encryption.key}") String key) {
        try {
            // PBKDF2 key derivation — significantly stronger than raw SHA-256
            byte[] salt = "codeops-static-salt-v1".getBytes(StandardCharsets.UTF_8);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            KeySpec spec = new PBEKeySpec(key.toCharArray(), salt, 100_000, 256);
            byte[] keyBytes = factory.generateSecret(spec).getEncoded();
            // TODO: Changing key derivation invalidates existing encrypted data — requires re-encryption migration
            this.secretKey = new SecretKeySpec(keyBytes, "AES");
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize encryption key", e);
        }
    }

    /**
     * Encrypts a plaintext string using AES-256-GCM with a randomly generated 12-byte IV.
     *
     * <p>The returned string is Base64-encoded and contains the IV prepended to the ciphertext
     * (including the GCM authentication tag). Format: {@code Base64(IV || ciphertext || authTag)}.</p>
     *
     * @param plaintext the plaintext string to encrypt
     * @return the Base64-encoded ciphertext with prepended IV
     * @throws RuntimeException if encryption fails due to a cryptographic error
     */
    public String encrypt(String plaintext) {
        try {
            byte[] iv = new byte[12];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(128, iv));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            byte[] combined = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }

    /**
     * Decrypts a Base64-encoded ciphertext that was produced by {@link #encrypt(String)}.
     *
     * <p>Extracts the 12-byte IV from the beginning of the decoded bytes, then decrypts the
     * remaining ciphertext using AES-256-GCM. The GCM authentication tag is verified automatically,
     * ensuring data integrity.</p>
     *
     * @param encryptedBase64 the Base64-encoded string containing IV and ciphertext
     * @return the decrypted plaintext string
     * @throws RuntimeException if decryption fails due to a wrong key, corrupted data, or tampered ciphertext
     */
    public String decrypt(String encryptedBase64) {
        try {
            byte[] combined = Base64.getDecoder().decode(encryptedBase64);
            byte[] iv = Arrays.copyOfRange(combined, 0, 12);
            byte[] ciphertext = Arrays.copyOfRange(combined, 12, combined.length);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(128, iv));
            byte[] plaintext = cipher.doFinal(ciphertext);

            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Decryption failed", e);
        }
    }
}
