# Spring Boot Login System — Complete JWT Authentication

A fully documented, educational Spring Boot login system with JWT authentication.
Built from scratch to teach you how every layer works.

---

## What This Project Covers

| Concept | Where to Learn |
|---|---|
| Maven build system | `pom.xml` |
| Spring Boot auto-configuration | `LoginSystemApplication.java` |
| JPA / Hibernate (ORM) | `entity/User.java` |
| Repository pattern | `repository/UserRepository.java` |
| DTO pattern | `dto/` package |
| Password hashing (BCrypt) | `security/SecurityConfig.java`, `service/AuthService.java` |
| JWT tokens | `security/JwtUtil.java` |
| Spring Security filter chain | `security/JwtAuthFilter.java` |
| Security configuration | `security/SecurityConfig.java` |
| Service layer (business logic) | `service/AuthService.java` |
| REST controllers | `controller/AuthController.java` |
| Global exception handling | `security/GlobalExceptionHandler.java` |

---

## Project Structure

```
src/main/java/com/example/loginsystem/
├── LoginSystemApplication.java     ← Entry point (@SpringBootApplication)
│
├── entity/
│   └── User.java                   ← JPA entity (maps to "users" table)
│
├── repository/
│   └── UserRepository.java         ← DB access (Spring Data JPA)
│
├── dto/
│   ├── RegisterRequest.java        ← Input DTO for registration
│   ├── LoginRequest.java           ← Input DTO for login
│   ├── AuthResponse.java           ← Output DTO (contains JWT token)
│   └── ApiResponse.java            ← Generic API response wrapper
│
├── security/
│   ├── JwtUtil.java                ← Token generation & validation
│   ├── JwtAuthFilter.java          ← Runs on every request, validates JWT
│   ├── CustomUserDetailsService.java← Loads user from DB for Spring Security
│   ├── SecurityConfig.java         ← Master security configuration
│   └── GlobalExceptionHandler.java ← Centralized error handling
│
├── service/
│   └── AuthService.java            ← Business logic (register, login)
│
└── controller/
    ├── AuthController.java         ← /api/auth endpoints (register, login, me)
    └── UserController.java         ← /api/user & /api/admin endpoints
```

---

## Quick Start

**Prerequisites:** Java 17+, Maven 3.6+

```bash
# Clone and run
cd login-system-springboot
mvn spring-boot:run
```

The app starts at `http://localhost:8080`

---

## API Endpoints

### Authentication (Public — no token required)

| Method | URL | Description |
|--------|-----|-------------|
| POST | `/api/auth/register` | Register a new user |
| POST | `/api/auth/login` | Login and get JWT token |
| GET | `/api/ping` | Health check |

### Protected (JWT token required)

| Method | URL | Description | Role |
|--------|-----|-------------|------|
| GET | `/api/auth/me` | Get current user info | Any |
| GET | `/api/user/profile` | User profile | Any |
| GET | `/api/admin/users` | List all users | ADMIN only |

---

## How to Use

### 1. Register

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "alice",
    "email": "alice@example.com",
    "password": "mySecret123"
  }'
```

Response:
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "username": "alice",
  "email": "alice@example.com",
  "roles": ["ROLE_USER"]
}
```

### 2. Login

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "alice",
    "password": "mySecret123"
  }'
```

### 3. Use Protected Endpoints

```bash
# Copy the token from the login response, then:
curl http://localhost:8080/api/auth/me \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

### 4. View the Database (H2 Console)

Open `http://localhost:8080/h2-console` in your browser:
- JDBC URL: `jdbc:h2:mem:logindb`
- Username: `sa`
- Password: *(empty)*

---

## How JWT Authentication Works (Big Picture)

```
┌─────────────────────────────────────────────────────────────────┐
│                     AUTHENTICATION FLOW                          │
│                                                                  │
│  1. POST /api/auth/login                                         │
│     { "username": "alice", "password": "mySecret123" }          │
│                   ↓                                              │
│  2. AuthController → AuthService                                 │
│                   ↓                                              │
│  3. AuthenticationManager.authenticate()                         │
│       → CustomUserDetailsService.loadUserByUsername("alice")     │
│       → BCrypt.matches("mySecret123", storedHash)                │
│       → Authentication succeeds ✓                                │
│                   ↓                                              │
│  4. JwtUtil.generateToken(userDetails)                           │
│       → Creates signed JWT with username + roles + expiry        │
│                   ↓                                              │
│  5. Return token to client                                       │
│                                                                  │
│  ─────────────────────────────────────────────────────────────  │
│                                                                  │
│  6. Client stores token, sends with future requests:             │
│     GET /api/auth/me                                             │
│     Authorization: Bearer eyJhbGci...                           │
│                   ↓                                              │
│  7. JwtAuthFilter runs (on EVERY request)                        │
│       → Extracts token from header                               │
│       → Validates signature + expiry                             │
│       → Extracts username from token                             │
│       → Loads user from DB                                       │
│       → Sets Authentication in SecurityContext                   │
│                   ↓                                              │
│  8. Spring Security allows request to reach controller ✓         │
└─────────────────────────────────────────────────────────────────┘
```

---

## Key Concepts Quick Reference

- **BCrypt**: One-way password hashing algorithm. See `docs/02-password-hashing.md`
- **JWT**: Self-contained token format. See `docs/03-jwt-explained.md`
- **Spring Security Filter Chain**: See `docs/04-security-filter-chain.md`
- **JPA/Hibernate ORM**: See `docs/05-jpa-orm.md`

---

## Dependencies Used

| Library | Version | Purpose |
|---------|---------|---------|
| spring-boot-starter-web | 3.2.4 | REST controllers, embedded Tomcat |
| spring-boot-starter-security | 3.2.4 | Authentication & authorization |
| spring-boot-starter-data-jpa | 3.2.4 | ORM (Hibernate) |
| h2 | built-in | In-memory database (dev only) |
| spring-boot-starter-validation | 3.2.4 | Bean validation (@NotBlank, etc.) |
| jjwt | 0.12.3 | JWT generation & validation |
| lombok | built-in | Reduces boilerplate code |
