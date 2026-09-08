package com.example.loginsystem.dto;

// =============================================================================
// ApiResponse.java — Generic API Response Wrapper
// =============================================================================
//
// Instead of returning raw objects or strings, wrapping all API responses
// in a consistent envelope improves the API:
//
//   Success: { "success": true,  "message": "User registered successfully" }
//   Failure: { "success": false, "message": "Username already taken" }
//
// WHY CONSISTENT RESPONSE SHAPES?
// ────────────────────────────────
// Frontend developers (and API consumers) can write one error-handling
// function instead of guessing the shape of each endpoint's response.
// It's a form of "contract" between the backend and frontend.

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse {

    private boolean success;
    private String message;

    // Optional data payload — null if not applicable
    // Using Object allows any type (String, List, Map, etc.)
    private Object data;

    // Convenience factory methods (no need to use @Builder every time)

    // Create a success response with just a message
    public static ApiResponse success(String message) {
        return ApiResponse.builder()
                .success(true)
                .message(message)
                .build();
    }

    // Create a success response with data (e.g., return the token)
    public static ApiResponse success(String message, Object data) {
        return ApiResponse.builder()
                .success(true)
                .message(message)
                .data(data)
                .build();
    }

    // Create an error response
    public static ApiResponse error(String message) {
        return ApiResponse.builder()
                .success(false)
                .message(message)
                .build();
    }
}
