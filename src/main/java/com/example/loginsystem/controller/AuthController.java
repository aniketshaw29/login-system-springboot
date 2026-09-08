package com.example.loginsystem.controller;

// =============================================================================
// AuthController.java — The HTTP Layer (REST API Endpoints)
// =============================================================================
//
// WHAT IS A CONTROLLER?
// ──────────────────────
// The Controller is the ENTRY POINT for HTTP requests. It:
//   1. Maps HTTP requests to Java methods (@GetMapping, @PostMapping)
//   2. Deserializes the request body (JSON → Java object)
//   3. Validates the request data (@Valid)
//   4. Calls the Service to do the actual work
//   5. Returns a response (Java object → JSON)
//
// IT DOES NOT contain business logic — that's the Service's job.
// The controller is just a "traffic director".
//
// REST API DESIGN:
// ────────────────
//   POST /api/auth/register  → create a new account
//   POST /api/auth/login     → authenticate and get a token
//   GET  /api/auth/me        → get current user info (requires token)
//   GET  /api/user/profile   → example of a protected endpoint
//   GET  /api/admin/users    → example admin-only endpoint
//
// HTTP STATUS CODES we use:
//   200 OK         = success
//   201 Created    = resource successfully created (registration)
//   400 Bad Request = validation error or bad input
//   401 Unauthorized = not authenticated (no/invalid token)
//   403 Forbidden  = authenticated but not authorized (wrong role)
//   409 Conflict   = resource already exists (duplicate username/email)
//   500 Internal Server Error = unexpected error
//
// =============================================================================

import com.example.loginsystem.dto.AuthResponse;
import com.example.loginsystem.dto.LoginRequest;
import com.example.loginsystem.dto.RegisterRequest;
import com.example.loginsystem.entity.User;
import com.example.loginsystem.repository.UserRepository;
import com.example.loginsystem.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

// @RestController = @Controller + @ResponseBody
//   @Controller = this class handles HTTP requests
//   @ResponseBody = serialize return values to JSON (don't look for view templates)
//
// @RequestMapping("/api/auth") = all methods in this class start with /api/auth

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;

    // =========================================================================
    // POST /api/auth/register
    // =========================================================================
    //
    // REQUEST BODY (JSON):
    //   {
    //     "username": "alice",
    //     "email": "alice@example.com",
    //     "password": "mySecret123"
    //   }
    //
    // RESPONSE (201 Created):
    //   {
    //     "token": "eyJhbGci...",
    //     "tokenType": "Bearer",
    //     "username": "alice",
    //     "email": "alice@example.com",
    //     "roles": ["ROLE_USER"]
    //   }
    //
    // @Valid → triggers Bean Validation on RegisterRequest.
    //   If any @NotBlank/@Email/@Size fails, Spring returns 400 automatically
    //   BEFORE this method even runs (MethodArgumentNotValidException).
    //
    // @RequestBody → tells Spring: "parse the JSON body into this object"

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        // 201 Created — we made something new
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // =========================================================================
    // POST /api/auth/login
    // =========================================================================
    //
    // REQUEST BODY (JSON):
    //   {
    //     "username": "alice",
    //     "password": "mySecret123"
    //   }
    //
    // RESPONSE (200 OK):
    //   {
    //     "token": "eyJhbGci...",
    //     ...
    //   }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response); // 200 OK
    }

    // =========================================================================
    // GET /api/auth/me  — Get current user info
    // =========================================================================
    //
    // This endpoint requires a valid JWT token.
    // Spring Security has already validated the token (via JwtAuthFilter)
    // before this method runs.
    //
    // @AuthenticationPrincipal UserDetails userDetails:
    //   Spring Security injects the currently authenticated UserDetails.
    //   This is the same UserDetails that JwtAuthFilter put in the SecurityContext.
    //   No need to parse the token again!
    //
    // AUTHORIZATION HEADER required:
    //   Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser(
            @AuthenticationPrincipal UserDetails userDetails) {

        // Load the full User entity from DB using the username from the token
        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow();

        // Return a safe subset of user data (no password hash!)
        Map<String, Object> userInfo = Map.of(
                "id", user.getId(),
                "username", user.getUsername(),
                "email", user.getEmail(),
                "roles", user.getRoles().stream().map(User.Role::name).toList(),
                "active", user.isActive(),
                "createdAt", user.getCreatedAt().toString()
        );

        return ResponseEntity.ok(userInfo);
    }
}

// =============================================================================
// UserController.java — Regular User Endpoints (protected)
// =============================================================================
// NOTE: In a real project, this would be its own file.
// Putting it here as a second class in a comment to show the concept.
// =============================================================================

// @RestController
// @RequestMapping("/api/user")
// @RequiredArgsConstructor
// class UserController {
//
//     @GetMapping("/profile")
//     public ResponseEntity<String> getProfile(@AuthenticationPrincipal UserDetails user) {
//         // Any authenticated user can reach this
//         return ResponseEntity.ok("Hello, " + user.getUsername() + "!");
//     }
// }
