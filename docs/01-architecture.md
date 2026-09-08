# 01 — Architecture Overview

## The Layers of This Application

Every well-structured Spring Boot app is divided into layers. Each layer has ONE job.

```
┌──────────────────────────────────────────────┐
│              CLIENT (browser, Postman)        │
│         sends HTTP request with JSON body     │
└────────────────────┬─────────────────────────┘
                     │ HTTP (GET/POST/etc.)
                     ▼
┌──────────────────────────────────────────────┐
│           CONTROLLER LAYER                    │
│  Files: AuthController.java                  │
│         UserController.java                  │
│                                              │
│  Job: Handle HTTP. Parse request. Return     │
│       response. NO business logic.           │
│  Annotation: @RestController                 │
└────────────────────┬─────────────────────────┘
                     │ calls
                     ▼
┌──────────────────────────────────────────────┐
│           SERVICE LAYER                       │
│  Files: AuthService.java                     │
│                                              │
│  Job: Business logic. Orchestrates calls     │
│       to repositories. Runs in transactions. │
│  Annotation: @Service                        │
└────────────────────┬─────────────────────────┘
                     │ calls
                     ▼
┌──────────────────────────────────────────────┐
│           REPOSITORY LAYER                    │
│  Files: UserRepository.java                  │
│                                              │
│  Job: Database access. No logic, just        │
│       reading/writing data.                  │
│  Annotation: @Repository                     │
└────────────────────┬─────────────────────────┘
                     │ SQL (via JPA/Hibernate)
                     ▼
┌──────────────────────────────────────────────┐
│              DATABASE (H2)                    │
│  Tables: users, user_roles                   │
└──────────────────────────────────────────────┘
```

## Why Layers?

**Single Responsibility**: Each layer has one job. If you change how the
database works, you only touch the repository layer, not the controller.

**Testability**: You can test each layer independently:
- Unit test Service by mocking the Repository
- Unit test Controller by mocking the Service
- Integration test Repository against a real (test) database

**Parallel Work**: Teams can work on different layers simultaneously.

---

## The Security Layer (Cross-Cutting Concern)

Security cuts ACROSS all layers. It runs before your controllers via filters:

```
HTTP Request
    ↓
[ Spring Security Filter Chain ]
    ├── JwtAuthFilter          ← validates JWT, sets SecurityContext
    ├── UsernamePasswordFilter ← Spring's built-in (we bypass this with JWT)
    └── ... other filters
    ↓
Your Controller
```

The filter chain is like a pipeline of guards — each one checks something
before passing the request to the next.

---

## Spring's IoC Container (Dependency Injection)

Instead of creating objects manually:
```java
// Without IoC (tightly coupled, hard to test):
AuthService service = new AuthService(new UserRepository(...), new BCryptPasswordEncoder(), ...);
```

Spring creates and wires everything for you:
```java
// With IoC (loose coupling, easy to test):
@Service
@RequiredArgsConstructor
class AuthService {
    private final UserRepository userRepository; // Spring provides this!
    private final PasswordEncoder passwordEncoder; // Spring provides this too!
}
```

This is **Dependency Injection** — you declare what you need, Spring provides it.
The Spring container is called the **ApplicationContext** (or IoC container).
