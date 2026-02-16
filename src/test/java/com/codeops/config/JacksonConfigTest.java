package com.codeops.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class JacksonConfigTest {

    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper();
        JavaTimeModule module = new JavaTimeModule();
        module.addDeserializer(Instant.class, new JacksonConfig.LenientInstantDeserializer());
        mapper.registerModule(module);
    }

    @Test
    void deserializesInstantWithZ() throws Exception {
        Instant result = mapper.readValue("\"2026-02-16T14:30:00.000Z\"", Instant.class);
        assertEquals(Instant.parse("2026-02-16T14:30:00.000Z"), result);
    }

    @Test
    void deserializesInstantWithOffset() throws Exception {
        Instant result = mapper.readValue("\"2026-02-16T14:30:00.000+00:00\"", Instant.class);
        assertEquals(Instant.parse("2026-02-16T14:30:00.000Z"), result);
    }

    @Test
    void deserializesInstantWithoutTimezone_assumesUtc() throws Exception {
        Instant result = mapper.readValue("\"2026-02-16T08:25:00.000\"", Instant.class);
        assertEquals(Instant.parse("2026-02-16T08:25:00.000Z"), result);
    }

    @Test
    void deserializesInstantWithoutMillis() throws Exception {
        Instant result = mapper.readValue("\"2026-02-16T08:25:00Z\"", Instant.class);
        assertEquals(Instant.parse("2026-02-16T08:25:00Z"), result);
    }

    @Test
    void deserializesInstantWithoutMillisNoZ_assumesUtc() throws Exception {
        Instant result = mapper.readValue("\"2026-02-16T08:25:00\"", Instant.class);
        assertEquals(Instant.parse("2026-02-16T08:25:00Z"), result);
    }
}
