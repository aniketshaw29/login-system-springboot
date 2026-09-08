# 03 — JWT (JSON Web Token) Explained

## What Is a JWT?

A JWT is a compact, self-contained token. It looks like this:

```
eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhbGljZSIsInJvbGVzIjpbIlJPTEVfVVNFUiJdLCJpYXQiOjE3MDAwMDAwMDAsImV4cCI6MTcwMDA4NjQwMH0.ABCDEF123456
```

Three parts separated by dots: `HEADER.PAYLOAD.SIGNATURE`

---

## Part 1: Header (Base64 decoded)
```json
{
  "alg": "HS256"
}
```
Tells you the signing algorithm. `HS256` = HMAC-SHA256 (symmetric key).

---

## Part 2: Payload / Claims (Base64 decoded)
```json
{
  "sub": "alice",
  "roles": ["ROLE_USER"],
  "iat": 1700000000,
  "exp": 1700086400
}
```

Standard claims:
- `sub` (subject) — who the token is about (our username)
- `iat` (issued at) — when the token was created (Unix timestamp)
- `exp` (expiration) — when the token expires (Unix timestamp)

Custom claims we add:
- `roles` — the user's roles (so we don't need a DB lookup every request)

> **Note**: The payload is Base64-encoded, NOT encrypted. Anyone can decode it.
> **Never put sensitive data** (passwords, credit cards) in a JWT payload.
> It's a secret SIGNATURE that matters, not encryption.

---

## Part 3: Signature

```
HMAC-SHA256(
  Base64(header) + "." + Base64(payload),
  SECRET_KEY
)
```

The signature **proves the token wasn't tampered with**. If anyone changes
a single character in the payload, the signature verification fails.

Only our server knows the `SECRET_KEY`. So only our server can:
- **Create** valid tokens (signing)
- **Verify** tokens (validation)

---

## The Full Auth Flow

```
  ┌─────────┐                               ┌─────────┐
  │  Client  │                               │  Server │
  └────┬─────┘                               └────┬────┘
       │  POST /api/auth/login                    │
       │  { username, password }  ────────────►   │
       │                                          │  1. Load user from DB
       │                                          │  2. BCrypt.matches(pass, hash)
       │                                          │  3. Create JWT token
       │                                          │  4. Sign with SECRET_KEY
       │   ◄────────────────────────────────────  │
       │   { token: "eyJhb..." }                  │
       │                                          │
  [Client stores token in localStorage]           │
       │                                          │
       │  GET /api/auth/me                        │
       │  Authorization: Bearer eyJhb... ──────►  │
       │                                          │  1. Extract token from header
       │                                          │  2. Verify signature
       │                                          │  3. Check not expired
       │                                          │  4. Extract username
       │                                          │  5. Set in SecurityContext
       │   ◄────────────────────────────────────  │
       │   { username: "alice", ... }             │
  └─────────┘                               └─────────┘
```

---

## Sessions vs JWTs

| | Traditional Sessions | JWT Tokens |
|---|---|---|
| State stored on | Server (session store) | Client (the token itself) |
| Scalability | Need shared session store for multiple servers | Stateless — any server validates any token |
| Logout | Delete server-side session | Token is valid until expiry (or use a blacklist) |
| Storage | Server memory/Redis/DB | Client localStorage/cookie |
| Size | Small (just a session ID) | Larger (~200-500 bytes) |

---

## In Our Code

See [JwtUtil.java](../src/main/java/com/example/loginsystem/security/JwtUtil.java):
```java
// Generate token after successful login
String token = jwtUtil.generateToken(userDetails);

// Validate token in the filter
boolean isValid = jwtUtil.isTokenValid(token, userDetails);

// Extract username (to know who sent the request)
String username = jwtUtil.extractUsername(token);
```

---

## Security Considerations

1. **Keep the secret key LONG and RANDOM** — our key is 256 bits (64 hex chars)
2. **Set a reasonable expiry** — we use 24 hours; shorter = more secure
3. **Never put sensitive data in the payload** — it's readable by anyone
4. **HTTPS in production** — tokens in plain HTTP can be intercepted
5. **The JWT secret must be in an environment variable** in production,
   never committed to git!
