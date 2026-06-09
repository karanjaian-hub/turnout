package com.turnout.authservice.exception;

import com.turnout.common.exception.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicate(
            DuplicateResourceException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(
            ResourceNotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(UnauthorizedAccessException.class)
    public ResponseEntity<Map<String, Object>> handleUnauthorized(
            UnauthorizedAccessException ex, HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidToken(
            InvalidTokenException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleBadCredentials(
            BadCredentialsException ex, HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(TurnoutException.class)
    public ResponseEntity<Map<String, Object>> handleTurnout(
            TurnoutException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI());
    }

    // Validation errors — @Valid failed on a request DTO field
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        // Collect all field errors into a readable map
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fe.getField(), fe.getDefaultMessage());
        }

        Map<String, Object> body = buildBody(
                HttpStatus.BAD_REQUEST,
                "Validation failed",
                request.getRequestURI()
        );
        body.put("fieldErrors", fieldErrors);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneral(
            Exception ex, HttpServletRequest request) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred", request.getRequestURI());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private ResponseEntity<Map<String, Object>> build(
            HttpStatus status, String message, String path) {
        return ResponseEntity.status(status).body(buildBody(status, message, path));
    }

    private Map<String, Object> buildBody(HttpStatus status, String message, String path) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        body.put("path", path);
        return body;
    }
}
