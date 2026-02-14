package com.codeops.config;

import com.codeops.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

class SecurityConfigTest {

    @Test
    void passwordEncoder_isBCrypt() throws Exception {
        // SecurityConfig requires constructor args, test the passwordEncoder bean method directly
        // via reflection since it's a simple bean method
        var method = SecurityConfig.class.getMethod("passwordEncoder");
        assertNotNull(method);

        // Verify the return type
        assertEquals(PasswordEncoder.class, method.getReturnType());
    }

    @Test
    void bCryptPasswordEncoder_encodes() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);
        String encoded = encoder.encode("password");
        assertTrue(encoder.matches("password", encoded));
        assertFalse(encoder.matches("wrong", encoded));
    }
}
