package com.example.loginsystem.exception;

// =============================================================================
// AuthException — Base class for all authentication/authorization exceptions
// =============================================================================
//
// WHY A BASE CLASS?
// ──────────────────
// All our custom exceptions inherit from AuthException.
// This lets us catch them all in one @ExceptionHandler method:
//
//   @ExceptionHandler(AuthException.class)
//   public ResponseEntity<ApiResponse> handleAuthException(AuthException ex) {
//       // handles all auth-related errors
//   }
//
// Without the base class, we'd need 5 separate handler methods.
// This is the "polymorphism" principle — one handler for the entire hierarchy.

public class AuthException extends RuntimeException {
    public AuthException(String message) {
        super(message);
    }

    public AuthException(String message, Throwable cause) {
        super(message, cause);
    }
}
