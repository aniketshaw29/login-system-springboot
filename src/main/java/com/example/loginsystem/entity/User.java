package com.example.loginsystem.entity;

// =============================================================================
// User.java — The User Entity (Database Model)
// =============================================================================
//
// WHAT IS AN ENTITY?
// ──────────────────
// An @Entity is a Java class that maps directly to a DATABASE TABLE.
// Each field in the class = a column in the table.
// Each instance of the class = a row in the table.
//
// FLOW: Java Object ←→ JPA/Hibernate ←→ Database Table
//
// For example:
//   User user = new User();       ← Java object in memory
//   user.setEmail("a@a.com");
//   userRepository.save(user);    ← JPA converts to: INSERT INTO users ...
//
// =============================================================================

import jakarta.persistence.*;        // JPA annotations (@Entity, @Id, etc.)
import jakarta.validation.constraints.*; // Validation annotations (@NotBlank, @Email)
import lombok.*;                     // Boilerplate-reducing annotations

import java.time.LocalDateTime;      // For created_at / updated_at timestamps
import java.util.Set;                // For storing roles (Set avoids duplicates)

/*
  @Entity  ← Tells JPA: "This class represents a database table"
  @Table   ← Tells JPA: "The table is named 'users'" (default would be "user"
             but "user" is a reserved word in many databases — avoid it!)

  Lombok annotations:
    @Data            = generates getters, setters, toString, equals, hashCode
    @Builder         = generates a fluent Builder API for object creation
    @NoArgsConstructor = generates a no-arg constructor (required by JPA!)
    @AllArgsConstructor = generates a constructor with all fields
*/
@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    // =========================================================================
    // PRIMARY KEY
    // =========================================================================
    //
    // @Id          ← This field is the PRIMARY KEY (unique identifier per row)
    // @GeneratedValue ← The database auto-generates this value
    //   strategy = IDENTITY means the DB uses AUTO_INCREMENT / SERIAL
    //   (each new row gets id = previous id + 1)
    //
    // WHY Long? Long can hold values up to ~9.2 quintillion.
    // You'll never have that many users, but Long is the standard choice.

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // =========================================================================
    // COLUMN: username
    // =========================================================================
    //
    // @Column customizes the database column:
    //   unique = true  → the database enforces no two rows can have the same value
    //                    (adds a UNIQUE INDEX in the DB)
    //   nullable = false → the DB column is NOT NULL (must have a value)
    //   length = 50     → VARCHAR(50) in SQL
    //
    // @NotBlank (validation) → checked BEFORE saving — throws exception if empty
    // @Size     (validation) → length between 3 and 50 characters
    //
    // NOTE: @Column constraints (nullable) are enforced at the DB level.
    //       @NotBlank/@Size are enforced at the application level (earlier).
    //       Both layers are good to have!

    @Column(unique = true, nullable = false, length = 50)
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;

    // =========================================================================
    // COLUMN: email
    // =========================================================================

    @Column(unique = true, nullable = false, length = 100)
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid email address")
    private String email;

    // =========================================================================
    // COLUMN: password
    // =========================================================================
    //
    // CRITICAL SECURITY NOTE:
    // ────────────────────────
    // We NEVER store plain-text passwords!
    // What we store is a BCRYPT HASH of the password.
    //
    // BCrypt is a one-way hashing algorithm. Given:
    //   password = "mySecret123"
    //   hash     = "$2a$10$..." (60 characters)
    //
    // You CAN go: password → hash (using BCryptPasswordEncoder.encode())
    // You CANNOT go: hash → password (one-way)
    //
    // To verify login: BCryptPasswordEncoder.matches(rawPassword, storedHash)
    //
    // WHY NOT PLAIN TEXT? If your database gets hacked, attackers get all
    // passwords. With hashes, they can't easily reverse them.
    //
    // columnDefinition = "TEXT" → unlimited length (BCrypt hashes are ~60 chars)

    @Column(nullable = false, columnDefinition = "TEXT")
    private String password; // This stores the HASHED password, never plain text!

    // =========================================================================
    // COLUMN: roles (Many-to-Many relationship with Role)
    // =========================================================================
    //
    // @ElementCollection: Stores a collection of simple values in a separate table.
    //   Spring creates a "user_roles" table with (user_id, role) columns.
    //
    // @CollectionTable: Specifies the name of that join table.
    //
    // @Enumerated(STRING): Stores enum as a string ("ROLE_USER") rather than
    //   an integer (0, 1). STRING is better — DB is readable and won't break
    //   if you reorder enum values.
    //
    // @FetchType.EAGER: Load roles immediately when loading the User.
    //   (Alternative: LAZY = load only when accessed — more efficient for large data)
    //
    // EXAMPLE DB RESULT:
    //   users table: | 1 | alice | alice@example.com | $2a$... |
    //   user_roles:  | 1 | ROLE_USER |
    //                | 1 | ROLE_ADMIN |

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role")
    @Enumerated(EnumType.STRING)
    private Set<Role> roles;

    // =========================================================================
    // COLUMNS: active flag and timestamps
    // =========================================================================

    // Whether the account is active (can be used to "soft delete" users
    // or disable accounts without removing them from the DB)
    @Column(nullable = false)
    @Builder.Default // Lombok @Builder needs this to apply a default value
    private boolean active = true;

    // These store when the record was created/updated.
    // @Column(updatable = false) → once set, createdAt never changes in DB

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // =========================================================================
    // JPA LIFECYCLE CALLBACKS
    // =========================================================================
    //
    // @PrePersist  → runs just BEFORE the entity is first saved (INSERT)
    // @PreUpdate   → runs just BEFORE the entity is updated (UPDATE)
    //
    // These automatically set timestamps so we don't have to remember to do it.

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // =========================================================================
    // INNER ENUM: Role
    // =========================================================================
    //
    // An enum is a fixed set of named constants.
    // Roles control what a user is AUTHORIZED to do.
    //
    // Spring Security expects roles to be prefixed with "ROLE_".
    // So "ROLE_USER" = a regular user, "ROLE_ADMIN" = administrator.
    //
    // In your security config you check: hasRole("USER") or hasRole("ADMIN")
    // Spring Security automatically prepends "ROLE_" when using hasRole().

    public enum Role {
        ROLE_USER,   // Regular user — can access user-level endpoints
        ROLE_ADMIN   // Admin — can access admin-level endpoints
    }
}
