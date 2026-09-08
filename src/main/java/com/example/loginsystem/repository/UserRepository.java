package com.example.loginsystem.repository;

// =============================================================================
// UserRepository.java — The Data Access Layer (Repository / DAO)
// =============================================================================
//
// WHAT IS A REPOSITORY?
// ─────────────────────
// A Repository is the layer between your SERVICE (business logic) and
// the DATABASE. Its job: translate Java method calls into SQL queries.
//
// ARCHITECTURAL LAYERS:
//   HTTP Request
//       ↓
//   Controller  ← handles HTTP, calls service
//       ↓
//   Service     ← business logic, calls repository
//       ↓
//   Repository  ← database queries ← YOU ARE HERE
//       ↓
//   Database
//
// SPRING DATA MAGIC:
// You just DECLARE an interface extending JpaRepository.
// Spring automatically generates the implementation at startup!
// No SQL, no JDBC code, no implementation class needed.
//
// JpaRepository<User, Long> means:
//   User = the entity type this repository manages
//   Long = the type of the primary key (@Id field)
//
// =============================================================================

import com.example.loginsystem.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

// @Repository marks this as a Spring-managed data access component.
// It also enables exception translation (JPA exceptions → Spring DataAccessException).
// Technically optional when extending JpaRepository (Spring detects it),
// but good practice for clarity.

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // =========================================================================
    // Spring Data Query Methods — Magic naming convention!
    // =========================================================================
    //
    // Spring Data JPA reads your method NAMES and generates SQL automatically.
    // The method name follows a pattern: findBy<FieldName>[Condition]
    //
    // findByUsername(String username)
    // → Spring generates: SELECT * FROM users WHERE username = ?
    //
    // Return type Optional<User>:
    //   Optional is a container that may or may not contain a value.
    //   It forces callers to handle the "not found" case explicitly.
    //   BETTER than returning null (which can cause NullPointerException)
    //
    // Usage in service:
    //   Optional<User> user = userRepository.findByUsername("alice");
    //   user.orElseThrow(() -> new UsernameNotFoundException("not found"));

    Optional<User> findByUsername(String username);

    // findByEmail → SELECT * FROM users WHERE email = ?
    Optional<User> findByEmail(String email);

    // =========================================================================
    // Existence checks — no need to load the full object if you just want
    // to know if something exists. These generate efficient EXISTS queries.
    //
    // existsByUsername → SELECT COUNT(*) > 0 FROM users WHERE username = ?
    // =========================================================================

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    // =========================================================================
    // FREE METHODS FROM JpaRepository (inherited, no code needed):
    // =========================================================================
    //
    // save(User user)             → INSERT or UPDATE (if id exists)
    // findById(Long id)           → SELECT * FROM users WHERE id = ?
    // findAll()                   → SELECT * FROM users
    // deleteById(Long id)         → DELETE FROM users WHERE id = ?
    // count()                     → SELECT COUNT(*) FROM users
    // existsById(Long id)         → SELECT COUNT(*) > 0 FROM users WHERE id = ?
    //
    // PAGINATION (great for large datasets):
    // findAll(Pageable pageable)  → SELECT * FROM users LIMIT ? OFFSET ?
    //
    // All these are available for free — no code needed!
}
