package com.example.loginsystem.dto;

// =============================================================================
// AuthResponse.java — DTO for Authentication Response
// =============================================================================
//
// After a successful login OR registration, we return this to the client:
//   {
//     "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ...",
//     "tokenType": "Bearer",
//     "username": "alice",
//     "email": "alice@example.com"
//   }
//
// The CLIENT then stores this token (usually in localStorage or a cookie)
// and sends it in every future request as:
//   Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
//
// TOKEN TYPE "Bearer":
// ─────────────────────
// "Bearer" is from RFC 6750 — it means "the bearer (holder) of this token
// is authorized". It's the standard prefix for JWT tokens in HTTP headers.
// The word comes from "bearer bond" — whoever holds it has access.
//
// WHAT WE INTENTIONALLY OMIT FROM THIS RESPONSE:
//   ✗ password hash  ← never send this!
//   ✗ internal id    ← not needed by clients
//   ✗ createdAt      ← implementation detail

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private String token;

    // "Bearer" — always the same, but included for RFC compliance
    @Builder.Default
    private String tokenType = "Bearer";

    private String username;
    private String email;

    // Include roles so the frontend knows what to show/hide
    // (e.g., show Admin panel only for ROLE_ADMIN users)
    private Set<String> roles;
}
