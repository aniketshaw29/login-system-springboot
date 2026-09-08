# 04 — Spring Security Filter Chain

## What Is the Filter Chain?

Every HTTP request to your app passes through a series of **filters** before
reaching your controller. Spring Security inserts its own filters into this
pipeline to handle authentication and authorization.

```
Incoming HTTP Request
         ↓
┌─────────────────────────────────────────┐
│         Spring Security Filter Chain    │
│                                         │
│  ┌─────────────────────────────────┐    │
│  │  SecurityContextPersistenceFilter│    │  ← Load/Save SecurityContext
│  └───────────────┬─────────────────┘    │
│                  ↓                      │
│  ┌─────────────────────────────────┐    │
│  │  JwtAuthFilter (OUR CUSTOM ONE) │    │  ← Validate JWT, set Authentication
│  └───────────────┬─────────────────┘    │
│                  ↓                      │
│  ┌─────────────────────────────────┐    │
│  │  UsernamePasswordAuthFilter     │    │  ← Default Spring form login (bypassed by JWT)
│  └───────────────┬─────────────────┘    │
│                  ↓                      │
│  ┌─────────────────────────────────┐    │
│  │  ExceptionTranslationFilter     │    │  ← Convert security exceptions → 401/403
│  └───────────────┬─────────────────┘    │
│                  ↓                      │
│  ┌─────────────────────────────────┐    │
│  │  FilterSecurityInterceptor      │    │  ← Check authorization rules
│  └───────────────┬─────────────────┘    │
└──────────────────┼──────────────────────┘
                   ↓
         Your Controller (if allowed)
```

---

## JwtAuthFilter: What It Does Step by Step

```
Request arrives → JwtAuthFilter.doFilterInternal()
                        ↓
               Is there an Authorization header?
                  NO ──────────────────────────────→ pass to next filter
                  YES
                        ↓
               Does it start with "Bearer "?
                  NO ──────────────────────────────→ pass to next filter
                  YES
                        ↓
               Extract token (substring after "Bearer ")
                        ↓
               Parse token → extract username
               (validates signature + format)
                  INVALID ─────────────────────────→ log warning, pass to next filter
                  VALID
                        ↓
               Is user already authenticated for this request?
                  YES ─────────────────────────────→ pass to next filter
                  NO
                        ↓
               Load UserDetails from database
                        ↓
               Is token still valid? (not expired, username matches)
                  NO ──────────────────────────────→ pass to next filter (unauthenticated)
                  YES
                        ↓
               Create UsernamePasswordAuthenticationToken
                        ↓
               Set in SecurityContextHolder    ← "This user is authenticated"
                        ↓
               Pass to next filter
```

---

## The SecurityContext

The `SecurityContextHolder` is a **thread-local** container — it stores the
current user's authentication for the duration of the current HTTP request
(on the current thread).

```java
// Set authentication (done in JwtAuthFilter)
SecurityContextHolder.getContext().setAuthentication(authToken);

// Get authentication (done anywhere in your code)
Authentication auth = SecurityContextHolder.getContext().getAuthentication();
String username = auth.getName();

// Spring injects it automatically via @AuthenticationPrincipal
@GetMapping("/me")
public String me(@AuthenticationPrincipal UserDetails user) {
    return user.getUsername(); // no manual SecurityContext access needed!
}
```

---

## Why `OncePerRequestFilter`?

Spring's normal filters can run multiple times if the request is "forwarded"
internally (e.g., error dispatches). `OncePerRequestFilter` guarantees your
filter runs **exactly once per request**, regardless of internal dispatching.

This matters for JWT validation — you don't want to re-authenticate the same
request multiple times.

---

## `addFilterBefore` in SecurityConfig

```java
.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
```

This inserts `JwtAuthFilter` immediately **before** Spring's default
`UsernamePasswordAuthenticationFilter`. This way:
1. Our JWT filter runs first, validates the token, sets the Authentication
2. Spring's filter runs and sees "already authenticated" → skips form login

If we added it **after**, Spring's filter would try (and fail) to handle
the JWT as a form login first.

---

## In Our Code

See [JwtAuthFilter.java](../src/main/java/com/example/loginsystem/security/JwtAuthFilter.java)
See [SecurityConfig.java](../src/main/java/com/example/loginsystem/security/SecurityConfig.java)
