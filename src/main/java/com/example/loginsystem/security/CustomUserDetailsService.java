package com.example.loginsystem.security;

// =============================================================================
// CustomUserDetailsService.java — Loads User from Database for Spring Security
// =============================================================================
//
// WHAT IS UserDetailsService?
// ───────────────────────────
// Spring Security doesn't know about YOUR User entity. It uses its own
// interface: UserDetails (which has username, password, authorities).
//
// UserDetailsService is the BRIDGE between your database and Spring Security.
// You implement ONE method: loadUserByUsername(String username)
// → Spring calls this to get user info during authentication.
//
// THE AUTHENTICATION FLOW:
//
//   POST /api/auth/login
//       ↓
//   AuthenticationManager.authenticate(usernamePasswordToken)
//       ↓
//   Spring Security calls: loadUserByUsername("alice")
//       ↓
//   We query the DB, return UserDetails
//       ↓
//   Spring Security compares: BCrypt.matches(rawPassword, storedHash)
//       ↓
//   Authentication succeeds or fails
//
// =============================================================================

import com.example.loginsystem.entity.User;
import com.example.loginsystem.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

// @Service = Spring-managed bean (like @Component but semantically = service layer)
// @RequiredArgsConstructor = Lombok generates a constructor for all final fields
//   This is the recommended way to inject dependencies (constructor injection)
//   instead of @Autowired on a field (field injection)

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    // Constructor injection (via @RequiredArgsConstructor)
    // Spring sees: "CustomUserDetailsService needs a UserRepository"
    // Spring finds the UserRepository bean it created, passes it in.
    private final UserRepository userRepository;

    // =========================================================================
    // THE KEY METHOD — Spring Security calls this during authentication
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    // @Transactional ensures this method runs in a database transaction.
    // readOnly = true is an optimization hint for databases (no writes needed).
    // This is needed here because we load EAGER roles (User.roles Set).
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        // 1. Look up the user from the database
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    // If user not found, throw the specific Spring Security exception.
                    // Spring Security catches this and treats it as authentication failure.
                    // SECURITY NOTE: Don't say "password wrong" vs "user not found" —
                    // that leaks information. Generic "not found" message is safer.
                    return new UsernameNotFoundException(
                            "User not found with username: " + username);
                });

        // 2. Check if the account is active
        if (!user.isActive()) {
            throw new UsernameNotFoundException("Account is disabled for user: " + username);
        }

        // 3. Convert our User entity to Spring Security's UserDetails format.
        //
        // Spring Security's UserDetails has:
        //   - getUsername()    → the username
        //   - getPassword()    → the STORED (hashed) password for comparison
        //   - getAuthorities() → a collection of GrantedAuthority (roles/permissions)
        //   - isAccountNonExpired(), isAccountNonLocked(), isCredentialsNonExpired()
        //   - isEnabled()
        //
        // We use org.springframework.security.core.userdetails.User (the Spring class,
        // not our User entity!) which is a built-in implementation of UserDetails.
        //
        // user.getRoles() → Set<Role> → map to SimpleGrantedAuthority
        // SimpleGrantedAuthority("ROLE_USER") = tells Spring what this user CAN do

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPassword()) // Spring Security will BCrypt-compare this
                .authorities(
                    user.getRoles().stream()
                        .map(role -> new SimpleGrantedAuthority(role.name()))
                        // role.name() returns "ROLE_USER" or "ROLE_ADMIN" (the enum name)
                        .collect(Collectors.toList())
                )
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .disabled(!user.isActive())
                .build();
    }
}
