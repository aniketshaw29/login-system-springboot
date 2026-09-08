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
// EXCEPTION HIERARCHY:
//   RuntimeException
//       └── AuthException (our custom base)
//           ├── InvalidCredentialsException        → 401 Unauthorized
//           ├── InvalidTokenException              → 401 Unauthorized
//           ├── UserAlreadyExistsException         → 409 Conflict
//           ├── UserNotFoundException              → 404 Not Found
//           └── ForbiddenException                 → 403 Forbidden
//
// HANDLER STRATEGY:
//   1. Most specific handlers first (UserAlreadyExistsException)
//   2. Then general base class handler (AuthException)
//   3. Then framework exceptions (@Valid, AccessDeniedException)
//   4. Finally, catch-all for unexpected exceptions
//
// RESPONSE SHAPE:
//   { "success": false, "message": "User-friendly error message" }
//
// =============================================================================

import com.example.loginsystem.dto.ApiResponse;
import com.example.loginsystem.exception.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // =========================================================================
    // CUSTOM EXCEPTIONS (most specific first)
    // =========================================================================

    /**
     * User tried to register with a username or email that already exists.
     * HTTP 409 Conflict — resource already exists
     */
    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ApiResponse> handleUserAlreadyExists(UserAlreadyExistsException ex) {
        logger.warn("User already exists: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.CONFLICT) // 409
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * User doesn't exist in the database.
     * HTTP 404 Not Found
     */
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiResponse> handleUserNotFound(UserNotFoundException ex) {
        logger.warn("User not found: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND) // 404
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * Invalid credentials (wrong password or user doesn't exist).
     * HTTP 401 Unauthorized
     *
     * NOTE: We DON'T distinguish between the two cases in the response.
     * If we said "User 'alice' not found" vs "Password wrong", an attacker
     * could enumerate valid usernames. Generic "Invalid credentials" is safer.
     */
    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiResponse> handleInvalidCredentials(InvalidCredentialsException ex) {
        logger.warn("Invalid credentials attempt: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED) // 401
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * JWT token is invalid, malformed, expired, or has a bad signature.
     * HTTP 401 Unauthorized
     */
    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ApiResponse> handleInvalidToken(InvalidTokenException ex) {
        logger.debug("Invalid token: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED) // 401
                .body(ApiResponse.error("Invalid or expired token"));
    }

    /**
     * User is authenticated but lacks permission for the resource.
     * HTTP 403 Forbidden
     *
     * Example: ROLE_USER trying to access /api/admin/users
     */
    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiResponse> handleForbidden(ForbiddenException ex) {
        logger.warn("Access forbidden: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN) // 403
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * Base handler for all other AuthException subclasses.
     * Catches any custom auth exceptions we haven't explicitly handled.
     * HTTP 400 Bad Request (safe default)
     */
    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ApiResponse> handleAuthException(AuthException ex) {
        logger.error("Auth exception: {}", ex.getMessage(), ex);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST) // 400
                .body(ApiResponse.error(ex.getMessage()));
    }

    // =========================================================================
    // SPRING FRAMEWORK EXCEPTIONS
    // =========================================================================

    /**
     * @Valid validation failed on a @RequestBody parameter.
     * Occurs BEFORE controller method runs if validation fails.
     * HTTP 400 Bad Request
     *
     * Example:
     *   POST /register
     *   { "username": "a", "email": "not-email", "password": "short" }
     *   → MethodArgumentNotValidException with field errors
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse> handleValidationErrors(MethodArgumentNotValidException ex) {
        String errorMessage = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));

        logger.warn("Validation error: {}", errorMessage);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST) // 400
                .body(ApiResponse.error("Validation failed: " + errorMessage));
    }

    /**
     * Spring Security's @PreAuthorize failed (user lacks required role).
     * HTTP 403 Forbidden
     *
     * Example: Method has @PreAuthorize("hasRole('ADMIN')") but user is ROLE_USER
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse> handleAccessDenied(AccessDeniedException ex) {
        logger.warn("Access denied: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN) // 403
                .body(ApiResponse.error("You don't have permission to access this resource"));
    }

    /**
     * Authentication-related exception from Spring Security (not caught by above handlers).
     * Examples: LockedException, DisabledException, CredentialsExpiredException
     * HTTP 401 Unauthorized
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse> handleAuthenticationException(AuthenticationException ex) {
        logger.warn("Authentication exception: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED) // 401
                .body(ApiResponse.error("Authentication failed: " + ex.getMessage()));
    }

    // =========================================================================
    // CATCH-ALL FOR UNEXPECTED EXCEPTIONS
    // =========================================================================

    /**
     * Catch any other exception that wasn't explicitly handled.
     * HTTP 500 Internal Server Error
     *
     * This is the safety net. In production, log these carefully — they indicate
     * bugs in your code that need investigation.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse> handleGenericException(Exception ex) {
        logger.error("Unexpected error", ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR) // 500
                .body(ApiResponse.error("An unexpected error occurred. Please try again later."));
    }
}
