package com.codeops.dto.response;

public record AuthResponse(String token, String refreshToken, UserResponse user) {}
