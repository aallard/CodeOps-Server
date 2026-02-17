package com.codeops.dto.response;

import java.util.List;

public record MfaRecoveryResponse(List<String> recoveryCodes) {}
