package com.gymtracker.presentation.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * GlobalExceptionHandler - centralized exception handler for entire application
 * 
 * How it works:
 * 1. Every time a controller throws ANY exception, this handler catches it
 * 2. Based on exception type, returns appropriate HTTP status code
 * 3. Returns standardized JSON error response to client
 * 4. Logs error details server-side for debugging
 * 
 * @RestControllerAdvice = applies to all @RestController classes in the app
 *                       Any method with @ExceptionHandler catches specified
 *                       exception type
 * 
 *                       Error Response Format (same for all errors):
 *                       {
 *                       "timestamp": "2026-03-15T10:30:45",
 *                       "status": 400,
 *                       "error": "Bad Request",
 *                       "message": "Email already exists",
 *                       "path": "/api/auth/register"
 *                       }
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handle validation errors (from @Valid annotation)
     * 
     * When controller has @Valid @RequestBody and validation fails:
     * - @NotBlank email is empty
     * - @Email email format invalid
     * - @Size password not 8-128 chars
     * - @Positive height is negative
     * 
     * Returns 400 Bad Request with field-level error details
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex,
            WebRequest request) {

        // Extract field names and error messages from validation errors
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> fieldErrors.put(error.getField(), error.getDefaultMessage()));

        String errorMessage = "Validation failed: " + fieldErrors;

        ErrorResponse errorResponse = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                errorMessage,
                request.getDescription(false).replace("uri=", ""));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handle IllegalArgumentException
     * 
     * Thrown by our business logic for recoverable errors:
     * - Email already exists (during registration)
     * - Invalid email or password (during login)
     * - User not found
     * - Invalid or expired refresh token
     * 
     * Returns 400 Bad Request (client mistake, not server error)
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException ex,
            WebRequest request) {

        ErrorResponse errorResponse = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                ex.getMessage(),
                request.getDescription(false).replace("uri=", ""));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handle all other unexpected exceptions
     * 
     * Any exception not explicitly handled above (database errors, NPE, etc.)
     * Returns 500 Internal Server Error - indicates server-side problem
     * 
     * In production, would NOT send full exception details to client
     * Would log full stack trace server-side for debugging
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(
            Exception ex,
            WebRequest request) {

        // In production, log full stack trace here
        ex.printStackTrace();

        ErrorResponse errorResponse = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                "An unexpected error occurred",
                request.getDescription(false).replace("uri=", ""));

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    /**
     * ErrorResponse - standardized error response sent to client
     * 
     * Fields:
     * - timestamp: When error occurred (ISO-8601 format)
     * - status: HTTP status code (400, 401, 500, etc.)
     * - error: HTTP status phrase (Bad Request, Unauthorized, etc.)
     * - message: Human-readable error description
     * - path: API endpoint that failed (e.g., /api/auth/register)
     */
    public record ErrorResponse(
            LocalDateTime timestamp,
            int status,
            String error,
            String message,
            String path) {
    }
}
