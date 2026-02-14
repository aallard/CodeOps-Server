package com.codeops.service;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TokenBlacklistService {

    private final ConcurrentHashMap.KeySetView<String, Boolean> blacklistedTokens = ConcurrentHashMap.newKeySet();

    public void blacklist(String jti, Instant expiry) {
        if (jti != null) {
            blacklistedTokens.add(jti);
        }
    }

    public boolean isBlacklisted(String jti) {
        return jti != null && blacklistedTokens.contains(jti);
    }
}
