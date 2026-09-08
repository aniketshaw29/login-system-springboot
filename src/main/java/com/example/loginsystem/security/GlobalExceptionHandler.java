package com.example.loginsystem.security;

// =============================================================================
// GlobalExceptionHandler.java — Centralized Error Handling
// =============================================================================
//
// WHAT IS @ControllerAdvice?
// ──────────────────────────
// @ControllerAdvice is a global "catch-all" for exceptions thrown anywhere
// in your controllers (or services, if they bubble up). Without it, Spring
// would return a generic 500 error page or JSON.
//
// With @RestControllerAdvice (@ControllerAdvice + @ResponseBody), you:
//   1. Catch specific exception types with @ExceptionHandler
//   2. Return clean, structured JSON error responses
//   3. Set the appropriate HTTP status codes
//
// BEFORE: Client gets:  { "timestamp": ..., "status": 500, "error": "Internal Server Error" }
// AFTER:  Client gets:  { "success": false, "message": "Username 'alice' is already taken" }
//
// =============================================================================

import com.example.loginsystem.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // =========================================================================
    // Handle @Valid validation failures
    // =========================================================================
    //
    // When @Valid fails on a @RequestBody, Spring throws MethodArgumentNotValidException.
    // We extract all field errors and join them into one message.
    //
    // Example: username is blank AND email is invalid →
    //   "Username is required; Email must be a valid email address"

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse> handleValidationErrors(MethodArgumentNotValidException ex) {
        String errorMessage = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST) // 400
                .body(ApiResponse.error("Validation failed: " + errorMessage));
    }

    // =========================================================================
    // Handle business rule violations (e.g., duplicate username)
    // =========================================================================

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT) // 409 Conflict (resource already exists)
                .body(ApiResponse.error(ex.getMessage()));
    }

    // =========================================================================
    // Handle wrong password / bad credentials
    // =========================================================================

    @ExceptionHandler({BadCredentialsException.class, RuntimeException.class})
    public ResponseEntity<ApiResponse> handleBadCredentials(RuntimeException ex) {
        if (ex.getMessage() != null && ex.getMessage().contains("Invalid username or password")) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED) // 401
                    .body(ApiResponse.error(ex.getMessage()));
        }
        // For other RuntimeExceptions, return 500
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("An unexpected error occurred"));
    }

    // =========================================================================
    // Handle 403 Forbidden (authenticated but wrong role)
    // =========================================================================

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN) // 403
                .body(ApiResponse.error("Access denied: you don't have permission for this resource"));
    }
}
