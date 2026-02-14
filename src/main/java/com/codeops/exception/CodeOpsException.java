package com.codeops.exception;

public class CodeOpsException extends RuntimeException {

    public CodeOpsException(String message) {
        super(message);
    }

    public CodeOpsException(String message, Throwable cause) {
        super(message, cause);
    }
}
