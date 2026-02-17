package com.codeops.dto.response;

import java.util.List;

public record MfaSetupResponse(String secret, String qrCodeUri, List<String> recoveryCodes) {}
