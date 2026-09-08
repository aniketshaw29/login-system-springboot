package com.example.loginsystem.security;

// =============================================================================
// JwtUtil.java — JWT Token Generator & Validator
// =============================================================================
//
// WHAT IS JWT?
// ────────────
// JWT = JSON Web Token. A compact, self-contained token for securely
// transmitting information between parties as a JSON object.
//
// JWT ANATOMY (three Base64-encoded parts separated by dots):
//
//   eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhbGljZSIsImlhdCI6MTcwMDAwMDAwMCwiZXhwIjoxNzAwMDg2NDAwfQ.ABC123
//   ──────────────────── ─────────────────────────────────────────────────── ──────────
//         HEADER                           PAYLOAD                            SIGNATURE
//
// HEADER (decoded):
//   { "alg": "HS256" }  ← Algorithm: HMAC-SHA256
//
// PAYLOAD (decoded) — the "claims" (data inside the token):
//   {
//     "sub": "alice",              ← subject (who the token is about)
//     "iat": 1700000000,           ← issued at (Unix timestamp)
//     "exp": 1700086400,           ← expiration (Unix timestamp)
//     "roles": ["ROLE_USER"]       ← custom claim we add
//   }
//
// SIGNATURE:
//   HMAC-SHA256(Base64(header) + "." + Base64(payload), SECRET_KEY)
//   → Proves the token wasn't tampered with. Only our server knows the secret.
//
// HOW IT'S USED IN THE AUTH FLOW:
// ─────────────────────────────────
//   1. Login  → server creates JWT, signs it, sends to client
//   2. Client stores the token (localStorage / cookie)
//   3. Every request → client sends: "Authorization: Bearer <token>"
//   4. Server validates token (signature + expiry) WITHOUT hitting the DB
//   5. Server extracts username from token, loads user if needed
//
// WHY STATELESS?
//   Traditional sessions store user state ON THE SERVER (session store).
//   JWTs are STATELESS — the token itself contains all needed info.
//   This scales better (no shared session store across multiple servers).
//
// =============================================================================

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

// @Component = tells Spring: "create one instance of this class and manage it"
// (Spring calls this a "bean"). Other classes can then @Autowired it.
@Component
public class JwtUtil {

    private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);

    // =========================================================================
    // CONFIGURATION (injected from application.properties)
    // =========================================================================
    //
    // @Value("${property.key}") injects values from application.properties.
    //
    // app.jwt.secret = the secret key for signing tokens
    //   → 256-bit hex string (must be at least 256 bits for HS256)
    //
    // app.jwt.expiration = token lifetime in milliseconds
    //   → 86400000 = 24 hours

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiration}")
    private long jwtExpiration;

    // =========================================================================
    // TOKEN GENERATION
    // =========================================================================

    /**
     * Generate a JWT token for the given UserDetails.
     * Called after successful login.
     */
    public String generateToken(UserDetails userDetails) {
        // Extra claims we want to embed in the token payload
        Map<String, Object> extraClaims = new HashMap<>();
        // Store roles in the token — avoids DB lookup just to check authorization
        extraClaims.put("roles", userDetails.getAuthorities()
                .stream()
                .map(a -> a.getAuthority())
                .toList());

        return buildToken(extraClaims, userDetails);
    }

    /**
     * Internal method that actually constructs the JWT.
     */
    private String buildToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        return Jwts.builder()
                // Add all extra claims (roles, etc.)
                .claims(extraClaims)
                // subject = "who this token is about" (the username)
                .subject(userDetails.getUsername())
                // issued at = right now
                .issuedAt(new Date(System.currentTimeMillis()))
                // expiration = now + 24 hours
                .expiration(new Date(System.currentTimeMillis() + jwtExpiration))
                // sign with our secret key using HS256
                .signWith(getSigningKey())
                // build and serialize to a compact string
                .compact();
        // Result looks like: eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhbGljZSJ9.ABCDEF
    }

    // =========================================================================
    // TOKEN VALIDATION
    // =========================================================================

    /**
     * Check if a token is valid for the given user.
     * Two conditions must BOTH be true:
     *   1. The username in the token matches the given user
     *   2. The token has not expired
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    /**
     * Check if the token's expiration date is in the past.
     */
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    // =========================================================================
    // CLAIM EXTRACTION
    // =========================================================================
    //
    // "Claims" are the pieces of data embedded in the JWT payload.
    // We extract them by parsing and verifying the token.

    /**
     * Extract the username (stored as "subject") from the token.
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
        // Claims::getSubject is a method reference — equivalent to:
        // extractClaim(token, claims -> claims.getSubject())
    }

    /**
     * Extract the expiration date from the token.
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Generic claim extractor. Takes a function that maps Claims to any type T.
     * This is a higher-order function — it receives a function as a parameter.
     *
     * Example usage:
     *   extractClaim(token, Claims::getSubject)     → returns String
     *   extractClaim(token, Claims::getExpiration)  → returns Date
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Parse the token and return all claims.
     * This VERIFIES the signature — if the token was tampered with or
     * uses a different secret key, it throws an exception.
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey()) // set the key to verify the signature
                .build()
                .parseSignedClaims(token)   // parse + verify
                .getPayload();              // extract the payload (claims)
    }

    // =========================================================================
    // SIGNING KEY
    // =========================================================================

    /**
     * Convert the hex secret from application.properties into a SecretKey
     * object that the JWT library can use for signing and verification.
     *
     * Decoders.BASE64.decode() → converts hex string to raw bytes
     * Keys.hmacShaKeyFor()     → wraps bytes into an HMAC-SHA key object
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
