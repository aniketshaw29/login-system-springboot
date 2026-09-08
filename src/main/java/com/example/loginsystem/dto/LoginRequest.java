package com.example.loginsystem.dto;

// =============================================================================
// LoginRequest.java — DTO for User Login
// =============================================================================
//
// The client sends this JSON body to POST /api/auth/login:
//   {
//     "username": "alice",
//     "password": "mySecret123"
//   }
//
// Spring Boot's Jackson library automatically converts (deserializes)
// the incoming JSON into this Java object.

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "Password is required")
    private String password;
}
