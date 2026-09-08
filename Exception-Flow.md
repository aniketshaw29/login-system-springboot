# Exception Flow Diagram and Mapping

## Project Structure (with exceptions)

```
src/main/java/com/example/loginsystem/
├── exception/
│   ├── AuthException.java                    ← Base class
│   ├── UserAlreadyExistsException.java
│   ├── InvalidCredentialsException.java
│   ├── InvalidTokenException.java
│   ├── UserNotFoundException.java
│   └── ForbiddenException.java
│
└── security/
    └── GlobalExceptionHandler.java           ← Catches all exceptions
```

---

## Complete Exception → HTTP Status Mapping

| Exception | HTTP Status | Scenario | Handler |
|-----------|-----------|----------|---------|
| `UserAlreadyExistsException` | 409 | Duplicate username/email during registration | `handleUserAlreadyExists()` |
| `UserNotFoundException` | 404 | User ID doesn't exist | `handleUserNotFound()` |
| `InvalidCredentialsException` | 401 | Wrong password or nonexistent user at login | `handleInvalidCredentials()` |
| `InvalidTokenException` | 401 | JWT expired, malformed, or tampered | `handleInvalidToken()` |
| `ForbiddenException` | 403 | Authenticated but no permission (custom logic) | `handleForbidden()` |
| `AuthException` | 400 | Any other auth exception (fallback) | `handleAuthException()` |
| `MethodArgumentNotValidException` | 400 | @Valid failed on @RequestBody | `handleValidationErrors()` |
| `AccessDeniedException` | 403 | @PreAuthorize or role check failed | `handleAccessDenied()` |
| `AuthenticationException` | 401 | Spring Security auth failed | `handleAuthenticationException()` |
| `Exception` | 500 | Unexpected error | `handleGenericException()` |

---

## Full Request → Response Flow for Each Scenario

### Scenario 1: Register with duplicate username

```
POST /api/auth/register
{ "username": "alice", "email": "new@a.com", "password": "secret" }

    ↓ AuthController.register()
    ↓ validates input with @Valid (passes)
    ↓ AuthService.register()
    ↓ userRepository.existsByUsername("alice") → true
    ↓ throw new UserAlreadyExistsException("Username 'alice' is already taken")
    
    ↓ Exception propagates up, Spring catches it
    ↓ GlobalExceptionHandler.handleUserAlreadyExists()
    ↓ returns ResponseEntity with status 409 + ApiResponse
    
Response:
HTTP/1.1 409 Conflict
{
  "success": false,
  "message": "Username 'alice' is already taken"
}
```

### Scenario 2: Register with invalid data

```
POST /api/auth/register
{ "username": "ab", "email": "not-email", "password": "x" }

    ↓ Spring's @Valid interceptor checks annotations
    ↓ @NotBlank, @Size, @Email fail for multiple fields
    ↓ throw MethodArgumentNotValidException (Spring framework)
    
    ↓ GlobalExceptionHandler.handleValidationErrors()
    ↓ extracts all field errors, joins with "; "
    ↓ returns ResponseEntity with status 400 + ApiResponse
    
Response:
HTTP/1.1 400 Bad Request
{
  "success": false,
  "message": "Validation failed: username: Username must be 3-50 characters; email: Please provide a valid email address; password: Password must be at least 8 characters"
}
```

### Scenario 3: Login with wrong password

```
POST /api/auth/login
{ "username": "alice", "password": "wrongPassword" }

    ↓ AuthController.login()
    ↓ AuthService.login()
    ↓ authenticationManager.authenticate()
    ↓ DaoAuthenticationProvider.authenticate()
    ↓ loads user from DB (found)
    ↓ BCrypt.matches("wrongPassword", hash) → false
    ↓ throw BadCredentialsException (Spring Security)
    
    ↓ caught by AuthService's catch(AuthenticationException e)
    ↓ throw new InvalidCredentialsException("Invalid username or password")
    
    ↓ GlobalExceptionHandler.handleInvalidCredentials()
    ↓ returns ResponseEntity with status 401 + ApiResponse
    
Response:
HTTP/1.1 401 Unauthorized
{
  "success": false,
  "message": "Invalid username or password"
}
```

### Scenario 4: Use expired JWT token

```
GET /api/auth/me
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...   (24+ hours old)

    ↓ JwtAuthFilter.doFilterInternal()
    ↓ extracts token from header
    ↓ jwtUtil.extractUsername(jwt)
    ↓ JwtUtil.extractAllClaims(jwt)
    ↓ Jwts.parser().parseSignedClaims()
    ↓ JWT library checks "exp" claim
    ↓ throw ExpiredJwtException (jjwt library)
    
    ↓ caught in extractAllClaims()
    ↓ throw new InvalidTokenException("Token has expired", cause)
    
    ↓ caught in JwtAuthFilter
    ↓ logs message, passes to next filter
    ↓ request reaches controller without Authentication set
    ↓ @AuthenticationPrincipal has no user
    
    ↓ Spring Security's ExceptionTranslationFilter handles it
    ↓ throws AuthenticationException
    ↓ GlobalExceptionHandler.handleAuthenticationException()
    
Response:
HTTP/1.1 401 Unauthorized
{
  "success": false,
  "message": "Unauthorized"
}
```

### Scenario 5: Access admin endpoint as regular user

```
GET /api/admin/users
Authorization: Bearer <regular-user-token>

    ↓ JwtAuthFilter validates token (valid)
    ↓ loads user with ROLE_USER
    ↓ sets Authentication in SecurityContext
    
    ↓ Controller method has @PreAuthorize("hasRole('ADMIN')")
    ↓ Spring Security checks: does user have ROLE_ADMIN? NO
    ↓ throw AccessDeniedException (Spring Security)
    
    ↓ GlobalExceptionHandler.handleAccessDenied()
    ↓ returns ResponseEntity with status 403 + ApiResponse
    
Response:
HTTP/1.1 403 Forbidden
{
  "success": false,
  "message": "You don't have permission to access this resource"
}
```

---

## Exception Hierarchy Visualization

```
                    java.lang.Exception
                            ↑
                            │
                  java.lang.RuntimeException
                            ↑
                            │
                    AuthException (ours)
                    ↙         ↓         ↘
        UserAlready    Invalid        UserNotFound
        Exists        Credentials
        Exception      Exception
        
        Also inherits:
        InvalidTokenException
        ForbiddenException
```

When you throw `InvalidCredentialsException`, these handlers can catch it:
```java
@ExceptionHandler(InvalidCredentialsException.class)  // ✓ specific
@ExceptionHandler(AuthException.class)                 // ✓ parent
@ExceptionHandler(RuntimeException.class)              // ✓ grandparent
@ExceptionHandler(Exception.class)                     // ✓ great-grandparent
```

But Spring picks the **most specific** match (1st one in this list).

---

## Key Takeaways

1. **Custom exceptions = clear intent**: `InvalidCredentialsException` immediately tells you what went wrong
2. **Single handler for entire hierarchy**: One method catches all subclasses via inheritance
3. **Automatic status codes**: Same exception type always returns same HTTP status
4. **Security by design**: Generic "Invalid credentials" message prevents username enumeration
5. **Stack traces preserved**: We pass `cause` to maintain the original exception for debugging
6. **Logging at the handler**: Each handler logs appropriately (debug for expected errors, error for bugs)
