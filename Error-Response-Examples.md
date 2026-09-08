# Error Response Examples

## Before vs After Custom Exceptions

### BEFORE: Generic RuntimeException

```json
POST /api/auth/register
{
  "username": "alice",
  "email": "alice@example.com",
  "password": "password123"
}

Response (409 Conflict):
{
  "success": false,
  "message": "Username 'alice' is already taken"
}

HOW WE GOT HERE:
throw new IllegalArgumentException("Username 'alice' is already taken");
→ Generic handler catches IllegalArgumentException
→ Hard to distinguish from other errors
→ Unclear what specific problem occurred
```

### AFTER: Custom UserAlreadyExistsException

```json
POST /api/auth/register
{
  "username": "alice",
  "email": "alice@example.com",
  "password": "password123"
}

Response (409 Conflict):
{
  "success": false,
  "message": "Username 'alice' is already taken"
}

HOW WE GET HERE:
throw new UserAlreadyExistsException("Username 'alice' is already taken");
→ Specific handler catches UserAlreadyExistsException
→ Clear what the problem is: user already exists
→ 409 is the standard HTTP status for this scenario
```

---

## All Error Response Examples

### 1. Registration Errors

#### Duplicate Username (409 Conflict)
```json
POST /api/auth/register

{
  "success": false,
  "message": "Username 'alice' is already taken"
}
```

#### Duplicate Email (409 Conflict)
```json
POST /api/auth/register

{
  "success": false,
  "message": "Email 'alice@example.com' is already registered"
}
```

#### Validation Error (400 Bad Request)
```json
POST /api/auth/register
{
  "username": "a",
  "email": "not-email",
  "password": "short"
}

{
  "success": false,
  "message": "Validation failed: username: Username must be 3-50 characters; email: Please provide a valid email address; password: Password must be at least 8 characters"
}
```

#### Invalid Username Format (400 Bad Request)
```json
POST /api/auth/register
{
  "username": "alice@invalid!",
  "email": "alice@example.com",
  "password": "mySecret123"
}

{
  "success": false,
  "message": "Validation failed: username: Username can only contain letters, numbers, underscores and hyphens"
}
```

---

### 2. Login Errors

#### Wrong Password (401 Unauthorized)
```json
POST /api/auth/login
{
  "username": "alice",
  "password": "wrongPassword"
}

{
  "success": false,
  "message": "Invalid username or password"
}
```

#### User Doesn't Exist (401 Unauthorized)
```json
POST /api/auth/login
{
  "username": "bob",
  "password": "somePassword"
}

{
  "success": false,
  "message": "Invalid username or password"
}
```

Note: Same message as wrong password! We don't distinguish to prevent username enumeration.

#### Missing Credentials (400 Bad Request)
```json
POST /api/auth/login
{
  "username": "alice"
}

{
  "success": false,
  "message": "Validation failed: password: Password is required"
}
```

---

### 3. JWT/Token Errors

#### Missing JWT Token (401 Unauthorized)
```json
GET /api/auth/me

{
  "success": false,
  "message": "Unauthorized"
}
```

#### Malformed JWT (401 Unauthorized)
```json
GET /api/auth/me
Authorization: Bearer invalid.jwt.malformed

{
  "success": false,
  "message": "Invalid or expired token"
}
```

#### Expired JWT (401 Unauthorized)
```json
GET /api/auth/me
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...   (24+ hours old)

{
  "success": false,
  "message": "Invalid or expired token"
}
```

#### Tampered JWT (401 Unauthorized)
```json
GET /api/auth/me
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhbGljZSIsImlhdCI6MTcwMDAwMDAwMCwiZXhwIjoxNzAwMDg2NDAwfQ.TAMPERED123

{
  "success": false,
  "message": "Invalid or expired token"
}
```

---

### 4. Authorization Errors

#### Admin Endpoint - Regular User (403 Forbidden)
```json
GET /api/admin/users
Authorization: Bearer <token-from-regular-user>

{
  "success": false,
  "message": "You don't have permission to access this resource"
}
```

#### Unknown Resource (404 Not Found)
```json
GET /api/admin/users/999
Authorization: Bearer <token-from-admin>

{
  "success": false,
  "message": "User with ID 999 not found"
}
```

---

### 5. Success Responses

#### Register Success (201 Created)
```json
POST /api/auth/register
{
  "username": "alice",
  "email": "alice@example.com",
  "password": "mySecret123"
}

HTTP/1.1 201 Created

{
  "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhbGljZSIsInJvbGVzIjpbIlJPTEVfVVNFUiJdLCJpYXQiOjE3MzA5MzQ3MjAsImV4cCI6MTczMTAyMTEyMH0.ABCDEF123456",
  "tokenType": "Bearer",
  "username": "alice",
  "email": "alice@example.com",
  "roles": ["ROLE_USER"]
}
```

#### Login Success (200 OK)
```json
POST /api/auth/login
{
  "username": "alice",
  "password": "mySecret123"
}

HTTP/1.1 200 OK

{
  "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhbGljZSIsInJvbGVzIjpbIlJPTEVfVVNFUiJdLCJpYXQiOjE3MzA5MzQ3MjAsImV4cCI6MTczMTAyMTEyMH0.ABCDEF123456",
  "tokenType": "Bearer",
  "username": "alice",
  "email": "alice@example.com",
  "roles": ["ROLE_USER"]
}
```

#### Get Current User (200 OK)
```json
GET /api/auth/me
Authorization: Bearer <valid-token>

HTTP/1.1 200 OK

{
  "id": 1,
  "username": "alice",
  "email": "alice@example.com",
  "roles": ["ROLE_USER"],
  "active": true,
  "createdAt": "2026-01-08T12:00:00"
}
```

#### Health Check (200 OK)
```json
GET /api/ping

HTTP/1.1 200 OK

{
  "status": "ok",
  "message": "Login System is running!"
}
```

---

## HTTP Status Code Reference

| Code | Meaning | Use When |
|------|---------|----------|
| 200 | OK | Request succeeded, response has data |
| 201 | Created | New resource was successfully created (register) |
| 400 | Bad Request | Client error (validation failure, malformed request) |
| 401 | Unauthorized | Missing or invalid credentials (no token, expired token, wrong password) |
| 403 | Forbidden | Authenticated but lacks permission (ROLE_USER trying /api/admin) |
| 404 | Not Found | Resource doesn't exist |
| 409 | Conflict | Resource already exists (duplicate username/email) |
| 500 | Internal Server Error | Unexpected server error (bug in code) |

---

## Testing Error Scenarios with curl

```bash
# 1. Duplicate username
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username": "alice", "email": "new@a.com", "password": "mySecret123"}'
# → 409 Conflict

# 2. Invalid data
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username": "ab", "email": "not-email", "password": "short"}'
# → 400 Bad Request

# 3. Wrong password
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "alice", "password": "wrongPassword"}'
# → 401 Unauthorized

# 4. Expired token
curl http://localhost:8080/api/auth/me \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhbGljZSJ9.EXPIRED"
# → 401 Unauthorized (or 401 for any JWT issue)

# 5. Admin endpoint, regular user
curl http://localhost:8080/api/admin/users \
  -H "Authorization: Bearer <token-from-regular-user>"
# → 403 Forbidden
```
