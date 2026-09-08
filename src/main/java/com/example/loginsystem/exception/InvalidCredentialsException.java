package com.example.loginsystem.exception;

// =============================================================================
// InvalidCredentialsException — Wrong password or user not found during login
// =============================================================================
//
// HTTP Status: 401 Unauthorized
// Scenario: User logs in with wrong password or non-existent username
//
// NOTE: We DON'T distinguish between "user not found" vs "wrong password"
// in the error message. This prevents attackers from enumerating valid usernames.

public class InvalidCredentialsException extends AuthException {
    public InvalidCredentialsException(String message) {
        super(message);
    }
}
