package com.example.loginsystem.controller;

// =============================================================================
// UserController.java — Example Protected Endpoints (requires valid JWT)
// =============================================================================
//
// These endpoints require authentication.
// If you call them without a valid JWT token, Spring Security returns:
//   401 Unauthorized
//
// If you call the admin endpoint with a non-admin token, you get:
//   403 Forbidden
//
// HOW TO TEST:
//   1. Register or login to get a token
//   2. Copy the token from the response
//   3. Add header: Authorization: Bearer <your-token>
//   4. Call the endpoint
//
// =============================================================================

import com.example.loginsystem.entity.User;
import com.example.loginsystem.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    // =========================================================================
    // GET /api/user/profile — For any authenticated user
    // =========================================================================
    //
    // @AuthenticationPrincipal:
    //   Spring injects the currently logged-in user's UserDetails automatically.
    //   Spring Security read it from the SecurityContext (which JwtAuthFilter set).
    //   No need to parse the token manually in the controller.

    @GetMapping("/user/profile")
    public ResponseEntity<Map<String, String>> getProfile(
            @AuthenticationPrincipal UserDetails userDetails) {

        return ResponseEntity.ok(Map.of(
                "message", "Welcome, " + userDetails.getUsername() + "!",
                "username", userDetails.getUsername(),
                "authorities", userDetails.getAuthorities().toString()
        ));
    }

    // =========================================================================
    // GET /api/admin/users — Admin only
    // =========================================================================
    //
    // @PreAuthorize is METHOD-LEVEL security (requires @EnableMethodSecurity in config).
    // It's evaluated BEFORE the method runs.
    //
    // hasRole("ADMIN") checks if the authenticated user has "ROLE_ADMIN".
    // Spring automatically prepends "ROLE_" when using hasRole().
    // (Use hasAuthority("ROLE_ADMIN") if you want the full string.)
    //
    // If the user doesn't have ADMIN role → 403 Forbidden is returned.

    @GetMapping("/admin/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> getAllUsers() {

        List<Map<String, Object>> users = userRepository.findAll().stream()
                .map(user -> Map.of(
                        "id", (Object) user.getId(),
                        "username", user.getUsername(),
                        "email", user.getEmail(),
                        "roles", user.getRoles().stream().map(User.Role::name).toList(),
                        "active", user.isActive(),
                        "createdAt", user.getCreatedAt().toString()
                ))
                .toList();

        return ResponseEntity.ok(users);
    }

    // =========================================================================
    // GET /api/ping — Health check (no auth needed — configured in SecurityConfig)
    // =========================================================================

    @GetMapping("/ping")
    public ResponseEntity<Map<String, String>> ping() {
        return ResponseEntity.ok(Map.of(
                "status", "ok",
                "message", "Login System is running!"
        ));
    }
}
