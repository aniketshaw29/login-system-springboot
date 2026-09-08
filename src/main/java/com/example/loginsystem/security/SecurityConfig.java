package com.example.loginsystem.security;

// =============================================================================
// SecurityConfig.java — The Master Security Configuration
// =============================================================================
//
// WHAT IS THIS?
// ─────────────
// This is the BRAIN of Spring Security for your application.
// It defines:
//   1. Which endpoints are public vs protected
//   2. How to authenticate (stateless JWT, no sessions)
//   3. What password encoder to use (BCrypt)
//   4. How the AuthenticationManager is wired together
//   5. CORS and CSRF settings
//
// @Configuration = tells Spring: "this class defines beans (configuration)"
// @EnableWebSecurity = activates Spring Security's web security support
//
// HOW SPRING SECURITY WORKED BEFORE (old style — WebSecurityConfigurerAdapter):
//   You'd extend WebSecurityConfigurerAdapter and override configure().
//   This was deprecated in Spring Security 5.7 / Boot 2.7.
//
// HOW IT WORKS NOW (Spring Boot 3.x — @Bean style):
//   You define @Bean methods. Spring detects and wires them automatically.
//   Much cleaner and more composable.
//
// =============================================================================

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity // Enables @PreAuthorize, @PostAuthorize on methods
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final JwtAuthFilter jwtAuthFilter;

    // =========================================================================
    // BEAN 1: SecurityFilterChain — The Main Security Rules
    // =========================================================================
    //
    // This @Bean defines the HTTP security rules.
    // Spring Security picks up any SecurityFilterChain bean automatically.

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // -----------------------------------------------------------------
            // CSRF (Cross-Site Request Forgery) Protection — DISABLED
            // -----------------------------------------------------------------
            //
            // WHAT IS CSRF?
            // A CSRF attack tricks a logged-in user's browser into making
            // an unwanted request to your server (using their session cookie).
            //
            // Example attack:
            //   User is logged into bank.com.
            //   Evil website has: <img src="http://bank.com/transfer?to=hacker&amount=1000">
            //   Bank's server sees a valid session cookie → executes transfer!
            //
            // WHY WE DISABLE IT FOR APIs:
            //   CSRF attacks exploit cookies (which browsers send automatically).
            //   We use JWTs in the Authorization header (browsers DON'T auto-send these).
            //   Therefore, CSRF attacks don't work against our API.
            //   Disabling CSRF is standard practice for stateless JWT REST APIs.

            .csrf(AbstractHttpConfigurer::disable)

            // -----------------------------------------------------------------
            // Authorization Rules — Who can access what?
            // -----------------------------------------------------------------
            //
            // Rules are evaluated in ORDER. First match wins.
            // Be careful about order — more specific rules go FIRST.

            .authorizeHttpRequests(auth -> auth

                // PUBLIC endpoints — no authentication needed
                // ─────────────────────────────────────────────
                // Anyone can call these (login, register, H2 console, etc.)

                .requestMatchers("/api/auth/**").permitAll()
                // /api/auth/login, /api/auth/register — obviously public

                .requestMatchers("/h2-console/**").permitAll()
                // H2 console for development (disable in production!)

                .requestMatchers("/actuator/health").permitAll()
                // Health check endpoint (for load balancers, monitoring)

                .requestMatchers("/api/ping").permitAll()
                // Simple ping — useful to verify the app is up without auth

                // ROLE-SPECIFIC endpoints
                // ─────────────────────────
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                // hasRole("ADMIN") → internally checks for "ROLE_ADMIN" authority
                // Only users with ROLE_ADMIN can access /api/admin/*

                // CATCH-ALL: everything else requires authentication
                // ─────────────────────────────────────────────────────
                .anyRequest().authenticated()
                // Any request not matched above must be authenticated
            )

            // -----------------------------------------------------------------
            // SESSION MANAGEMENT — STATELESS
            // -----------------------------------------------------------------
            //
            // STATEFUL (traditional): Server creates a session, stores user
            //   info in memory, sends a session ID cookie to the client.
            //   Problem: doesn't scale well (state on server), hard to use
            //   across multiple servers without a shared session store.
            //
            // STATELESS (JWT approach): Server creates no session. All user
            //   info is in the JWT token the client sends each request.
            //   Scales perfectly — any server can validate any token.
            //
            // ALWAYS_SESSION_LESS = never create HTTP sessions

            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // -----------------------------------------------------------------
            // Authentication Provider
            // -----------------------------------------------------------------
            //
            // Tells Spring Security HOW to authenticate:
            //   "Use our DaoAuthenticationProvider (defined below)"

            .authenticationProvider(authenticationProvider())

            // -----------------------------------------------------------------
            // Add Our JWT Filter
            // -----------------------------------------------------------------
            //
            // addFilterBefore(A, B) = "run filter A before filter B"
            //
            // We add JwtAuthFilter BEFORE UsernamePasswordAuthenticationFilter.
            // UsernamePasswordAuthenticationFilter is Spring's default username/
            // password form login filter. We want our JWT check to run first,
            // so Spring already knows who the user is before that filter runs.

            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)

            // -----------------------------------------------------------------
            // H2 Console Fix
            // -----------------------------------------------------------------
            //
            // H2 console uses HTML frames internally.
            // Spring Security blocks frames by default (X-Frame-Options: DENY).
            // We allow frames only from the same origin for H2 console to work.
            //
            // This is only needed in development (when H2 console is enabled).

            .headers(headers ->
                headers.frameOptions(frame -> frame.sameOrigin())
            );

        return http.build();
    }

    // =========================================================================
    // BEAN 2: PasswordEncoder — BCrypt
    // =========================================================================
    //
    // BCrypt is the gold standard for password hashing:
    //   • Automatically includes a random SALT (prevents rainbow table attacks)
    //   • Has a configurable cost factor (default 10 = ~100ms to hash)
    //   • The same password hashed twice gives DIFFERENT hashes (due to salt)
    //     but BCryptPasswordEncoder.matches() still returns true!
    //
    // NEVER use MD5 or SHA for passwords — they're too fast (can be brute-forced)
    // and don't include salts.
    //
    // Cost factor 10 = 2^10 = 1024 rounds of hashing.
    // Higher = slower = more secure but more CPU. 10-12 is the common sweet spot.

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    // =========================================================================
    // BEAN 3: AuthenticationProvider — DaoAuthenticationProvider
    // =========================================================================
    //
    // This is the component that ACTUALLY performs authentication.
    // "Dao" = Data Access Object — it loads the user from a DAO (repository).
    //
    // When AuthenticationManager.authenticate() is called with
    // username + password, DaoAuthenticationProvider:
    //   1. Calls userDetailsService.loadUserByUsername(username) → gets stored hash
    //   2. Calls passwordEncoder.matches(rawPassword, storedHash) → true/false
    //   3. Returns authenticated token or throws BadCredentialsException

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService); // HOW to load users
        provider.setPasswordEncoder(passwordEncoder());     // HOW to verify passwords
        return provider;
    }

    // =========================================================================
    // BEAN 4: AuthenticationManager
    // =========================================================================
    //
    // AuthenticationManager is the high-level API for authentication.
    // You call: authManager.authenticate(new UsernamePasswordAuthenticationToken(...))
    // It delegates to the configured AuthenticationProvider(s).
    //
    // We expose it as a @Bean so our AuthService can @Autowired it.
    // Spring provides the AuthenticationConfiguration with the current config.

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
