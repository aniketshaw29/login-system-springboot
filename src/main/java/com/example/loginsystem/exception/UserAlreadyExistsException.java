package com.example.loginsystem.exception;

// =============================================================================
// UserAlreadyExistsException — User tries to register with existing username/email
// =============================================================================
//
// HTTP Status: 409 Conflict
// Scenario: User tries to register with username "alice" but "alice" already exists

public class UserAlreadyExistsException extends AuthException {
    public UserAlreadyExistsException(String message) {
        super(message);
    }
}
