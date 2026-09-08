# 07 — Custom Exceptions and Error Handling

## Exception Architecture

We use a **hierarchy-based approach** to custom exceptions:

```
Exception
  └── RuntimeException (checked → unchecked, Spring propagates)
      └── AuthException (base class for all auth-related errors)
          ├── UserAlreadyExistsException
          ├── UserNotFoundException
          ├── InvalidCredentialsException
          ├── InvalidTokenException
          └── ForbiddenException
```

---

## Why Custom Exceptions?

| Without Custom Exceptions | With Custom Exceptions |
|---|---|
| `throw new IllegalArgumentException("...")` | `throw new UserAlreadyExistsException("...")` |
| Generic, tells you nothing about the problem | Specific, clear intent |
| Handler must check message strings | Handler checks exception type |
| Hard to distinguish error scenarios | Different status codes per exception type |

---

## The Exception Hierarchy

### 1. **AuthException** (Base Class)

```java
// Base class — all others inherit from this
public class AuthException extends RuntimeException {
    public AuthException(String message) { ... }
    public AuthException(String message, Throwable cause) { ... }
}
```

**Benefits:**
- Single `@ExceptionHandler(AuthException.class)` catches all subclasses (polymorphism)
- Subclasses override behavior for specific error types
- Easy to add new exception types without changing the handler

---

### 2. **UserAlreadyExistsException** → HTTP 409 Conflict

Thrown when user tries to register with a username/email that already exists.

**Where it's thrown:**
```java
// AuthService.java - register()
if (userRepository.existsByUsername(request.getUsername())) {
    throw new UserAlreadyExistsException(
        "Username 'alice' is already taken");
}
```

**Client receives:**
```json
{
  "success": false,
  "message": "Username 'alice' is already taken"
}
```

**HTTP Status:** `409 Conflict`

---

### 3. **InvalidCredentialsException** → HTTP 401 Unauthorized

Thrown when login credentials are invalid (wrong password or user doesn't exist).

**Where it's thrown:**
```java
// AuthService.java - login()
} catch (AuthenticationException e) {
    throw new InvalidCredentialsException("Invalid username or password");
}
```

**Security Note:**
We deliberately DON'T distinguish between "user not found" and "password wrong"
in the response. If we did, attackers could enumerate valid usernames by trying
different responses.

**Client receives:**
```json
{
  "success": false,
  "message": "Invalid username or password"
}
```

**HTTP Status:** `401 Unauthorized`

---

### 4. **InvalidTokenException** → HTTP 401 Unauthorized

Thrown when JWT token is malformed, expired, or has an invalid signature.

**Where it's thrown:**
```java
// JwtUtil.java - extractAllClaims()
try {
    return Jwts.parser()
            .verifyWith(getSigningKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
} catch (ExpiredJwtException e) {
    throw new InvalidTokenException("Token has expired", e);
} catch (MalformedJwtException e) {
    throw new InvalidTokenException("Invalid token format", e);
} catch (SignatureException e) {
    throw new InvalidTokenException("Token signature is invalid", e);
}
```

**Specific JWT errors caught:**
- `ExpiredJwtException` — token's `exp` claim is in the past
- `MalformedJwtException` — token doesn't have 3 parts (header.payload.signature)
- `SignatureException` — signature doesn't match (token was tampered with)
- `JwtException` — any other JWT-related error

**Client receives:**
```json
{
  "success": false,
  "message": "Invalid or expired token"
}
```

**HTTP Status:** `401 Unauthorized`

---

### 5. **UserNotFoundException** → HTTP 404 Not Found

Thrown when attempting to load a user that doesn't exist.

**Example use case:**
```java
// If we had an endpoint: GET /api/admin/users/{id}
User user = userRepository.findById(id)
    .orElseThrow(() -> new UserNotFoundException("User with ID " + id + " not found"));
```

**Client receives:**
```json
{
  "success": false,
  "message": "User with ID 999 not found"
}
```

**HTTP Status:** `404 Not Found`

---

### 6. **ForbiddenException** → HTTP 403 Forbidden

Thrown when user is authenticated but lacks permission for the resource.

**Example use case:**
```java
// If we had role-checking logic in a service:
if (!user.getRoles().contains(Role.ROLE_ADMIN)) {
    throw new ForbiddenException("You must be an admin to perform this action");
}
```

**Client receives:**
```json
{
  "success": false,
  "message": "You must be an admin to perform this action"
}
```

**HTTP Status:** `403 Forbidden`

---

## GlobalExceptionHandler: How It Works

The `@RestControllerAdvice` class has multiple `@ExceptionHandler` methods.
Each handles a specific exception type and returns the appropriate HTTP status.

```
Exception thrown in controller/service
             ↓
Spring catches it
             ↓
Searches for @ExceptionHandler method matching exception type
             ↓
        Most specific match wins (checked in order):
        1. UserAlreadyExistsException? → 409
        2. InvalidTokenException?      → 401
        3. AuthException?              → 400 (base class fallback)
        4. MethodArgumentNotValidException? → 400 (@Valid failure)
        5. Exception?                  → 500 (catch-all)
             ↓
Handler method converts exception → ApiResponse → JSON
             ↓
ResponseEntity with status code sent to client
```

---

## Exception Handling Order Matters

Handlers are checked in declaration order. **More specific exceptions first!**

```java
@ExceptionHandler(InvalidTokenException.class)    // ← specific
public ResponseEntity<ApiResponse> handleInvalidToken(...) { ... }

@ExceptionHandler(AuthException.class)             // ← general base class
public ResponseEntity<ApiResponse> handleAuthException(...) { ... }

@ExceptionHandler(Exception.class)                 // ← catch-all
public ResponseEntity<ApiResponse> handleGeneric(...) { ... }
```

If we put `AuthException` before `InvalidTokenException`, then `InvalidTokenException`
(which IS-A `AuthException`) would be caught by the parent handler. Wrong behavior.

---

## Testing Exceptions with Postman

Try these requests to trigger different exceptions:

### Register with duplicate username
```
POST /api/auth/register
{
  "username": "alice",      ← already exists
  "email": "new@a.com",
  "password": "mySecret123"
}
```
Expected: `409 Conflict` → `UserAlreadyExistsException`

### Register with invalid data
```
POST /api/auth/register
{
  "username": "ab",         ← too short
  "email": "not-email",     ← not an email
  "password": "short"       ← too short
}
```
Expected: `400 Bad Request` → `MethodArgumentNotValidException` (@Valid)

### Login with wrong password
```
POST /api/auth/login
{
  "username": "alice",
  "password": "wrongPassword"
}
```
Expected: `401 Unauthorized` → `InvalidCredentialsException`

### Use invalid JWT token
```
GET /api/auth/me
Authorization: Bearer invalid.jwt.token
```
Expected: `401 Unauthorized` → `InvalidTokenException`

### Access admin endpoint without permission
```
GET /api/admin/users
Authorization: Bearer <token-from-regular-user>
```
Expected: `403 Forbidden` → Spring Security's `AccessDeniedException` handler

---

## Best Practices

✅ **DO:**
- Create specific exception types for each error scenario
- Throw exceptions early (fail fast)
- Include helpful context in the message (username, field name, etc.)
- Use the exception cause chain to preserve stack traces

❌ **DON'T:**
- Throw generic exceptions (`RuntimeException`, `IllegalArgumentException`)
- Leak sensitive info in error messages (don't say "password wrong" vs "user not found")
- Return 5xx errors for client mistakes (use 4xx instead)
- Log exceptions at every layer (log once where caught, or in the handler)

---

## Adding a New Custom Exception

To handle a new error scenario:

1. Create the exception class:
```java
public class QuotaExceededException extends AuthException {
    public QuotaExceededException(String message) {
        super(message);
    }
}
```

2. Add a handler in `GlobalExceptionHandler`:
```java
@ExceptionHandler(QuotaExceededException.class)
public ResponseEntity<ApiResponse> handleQuotaExceeded(QuotaExceededException ex) {
    return ResponseEntity
            .status(HttpStatus.TOO_MANY_REQUESTS)  // 429
            .body(ApiResponse.error(ex.getMessage()));
}
```

3. Throw it in your service:
```java
if (user.getApiCallsThisMonth() > LIMIT) {
    throw new QuotaExceededException("API quota exceeded. Try again next month.");
}
```

That's it! The global handler automatically catches and converts it.
