package com.example.loginsystem.exception;

// =============================================================================
// UserNotFoundException — User doesn't exist in database
// =============================================================================
//
// HTTP Status: 404 Not Found
// Scenario: Trying to load a user by ID that doesn't exist

public class UserNotFoundException extends AuthException {
    public UserNotFoundException(String message) {
        super(message);
    }
}
