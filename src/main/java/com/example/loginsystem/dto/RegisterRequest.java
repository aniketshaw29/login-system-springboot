package com.example.loginsystem.dto;

// =============================================================================
// RegisterRequest.java — DTO for User Registration
// =============================================================================
//
// WHAT IS A DTO?
// ──────────────
// DTO = Data Transfer Object. It's a plain Java class whose only job is
// to CARRY DATA between layers (Controller ↔ Service) or over the network.
//
// WHY NOT JUST USE THE ENTITY (User.java) DIRECTLY?
// ──────────────────────────────────────────────────
// Several reasons:
//
//  1. SECURITY: The User entity has a 'password' field. If you return
//     a User entity as JSON, you might accidentally expose the hash.
//     DTOs let you control EXACTLY what data goes in and out.
//
//  2. SEPARATION OF CONCERNS: Your entity is designed for the database.
//     Your DTO is designed for the API. Different shapes for different purposes.
//
//  3. VALIDATION: You want validation on the incoming request, not on
//     the entity (which is already valid by the time you store it).
//
//  4. FLEXIBILITY: API shape and DB shape can evolve independently.
//
// EXAMPLE FLOW:
//   POST /api/auth/register
//   Body: { "username": "alice", "email": "a@a.com", "password": "secret" }
//       ↓ Spring deserializes JSON into RegisterRequest
//   Controller receives RegisterRequest (NOT a User entity)
//       ↓ Service creates User from RegisterRequest
//   User entity is saved to database

import jakarta.validation.constraints.*;
import lombok.Data;

// @Data from Lombok = @Getter + @Setter + @ToString + @EqualsAndHashCode
// Perfect for DTOs — just data, no behavior.

@Data
public class RegisterRequest {

    // @NotBlank checks: not null AND not empty AND not only whitespace
    // message = the error message returned to the client if validation fails
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be 3-50 characters")
    // This pattern only allows letters, digits, underscores, hyphens
    @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "Username can only contain letters, numbers, underscores and hyphens")
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    private String email;

    // Password validation — enforce strong passwords at the API boundary
    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 100, message = "Password must be at least 8 characters")
    private String password;
}
