package com.example.loginsystem.security;

// =============================================================================
// JwtAuthFilter.java — The JWT Authentication Filter
// =============================================================================
//
// WHAT IS A FILTER?
// ─────────────────
// In Spring (and Java web apps in general), a Filter is a piece of code
// that runs on EVERY HTTP request BEFORE it reaches your controllers.
// Filters form a "chain" — each one processes the request and passes it on.
//
// FILTER CHAIN VISUALIZATION:
//
//   HTTP Request
//       ↓
//   [ JwtAuthFilter ]  ← WE ARE HERE — runs first
//       ↓
//   [ CorsFilter ]
//       ↓
//   [ Other Security Filters ]
//       ↓
//   [ DispatcherServlet ]
//       ↓
//   [ Your Controller ]
//
// WHAT THIS FILTER DOES (on every request):
// ──────────────────────────────────────────
//   1. Look for "Authorization: Bearer <token>" header
//   2. Extract the JWT token
//   3. Validate the token (signature + expiry)
//   4. Extract username from the token
//   5. Load the user from the database
//   6. Create an Authentication object
//   7. Set it in the SecurityContext
//
// SECURITY CONTEXT:
//   A thread-local container that Spring Security uses to know "who is
//   currently logged in" for this request. Once you set an Authentication
//   object here, Spring considers the user authenticated for this request.
//
// EXTENDS OncePerRequestFilter:
//   Guarantees this filter runs exactly ONCE per request (not once per
//   dispatch). Some filters can run multiple times if a request is
//   forwarded internally.
//
// =============================================================================

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);

    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;

    // =========================================================================
    // THE MAIN FILTER METHOD
    // =========================================================================
    //
    // Spring calls this on every incoming HTTP request.
    // @NonNull = IDE hint that these params should never be null

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain  // the rest of the filter chain
    ) throws ServletException, IOException {

        // ------------------------------------------------------------------
        // STEP 1: Extract the Authorization header
        // ------------------------------------------------------------------
        //
        // The client sends:  Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
        //                    ─────────────  ──────  ─────────────────────
        //                    header name    token   the JWT itself
        //                                  type

        final String authHeader = request.getHeader("Authorization");

        // If there's no Authorization header, or it doesn't start with "Bearer ",
        // this request is not using JWT — pass it along to the next filter.
        // Spring Security will handle it (probably reject it if the endpoint is protected).
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response); // pass to next filter
            return; // ← exit this filter
        }

        // ------------------------------------------------------------------
        // STEP 2: Extract the token from the header
        // ------------------------------------------------------------------
        //
        // "Bearer eyJhbGciOiJIUzI1NiJ9..."
        //   0123456 ← "Bearer " is 7 characters, so we start at index 7

        final String jwt = authHeader.substring(7);

        // ------------------------------------------------------------------
        // STEP 3: Extract the username from the token
        // ------------------------------------------------------------------
        //
        // jwtUtil.extractUsername() parses and verifies the token signature.
        // If the token is malformed or tampered with, it throws an exception.

        final String username;
        try {
            username = jwtUtil.extractUsername(jwt);
        } catch (Exception e) {
            // Token is invalid (malformed, expired, wrong signature, etc.)
            // Log it (good for debugging/monitoring) and let the request continue
            // without authentication. Spring Security will reject it later.
            log.warn("Could not extract username from JWT: {}", e.getMessage());
            filterChain.doFilter(request, response);
            return;
        }

        // ------------------------------------------------------------------
        // STEP 4: Authenticate the user (if not already authenticated)
        // ------------------------------------------------------------------
        //
        // SecurityContextHolder.getContext().getAuthentication() returns
        // the current authentication object for THIS request.
        //
        // We only proceed if:
        //   a) We got a username from the token (username != null)
        //   b) The user is NOT already authenticated for this request
        //      (avoids redundant processing if something already authenticated them)

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            // Load the full user from the database
            // (needed to get roles/authorities and verify account is still active)
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            // Validate the token against the loaded user
            // (checks: token username matches DB user, token not expired)
            if (jwtUtil.isTokenValid(jwt, userDetails)) {

                // Create an Authentication object.
                // UsernamePasswordAuthenticationToken is Spring Security's standard
                // holder for username/password-based authentication.
                //
                // Parameters:
                //   principal   = the UserDetails (who is authenticated)
                //   credentials = null (we already verified, no need to keep password)
                //   authorities = the user's roles/permissions
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null, // credentials null = already authenticated
                                userDetails.getAuthorities()
                        );

                // Add extra request details (IP address, session ID) to the token.
                // Useful for audit logging.
                authToken.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );

                // !! THE KEY STEP !!
                // Set the authentication in the SecurityContext.
                // This is what tells Spring Security: "This user is authenticated."
                // From this point forward in the request lifecycle, Spring knows
                // WHO is making the request.
                SecurityContextHolder.getContext().setAuthentication(authToken);

                log.debug("Authenticated user '{}' via JWT", username);
            }
        }

        // ------------------------------------------------------------------
        // STEP 5: Pass the request to the next filter in the chain
        // ------------------------------------------------------------------
        //
        // ALWAYS call this at the end, or the request will never reach
        // your controllers! This is a common mistake.

        filterChain.doFilter(request, response);
    }
}
