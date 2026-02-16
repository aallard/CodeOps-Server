package com.codeops.config;

import com.codeops.dto.response.ErrorResponse;
import com.codeops.exception.AuthorizationException;
import com.codeops.exception.CodeOpsException;
import com.codeops.exception.NotFoundException;
import com.codeops.exception.ValidationException;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void handleNotFound_returns404() {
        ResponseEntity<ErrorResponse> response = handler.handleNotFound(new EntityNotFoundException("User not found"));
        assertEquals(404, response.getStatusCode().value());
        assertEquals("Resource not found", response.getBody().message());
    }

    @Test
    void handleBadRequest_returns400() {
        ResponseEntity<ErrorResponse> response = handler.handleBadRequest(new IllegalArgumentException("Invalid input"));
        assertEquals(400, response.getStatusCode().value());
        assertEquals("Invalid request", response.getBody().message());
    }

    @Test
    void handleForbidden_returns403() {
        ResponseEntity<ErrorResponse> response = handler.handleForbidden(new AccessDeniedException("Access denied"));
        assertEquals(403, response.getStatusCode().value());
        assertEquals("Access denied", response.getBody().message());
    }

    @Test
    void handleValidation_returns400WithFieldErrors() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(
                new FieldError("obj", "email", "must not be blank"),
                new FieldError("obj", "name", "size must be between 1 and 100")
        ));

        ResponseEntity<ErrorResponse> response = handler.handleValidation(ex);
        assertEquals(400, response.getStatusCode().value());
        assertTrue(response.getBody().message().contains("email"));
        assertTrue(response.getBody().message().contains("name"));
    }

    @Test
    void handleCodeOpsNotFound_returns404() {
        ResponseEntity<ErrorResponse> response = handler.handleCodeOpsNotFound(new NotFoundException("Team not found"));
        assertEquals(404, response.getStatusCode().value());
        assertEquals("Team not found", response.getBody().message());
    }

    @Test
    void handleCodeOpsValidation_returns400() {
        ResponseEntity<ErrorResponse> response = handler.handleCodeOpsValidation(new ValidationException("Invalid field"));
        assertEquals(400, response.getStatusCode().value());
        assertEquals("Invalid field", response.getBody().message());
    }

    @Test
    void handleCodeOpsAuth_returns403() {
        ResponseEntity<ErrorResponse> response = handler.handleCodeOpsAuth(new AuthorizationException("Not authorized"));
        assertEquals(403, response.getStatusCode().value());
        assertEquals("Not authorized", response.getBody().message());
    }

    @Test
    void handleCodeOps_returns500() {
        ResponseEntity<ErrorResponse> response = handler.handleCodeOps(new CodeOpsException("Something broke"));
        assertEquals(500, response.getStatusCode().value());
        assertEquals("An internal error occurred", response.getBody().message());
    }

    @Test
    void handleMessageNotReadable_returns400() {
        HttpMessageNotReadableException ex = mock(HttpMessageNotReadableException.class);
        when(ex.getMessage()).thenReturn("Cannot deserialize value of type `java.time.Instant`");

        ResponseEntity<ErrorResponse> response = handler.handleMessageNotReadable(ex);
        assertEquals(400, response.getStatusCode().value());
        assertEquals("Malformed request body", response.getBody().message());
    }

    @Test
    void handleNoResourceFound_returns404() throws NoResourceFoundException {
        NoResourceFoundException ex = mock(NoResourceFoundException.class);
        when(ex.getMessage()).thenReturn("No static resource api/v1/nonexistent.");

        ResponseEntity<ErrorResponse> response = handler.handleNoResourceFound(ex);
        assertEquals(404, response.getStatusCode().value());
        assertEquals("Resource not found", response.getBody().message());
    }

    @Test
    void handleGeneral_returns500_sanitizedMessage() {
        ResponseEntity<ErrorResponse> response = handler.handleGeneral(new RuntimeException("Sensitive stack trace info"));
        assertEquals(500, response.getStatusCode().value());
        assertEquals("An internal error occurred", response.getBody().message());
    }
}
