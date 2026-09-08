# 02 — Password Hashing (BCrypt)

## Why Not Store Plain Text Passwords?

Imagine your database gets hacked. The attacker gets a file like:

```
alice | alice@example.com | mySecret123
bob   | bob@example.com   | password123
carol | carol@example.com | ilovecats
```

Every user's password is immediately compromised. Even worse — most people
reuse passwords. The attacker now has access to their email, banking, everything.

---

## What We Store Instead: A HASH

A hash function is **one-way**: given input, it produces output. But you
**cannot reverse** it (output → input).

```
"mySecret123" ──→ BCrypt ──→ "$2a$10$N9qo8uLOickgx2ZMRZoMye..."
                  one-way          (60-character hash)
```

If the database leaks, the attacker gets:
```
alice | $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy
```

They **cannot reverse** this to get `mySecret123`. They can try to guess
(brute force), but BCrypt is deliberately slow (~100ms per attempt).
At 100ms/attempt, trying 1 billion passwords takes ~3 years per user.

---

## BCrypt Anatomy

Every BCrypt hash looks like this:

```
$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy
│   │  │──────────────────│─────────────────────────────────────│
│   │  │    22 char salt           31 char hash
│   │  └── the hash
│   └── cost factor (10 = 2^10 = 1024 rounds)
└── BCrypt version 2a
```

### The SALT

The salt is a random string added to the password before hashing.

WITHOUT salt:
```
hash("password123") = "abc123"  ← always the same!
```
An attacker can precompute a "rainbow table": a dictionary of hash→password.
They look up "abc123" and instantly know the password.

WITH salt:
```
hash("password123" + "XyZ9random") = "qwerty7"   ← different salt, different hash
hash("password123" + "AaB3random") = "mnbvcx"
```
Even if two users have the SAME password, they get DIFFERENT hashes.
Rainbow tables become useless.

---

## How Login Verification Works

```java
// Registration: encode the password
String hash = BCryptPasswordEncoder.encode("mySecret123");
// → "$2a$10$..."
user.setPassword(hash); // store the HASH, not the password
userRepository.save(user);

// Login: verify the password
boolean isCorrect = BCryptPasswordEncoder.matches("mySecret123", storedHash);
// → true (BCrypt knows how to extract the salt and rehash)
```

The key insight: `encode()` always produces a **different** hash for the same input.
But `matches()` always returns `true` for the correct password.

This works because the hash itself CONTAINS the salt — BCrypt knows how to
extract it during verification.

---

## In Our Code

See [SecurityConfig.java](../src/main/java/com/example/loginsystem/security/SecurityConfig.java):
```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(10); // cost factor = 10
}
```

See [AuthService.java](../src/main/java/com/example/loginsystem/service/AuthService.java):
```java
// Registration
String hashedPassword = passwordEncoder.encode(request.getPassword());

// Login (Spring Security does this internally via DaoAuthenticationProvider)
boolean correct = passwordEncoder.matches(rawPassword, storedHash);
```
