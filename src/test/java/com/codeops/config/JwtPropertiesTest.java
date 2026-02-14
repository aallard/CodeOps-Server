package com.codeops.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtPropertiesTest {

    @Test
    void defaultValues() {
        JwtProperties props = new JwtProperties();
        assertEquals(24, props.getExpirationHours());
        assertEquals(30, props.getRefreshExpirationDays());
        assertNull(props.getSecret());
    }

    @Test
    void settersAndGetters() {
        JwtProperties props = new JwtProperties();
        props.setSecret("my-secret-key-that-is-long-enough");
        props.setExpirationHours(48);
        props.setRefreshExpirationDays(60);

        assertEquals("my-secret-key-that-is-long-enough", props.getSecret());
        assertEquals(48, props.getExpirationHours());
        assertEquals(60, props.getRefreshExpirationDays());
    }
}
