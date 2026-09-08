package com.example.loginsystem.exception;

// =============================================================================
// InvalidTokenException — JWT token is malformed, expired, or has bad signature
// =============================================================================
//
// HTTP Status: 401 Unauthorized
// Scenario: Token was tampered with, or expiration time has passed

public class InvalidTokenException extends AuthException {
    public InvalidTokenException(String message) {
        super(message);
    }

    public InvalidTokenException(String message, Throwable cause) {
        super(message, cause);
    }
}
