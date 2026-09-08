package com.example.loginsystem.exception;

// =============================================================================
// ForbiddenException — User is authenticated but lacks permission for resource
// =============================================================================
//
// HTTP Status: 403 Forbidden
// Scenario: User tries to access /api/admin/users but only has ROLE_USER

public class ForbiddenException extends AuthException {
    public ForbiddenException(String message) {
        super(message);
    }
}
